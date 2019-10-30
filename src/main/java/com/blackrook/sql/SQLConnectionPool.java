/*******************************************************************************
 * Copyright (c) 2019 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.sql;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.sql.Connection;
import java.sql.SQLException;

import com.blackrook.sql.SQLTransaction.Level;

/**
 * This is a database connection pool class for a bunch of shared, managed connections.
 * Meant to be accessed by many threads in an enterprise setting.
 * If a connection is requested that is not available, the requesting thread will wait
 * until a connection is found or until it times out. 
 * @author Matthew Tropiano
 */
public class SQLConnectionPool implements AutoCloseable
{
	/** The connector used. */
	private SQLConnector connector;
	/** The user name used. */
	private String userName;
	/** The password used. */
	private String password;
	/** The properties used. */
	private Properties info;
	
	/** List of managed connections. */
	private final Queue<Connection> availableConnections;
	/** List of used connections. */
	private final HashSet<Connection> usedConnections;
	
	/** Connection pool mutex. */
	private final Object POOLMUTEX = new Object();
	
	/**
	 * Creates a new connection pool with a set amount of managed connections,
	 * and no credentials (used with databases that require no login).
	 * @param connector the connector to use.
	 * @param connectionCount the number of connections to pool.
	 * @throws SQLException if a connection cannot be established.
	 */
	public SQLConnectionPool(SQLConnector connector, int connectionCount) throws SQLException
	{
		this(connector, connectionCount, null, null);
	}
	
	/**
	 * Creates a new connection pool with a set amount of managed connections.
	 * @param connector the connector to use.
	 * @param connectionCount the number of connections to pool.
	 * @param userName the account user name.
	 * @param password the account password.
	 * @throws SQLException if a connection cannot be established and the amount of connections opened.
	 */
	public SQLConnectionPool(SQLConnector connector, int connectionCount, String userName, String password) throws SQLException
	{
		this.connector = connector;
		this.userName = userName;
		this.password = password;
		this.info = null;
		
		this.availableConnections = new LinkedList<Connection>();
		this.usedConnections = new HashSet<Connection>();
		for (int i = 0; i < connectionCount; i++)
			availableConnections.add(connector.getConnection(userName, password));
	}

	/**
	 * Creates a new connection pool with a set amount of managed connections.
	 * @param connector the connector to use.
	 * @param connectionCount the number of connections to pool.
	 * @param info the set of {@link Properties} to pass along to the connector.
	 * @throws SQLException if a connection cannot be established and the amount of connections opened.
	 */
	public SQLConnectionPool(SQLConnector connector, int connectionCount, Properties info) throws SQLException
	{
		this.connector = connector;
		this.userName = null;
		this.password = null;
		this.info = info;
		
		this.availableConnections = new LinkedList<Connection>();
		this.usedConnections = new HashSet<Connection>();
		for (int i = 0; i < connectionCount; i++)
			availableConnections.add(connector.getConnection(info));
	}

	/**
	 * Retrieves a connection from this pool, passes it to the provided {@link Consumer} function,
	 * then returns it to the pool.
	 * @param handler the Consumer function that accepts the retrieved connection.
	 * @throws InterruptedException	if an interrupt is thrown by the current thread waiting for an available connection. 
	 * @throws SQLException if a connection cannot be re-created or re-established.
	 */
	public void getConnectionAnd(Consumer<Connection> handler) throws InterruptedException, SQLException
	{
		try {
			getConnectionAnd(0L, handler);
		} catch (TimeoutException e) {
			throw new RuntimeException(e); // Does not happen.
		}
	}
	
	/**
	 * Retrieves a connection from this pool, passes it to the provided {@link Consumer} function,
	 * calls it, then returns it to the pool.
	 * @param waitMillis the amount of time (in milliseconds) to wait for a connection.
	 * @param handler the Consumer function that accepts the retrieved connection.
	 * @throws InterruptedException	if an interrupt is thrown by the current thread waiting for an available connection. 
	 * @throws TimeoutException if the wait lapses and there are no available connections.
	 * @throws SQLException if a connection cannot be re-created or re-established.
	 */
	public void getConnectionAnd(long waitMillis, Consumer<Connection> handler) throws InterruptedException, TimeoutException, SQLException
	{
		Connection conn = null;
		try
		{
			conn = getAvailableConnection(waitMillis);
			handler.accept(conn);
		}
		finally
		{
			if (conn != null)
				releaseConnection(conn);
		}	
	}
	
	/**
	 * Retrieves a connection from this pool, passes it to the provided {@link Function},
	 * calls it, returns it to the pool, and returns the result.
	 * @param <R> the return type.
	 * @param handler the Consumer function that accepts the retrieved connection.
	 * @return the return value of the handler function.
	 * @throws InterruptedException	if an interrupt is thrown by the current thread waiting for an available connection. 
	 * @throws SQLException if a connection cannot be re-created or re-established.
	 */
	public <R> R getConnectionAnd(Function<Connection, R> handler) throws InterruptedException, SQLException
	{
		try {
			return getConnectionAnd(0L, handler);
		} catch (TimeoutException e) {
			throw new RuntimeException(e); // Does not happen.
		}
	}
	
	/**
	 * Retrieves a connection from this pool, passes it to the provided {@link Function},
	 * calls it, returns it to the pool, and returns the result.
	 * @param <R> the return type.
	 * @param waitMillis the amount of time (in milliseconds) to wait for a connection.
	 * @param handler the Consumer function that accepts the retrieved connection.
	 * @return the return value of the handler function.
	 * @throws InterruptedException	if an interrupt is thrown by the current thread waiting for an available connection. 
	 * @throws TimeoutException if the wait lapses and there are no available connections.
	 * @throws SQLException if a connection cannot be re-created or re-established.
	 */
	public <R> R getConnectionAnd(long waitMillis, Function<Connection, R> handler) throws InterruptedException, TimeoutException, SQLException
	{
		Connection conn = null;
		try
		{
			conn = getAvailableConnection(waitMillis);
			return handler.apply(conn);
		}
		finally
		{
			if (conn != null)
				releaseConnection(conn);
		}	
	}
	
	/**
	 * Retrieves an available connection from the pool.
	 * @return a connection to use.
	 * @throws InterruptedException	if an interrupt is thrown by the current thread waiting for an available connection. 
	 * @throws SQLException if a connection cannot be re-created or re-established.
	 */
	public Connection getAvailableConnection() throws InterruptedException, SQLException
	{
		try {
			return getAvailableConnection(0L);
		} catch (TimeoutException e) {
			return null; // Does not happen.
		}
	}
	
	/**
	 * Retrieves an available connection from the pool.
	 * @param waitMillis the amount of time (in milliseconds) to wait for a connection.
	 * @return a connection to use.
	 * @throws InterruptedException	if an interrupt is thrown by the current thread waiting for an available connection. 
	 * @throws TimeoutException if the wait lapses and there are no available connections.
	 * @throws SQLException if a connection cannot be re-created or re-established.
	 */
	public Connection getAvailableConnection(long waitMillis) throws InterruptedException, TimeoutException, SQLException
	{
		synchronized (POOLMUTEX)
		{
			if (availableConnections.isEmpty())
			{
				availableConnections.wait(waitMillis);
				if (availableConnections.isEmpty())
					throw new TimeoutException("no available connections.");
			}
			
			Connection out;
			while ((out = availableConnections.poll()).isClosed())
			{
				if (info != null)
					out = connector.getConnection(info);
				else if (userName != null)
					out = connector.getConnection(userName, password);
				else
					out = connector.getConnection();
			}
			
			usedConnections.add(out);
			return out;
		}
	}
	
	/**
	 * Gets the number of available connections.
	 * @return the amount of connections currently used.
	 */
	public int getAvailableConnectionCount()
	{
		return availableConnections.size();
	}
	
	/**
	 * Gets the number of connections in use.
	 * @return the amount of connections currently used.
	 */
	public int getUsedConnectionCount()
	{
		return usedConnections.size();
	}

	/**
	 * Gets the number of total connections.
	 * @return the total amount of managed connections.
	 */
	public int getTotalConnectionCount()
	{
		return getAvailableConnectionCount() + getUsedConnectionCount();
	}

	/**
	 * Generates a transaction for multiple queries in one set.
	 * This transaction performs all of its queries through one connection.
	 * The connection is held by this transaction until it is finished via {@link SQLTransaction#complete()}.
	 * @param transactionLevel the isolation level of the transaction.
	 * @return a {@link SQLTransaction} object to handle a contiguous transaction.
	 * @throws InterruptedException	if an interrupt is thrown by the current thread waiting for an available connection. 
	 * @throws SQLException if a connection cannot be re-created or re-established.
	 */
	public SQLTransaction startTransaction(Level transactionLevel) throws InterruptedException, SQLException
	{
		return new SQLTransaction(getAvailableConnection(), transactionLevel);
	}

	/**
	 * Generates a transaction for multiple queries in one set.
	 * This transaction performs all of its queries through one connection.
	 * The connection is held by this transaction until it is finished via {@link SQLTransaction#complete()}.
	 * @param waitMillis the amount of time (in milliseconds) to wait for a connection.
	 * @param transactionLevel the isolation level of the transaction.
	 * @return a {@link SQLTransaction} object to handle a contiguous transaction.
	 * @throws InterruptedException	if an interrupt is thrown by the current thread waiting for an available connection. 
	 * @throws TimeoutException if the wait lapses and there are no available connections.
	 * @throws SQLException if a connection cannot be re-created or re-established.
	 */
	public SQLTransaction startTransaction(long waitMillis, Level transactionLevel) throws InterruptedException, TimeoutException, SQLException
	{
		return new SQLTransaction(getAvailableConnection(waitMillis), transactionLevel);
	}

	/**
	 * Releases a connection back to the pool.
	 * @param connection the connection to release.
	 */
	public void releaseConnection(Connection connection)
	{
		if (!usedConnections.contains(connection))
			throw new IllegalStateException("Tried to release a connection not maintained by this pool.");
		
		synchronized (POOLMUTEX)
		{
			usedConnections.remove(connection);
			availableConnections.add(connection);
			availableConnections.notifyAll();
		}
	}
	
	/**
	 * Closes all open connections in the pool.
	 */
	@Override
	public void close()
	{
		synchronized (POOLMUTEX)
		{
			Iterator<Connection> it = usedConnections.iterator();
			while (it.hasNext())
			{
				availableConnections.add(it.next());
				it.remove();
			}
			while (!availableConnections.isEmpty())
			{
				try {
					availableConnections.poll().close();
				} catch (SQLException e) {
					// Should not be thrown - does not matter anyway.
				}
			}
		}
	}
	
}
