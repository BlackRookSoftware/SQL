/*******************************************************************************
 * Copyright (c) 2019-2022 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.sql;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeoutException;

import com.blackrook.sql.util.SQLConnectionConsumer;
import com.blackrook.sql.util.SQLConnectionFunction;

import java.sql.SQLException;

/**
 * This is a database connection pool class for a bunch of shared, managed connections.
 * Meant to be accessed by many threads in an enterprise setting.
 * If a connection is requested that is not available, the requesting thread will wait
 * until a connection is found or until it times out. 
 * @author Matthew Tropiano
 */
public class SQLPool implements AutoCloseable
{
	/** The connector used. */
	private SQLConnector connector;
	
	/** List of managed connections. */
	private final Queue<SQLConnection> availableConnections;
	/** List of used connections. */
	private final HashSet<SQLConnection> usedConnections;
	
	/**
	 * Creates a new connection pool from a {@link SQLConnector}.
	 * @param connector the connector to use.
	 * @param connectionCount the number of connections to pool.
	 * @throws SQLException if a connection cannot be established.
	 */
	public SQLPool(SQLConnector connector, int connectionCount) throws SQLException
	{
		this.connector = connector;
		this.availableConnections = new LinkedList<SQLConnection>();
		this.usedConnections = new HashSet<SQLConnection>();
		for (int i = 0; i < connectionCount; i++)
			availableConnections.add(connector.getConnection());
	}
	
	/**
	 * Retrieves a connection from this pool, passes it to the provided {@link SQLConnectionConsumer} function,
	 * then returns it to the pool.
	 * @param handler the consumer function that accepts the retrieved connection.
	 * @throws InterruptedException	if an interrupt is thrown by the current thread waiting for an available connection. 
	 * @throws SQLException if a connection cannot be re-created or re-established.
	 */
	public void getConnectionAnd(SQLConnectionConsumer handler) throws InterruptedException, SQLException
	{
		try {
			getConnectionAnd(0L, handler);
		} catch (TimeoutException e) {
			throw new RuntimeException(e); // Does not happen.
		}
	}
	
	/**
	 * Retrieves a connection from this pool, passes it to the provided {@link SQLConnectionConsumer} function,
	 * calls it, then returns it to the pool.
	 * @param waitMillis the amount of time (in milliseconds) to wait for a connection.
	 * @param handler the consumer function that accepts the retrieved connection.
	 * @throws InterruptedException	if an interrupt is thrown by the current thread waiting for an available connection. 
	 * @throws TimeoutException if the wait lapses and there are no available connections.
	 * @throws SQLException if a connection cannot be re-created or re-established.
	 */
	public void getConnectionAnd(long waitMillis, SQLConnectionConsumer handler) throws InterruptedException, TimeoutException, SQLException
	{
		SQLConnection conn = null;
		try {
			conn = getAvailableConnection(waitMillis);
			handler.accept(conn);
		} finally {
			if (conn != null)
				releaseConnection(conn);
		}	
	}
	
	/**
	 * Retrieves a connection from this pool, passes it to the provided {@link SQLConnectionFunction},
	 * calls it, returns it to the pool, and returns the result.
	 * @param <R> the return type.
	 * @param handler the consumer function that accepts the retrieved connection and returns a value.
	 * @return the return value of the handler function.
	 * @throws InterruptedException	if an interrupt is thrown by the current thread waiting for an available connection. 
	 * @throws SQLException if a connection cannot be re-created or re-established.
	 */
	public <R> R getConnectionAnd(SQLConnectionFunction<R> handler) throws InterruptedException, SQLException
	{
		try {
			return getConnectionAnd(0L, handler);
		} catch (TimeoutException e) {
			throw new RuntimeException(e); // Does not happen.
		}
	}
	
	/**
	 * Retrieves a connection from this pool, passes it to the provided {@link SQLConnectionFunction},
	 * calls it, returns it to the pool, and returns the result.
	 * @param <R> the return type.
	 * @param waitMillis the amount of time (in milliseconds) to wait for a connection.
	 * @param handler the consumer function that accepts the retrieved connection and returns a value.
	 * @return the return value of the handler function.
	 * @throws InterruptedException	if an interrupt is thrown by the current thread waiting for an available connection. 
	 * @throws TimeoutException if the wait lapses and there are no available connections.
	 * @throws SQLException if a connection cannot be re-created or re-established.
	 */
	public <R> R getConnectionAnd(long waitMillis, SQLConnectionFunction<R> handler) throws InterruptedException, TimeoutException, SQLException
	{
		SQLConnection conn = null;
		try {
			conn = getAvailableConnection(waitMillis);
			return handler.apply(conn);
		} finally {
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
	public SQLConnection getAvailableConnection() throws InterruptedException, SQLException
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
	public SQLConnection getAvailableConnection(long waitMillis) throws InterruptedException, TimeoutException, SQLException
	{
		synchronized (availableConnections)
		{
			if (availableConnections.isEmpty())
			{
				availableConnections.wait(waitMillis);
				if (availableConnections.isEmpty())
					throw new TimeoutException("no available connections.");
			}
			
			SQLConnection out;
			if ((out = availableConnections.poll()).isClosed())
			{
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
	 * Releases a connection back to the pool.
	 * Also cancels a transaction that it may still be in, if any.
	 * @param connection the connection to release.
	 */
	public void releaseConnection(SQLConnection connection)
	{
		if (!usedConnections.contains(connection))
			throw new IllegalStateException("Tried to release a connection not maintained by this pool.");
		
		connection.endTransaction();
		
		synchronized (availableConnections)
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
		synchronized (availableConnections)
		{
			Iterator<SQLConnection> it = usedConnections.iterator();
			while (it.hasNext())
			{
				availableConnections.add(it.next());
				it.remove();
			}
			while (!availableConnections.isEmpty())
			{
				availableConnections.poll().close();
			}
		}
	}
	
}
