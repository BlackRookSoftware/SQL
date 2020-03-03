/*******************************************************************************
 * Copyright (c) 2019-2020 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.sql;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import com.blackrook.sql.util.SQLConnectionConsumer;
import com.blackrook.sql.util.SQLConnectionFunction;

/**
 * Core database JDBC connector object.
 * From this object, representing a potential link to a remote (or local) database, connections can be spawned.
 * @author Matthew Tropiano
 */
public class SQLConnector
{
	/** JDBC URL. */
	private String jdbcURL;
	/** Info properties. */
	private Properties info;
	/** Username. */
	private String userName;
	/** Password. */
	private String password;
	
	/**
	 * Constructs a new database connector.
	 * @param className	The fully qualified class name of the driver.
	 * @param jdbcURL The JDBC URL to use.
	 * @throws RuntimeException if the driver class cannot be found.
	 */
	public SQLConnector(String className, String jdbcURL)
	{
		this.jdbcURL = jdbcURL;
		this.info = null;
		this.userName = null;
		this.password = null;
		
		try {
			Class.forName(className);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Constructs a new database connector.
	 * @param className	The fully qualified class name of the driver.
	 * @param jdbcURL The JDBC URL to use.
	 * @param info the set of {@link Properties} to pass along to the JDBC {@link DriverManager}.
	 * @throws RuntimeException if the driver class cannot be found.
	 */
	public SQLConnector(String className, String jdbcURL, Properties info)
	{
		this(className, jdbcURL);
		this.info = info;
	}

	/**
	 * Constructs a new database connector.
	 * @param className	The fully qualified class name of the driver.
	 * @param jdbcURL The JDBC URL to use.
	 * @param userName the username.
	 * @param password the password.
	 * @throws RuntimeException if the driver class cannot be found.
	 */
	public SQLConnector(String className, String jdbcURL, String userName, String password)
	{
		this(className, jdbcURL);
		this.userName = userName;
		this.password = password;
	}

	/**
	 * Returns the full JDBC URL for this specific connector.
	 * This differs by implementation and driver.
	 * @return the URL
	 */
	public String getJDBCURL()
	{
		return jdbcURL;
	}

	/**
	 * Returns a new, opened JDBC Connection using the credentials stored with this connector.
	 * @return a {@link DriverManager}-created connection.
	 * @throws SQLException	if a connection can't be procured.
	 * @see DriverManager#getConnection(String)
	 */
	public SQLConnection getConnection() throws SQLException
	{
		if (userName != null)
			return new SQLConnection(DriverManager.getConnection(getJDBCURL(), userName, password));
		else if (info != null)
			return new SQLConnection(DriverManager.getConnection(getJDBCURL(), info));
		else
			return new SQLConnection(DriverManager.getConnection(getJDBCURL()));
	}

	/**
	 * Creates a connection, passes it to the provided {@link SQLConnectionConsumer} function, then closes it.
	 * @param handler the consumer function that accepts the retrieved connection.
	 * @throws SQLException if a connection cannot be re-created or re-established.
	 */
	public void getConnectionAnd(SQLConnectionConsumer handler) throws SQLException
	{
		try (SQLConnection connection = getConnection())
		{
			handler.accept(connection);
		}
	}
	
	/**
	 * Creates a connection, passes it to the provided {@link SQLConnectionFunction}, 
	 * calls it, closes it, and returns the result.
	 * @param <R> the return type.
	 * @param handler the consumer function that accepts the retrieved connection and returns a value.
	 * @return the return value of the handler function.
	 * @throws SQLException if a connection cannot be re-created or re-established.
	 */
	public <R> R getConnectionAnd(SQLConnectionFunction<R> handler) throws SQLException
	{
		try (SQLConnection connection = getConnection())
		{
			return handler.apply(connection);
		}
	}
	
}
