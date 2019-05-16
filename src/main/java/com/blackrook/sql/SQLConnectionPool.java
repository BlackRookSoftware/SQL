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
import java.util.Queue;
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
public class SQLConnectionPool
{
	/** Util reference. */
	protected SQLConnector connector;
	/** Pool username. */
	protected String userName;
	/** Pool password. */
	protected String password;
	/** List of managed connections. */
	protected final Queue<Connection> availableConnections;
	/** List of used connections. */
	protected final HashSet<Connection> usedConnections;
	
	/**
	 * Creates a new connection pool with a set amount of managed connections,
	 * and no credentials (used with databases that require no login).
	 * @param connector the connector to use.
	 * @param conns the number of connections to pool.
	 */
	public SQLConnectionPool(SQLConnector connector, int conns) throws SQLException
	{
		this(connector, conns, null, null);
	}
	
	/**
	 * Creates a new connection pool with a set amount of managed connections.
	 * @param connector the connector to use.
	 * @param conns the number of connections to pool.
	 * @param userName the account user name.
	 * @param password the account password.
	 */
	public SQLConnectionPool(SQLConnector connector, int conns, String userName, String password) throws SQLException
	{
		this.connector = connector;
		this.userName = userName;
		this.password = password;
		this.availableConnections = new LinkedList<Connection>();
		this.usedConnections = new HashSet<Connection>();
		for (int i = 0; i < conns; i++)
			availableConnections.add(connector.getConnection(userName,password));
	}

	/**
	 * Retrieves an available connection from the pool.
	 * @throws InterruptedException	if an interrupt is thrown by the current thread waiting for an available connection. 
	 */
	@SuppressWarnings("resource")
	public Connection getAvailableConnection() throws InterruptedException
	{
		Connection out = null;
		synchronized (availableConnections)
		{
			while (availableConnections.isEmpty())
				availableConnections.wait();

			while (out == null)
			{
				try{
					Connection conn = availableConnections.poll();
					if (conn.isClosed())
					{
						if (userName != null)
							availableConnections.add(conn = connector.getConnection(userName,password));
						else
							availableConnections.add(conn = connector.getConnection());
					}
					
					out = conn;
					usedConnections.add(out);
				} catch (SQLException e) {
					throw new RuntimeException("Could not reopen connection: "+e.getLocalizedMessage());
				}
			}
			
		}
		
		return out;
	}
	
	/**
	 * Gets the number of available connections.
	 */
	public int getAvailableConnectionCount()
	{
		return availableConnections.size();
	}
	
	/**
	 * Gets the number of connections in use.
	 */
	public int getUsedConnectionCount()
	{
		return usedConnections.size();
	}

	/**
	 * Gets the number of total connections.
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
	 */
	public SQLTransaction startTransaction(Level transactionLevel) throws InterruptedException
	{
		return new SQLTransaction(getAvailableConnection(), transactionLevel);
	}

	/**
	 * Releases a connection back to the pool.
	 * @param c the connection to release.
	 */
	public void releaseConnection(Connection c)
	{
		if (!usedConnections.contains(c))
			throw new IllegalStateException("Tried to release a connection not maintained by this pool.");
		
		synchronized (availableConnections)
		{
			usedConnections.remove(c);
			availableConnections.add(c);
			availableConnections.notifyAll();
		}
	}
	
	/**
	 * Closes all open connections in the pool.
	 */
	public void close()
	{
		synchronized (availableConnections)
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
