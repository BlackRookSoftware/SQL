/*******************************************************************************
 * Copyright (c) 2019 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.sql;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;

import com.blackrook.sql.SQLResult;

/**
 * A transaction object that holds a connection that guarantees an isolation level
 * of some kind. Queries can be made through this object until it has been released. 
 * <p>
 * This object's {@link #finalize()} method attempts to roll back the transaction if it hasn't already
 * been finished.
 * @author Matthew Tropiano
 */
public class SQLTransaction implements AutoCloseable
{
	/** 
	 * Enumeration of transaction levels. 
	 */
	public enum Level
	{
		/**
		 * From {@link Connection}: A constant indicating that dirty reads are 
		 * prevented; non-repeatable reads and phantom reads can occur. This 
		 * level only prohibits a transaction from reading a row with uncommitted 
		 * changes in it.
		 */
		READ_COMMITTED(Connection.TRANSACTION_READ_COMMITTED),

		/**
		 * From {@link Connection}: A constant indicating that dirty reads, 
		 * non-repeatable reads and phantom reads can occur. This level allows 
		 * a row changed by one transaction to be read by another transaction 
		 * before any changes in that row have been committed (a "dirty read"). 
		 * If any of the changes are rolled back, the second transaction will 
		 * have retrieved an invalid row.
		 */
		READ_UNCOMMITTED(Connection.TRANSACTION_READ_UNCOMMITTED),
		
		/**
		 * From {@link Connection}: A constant indicating that dirty reads and 
		 * non-repeatable reads are prevented; phantom reads can occur. 
		 * This level prohibits a transaction from reading a row with 
		 * uncommitted changes in it, and it also prohibits the situation 
		 * where one transaction reads a row, a second transaction alters 
		 * the row, and the first transaction rereads the row, getting different 
		 * values the second time (a "non-repeatable read").
		 */
		REPEATABLE_READ(Connection.TRANSACTION_REPEATABLE_READ),
		
		/**
		 * From {@link Connection}: A constant indicating that dirty reads, 
		 * non-repeatable reads and phantom reads are prevented. This level 
		 * includes the prohibitions in TRANSACTION_REPEATABLE_READ and further 
		 * prohibits the situation where one transaction reads all rows that 
		 * satisfy a WHERE condition, a second transaction inserts a row that 
		 * satisfies that WHERE condition, and the first transaction rereads for 
		 * the same condition, retrieving the additional "phantom" row in the 
		 * second read.
		 */
		SERIALIZABLE(Connection.TRANSACTION_SERIALIZABLE),
		;
		
		private final int id;
		private Level(int id)
		{
			this.id = id;
		}
	}

	/** The encapsulated connection. */
	private Connection connection;
	
	/** Previous level state on the incoming connection. */
	private int previousLevelState;
	/** Previous auto-commit state on the incoming connection. */
	private boolean previousAutoCommit;
	
	/**
	 * Wraps a connection in a transaction.
	 * The connection gets {@link Connection#setAutoCommit(boolean)} called on it with a FALSE parameter,
	 * and sets the transaction isolation level. These settings are restored when the transaction is 
	 * finished via {@link #close()}, {@link #commit()}, or {@link #abort()}.
	 * @param connection the connection to the database to use for this transaction.
	 * @param transactionLevel the transaction level to set on this transaction.
	 * @throws SQLException if this transaction could not be prepared.
	 */
	public SQLTransaction(Connection connection, Level transactionLevel) throws SQLException
	{
		this.connection = connection;
		this.previousLevelState = connection.getTransactionIsolation();
		this.previousAutoCommit = connection.getAutoCommit();
		this.connection.setAutoCommit(false);
		this.connection.setTransactionIsolation(transactionLevel.id);
	}

	/**
	 * @return true if this transaction has been completed or false if more methods can be invoked on it.
	 */
	public boolean isFinished()
	{
		return connection == null; 
	}	
	
	/**
	 * Completes this transaction and prevents further calls on it.
	 * This calls {@link Connection#commit()}. 
	 * on the encapsulated connection and resets its previous transaction level state plus its auto-commit state.
	 * @throws IllegalStateException if this transaction was already finished.
	 * @throws SQLException if this causes a database error.
	 */
	public void complete() throws SQLException
	{
		if (isFinished())
			throw new IllegalStateException("This transaction is already finished.");
		
		connection.commit();
		connection.setTransactionIsolation(previousLevelState);
		connection.setAutoCommit(previousAutoCommit);
		connection = null;
	}
	
	/**
	 * Aborts this transaction and prevents further calls on it.
	 * This calls {@link Connection#rollback()}. 
	 * on the encapsulated connection and resets its previous transaction level state plus its auto-commit state.
	 * @throws IllegalStateException if this transaction was already finished.
	 * @throws SQLException if this causes a database error.
	 */
	public void abort() throws SQLException
	{
		if (isFinished())
			throw new IllegalStateException("This transaction is already finished.");
		
		connection.rollback();
		connection.setTransactionIsolation(previousLevelState);
		connection.setAutoCommit(previousAutoCommit);
		connection = null;
	}
	
	/**
	 * Commits the actions completed so far in this transaction.
	 * This is also called during {@link #complete()}.
	 * @throws IllegalStateException if this transaction was already finished.
	 * @throws SQLException if this causes a database error.
	 */
	public void commit() throws SQLException
	{
		if (isFinished())
			throw new IllegalStateException("This transaction is already finished.");
		connection.commit();
	}
	
	/**
	 * Rolls back this entire transaction.
	 * @throws IllegalStateException if this transaction was already finished.
	 * @throws SQLException if this causes a database error.
	 */
	public void rollback() throws SQLException
	{
		if (isFinished())
			throw new IllegalStateException("This transaction is already finished.");
		connection.rollback();
	}
	
	/**
	 * Rolls back this transaction to a {@link Savepoint}. Everything executed
	 * after the {@link Savepoint} passed into this method will be rolled back.
	 * @param savepoint the {@link Savepoint} to roll back to.
	 * @throws IllegalStateException if this transaction was already finished.
	 * @throws SQLException if this causes a database error.
	 */
	public void rollback(Savepoint savepoint) throws SQLException
	{
		if (isFinished())
			throw new IllegalStateException("This transaction is already finished.");
		connection.rollback(savepoint);
	}
	
	/**
	 * Calls {@link Connection#setSavepoint()} on the encapsulated connection.
	 * @return a generated {@link Savepoint} of this transaction.
	 * @throws IllegalStateException if this transaction was already finished.
	 * @throws SQLException if this causes a database error.
	 */
	public Savepoint setSavepoint() throws SQLException
	{
		if (isFinished())
			throw new IllegalStateException("This transaction is already finished.");
		return connection.setSavepoint();
	}
	
	/**
	 * Calls {@link Connection#setSavepoint()} on the encapsulated connection.
	 * @param name the name of the savepoint.
	 * @return a generated {@link Savepoint} of this transaction.
	 * @throws IllegalStateException if this transaction was already finished.
	 * @throws SQLException if this causes a database error.
	 */
	public Savepoint setSavepoint(String name) throws SQLException
	{
		if (isFinished())
			throw new IllegalStateException("This transaction is already finished.");
		return connection.setSavepoint(name);
	}
	
	/**
	 * Performs a query on a connection and extracts the data into a single SQLRow.
	 * @param query the query statement to execute.
	 * @param parameters list of parameters for parameterized queries.
	 * @return the single result row returned, or null if no row returned.
	 * @throws SQLException if the query cannot be executed or the query causes an error.
	 */
	public SQLRow getSingle(String query, Object ... parameters) throws SQLException
	{
		return SQL.getSingle(connection, query, parameters);
	}

	/**
	 * Performs a query on a connection and creates an object from it from the first row, setting relevant fields.
	 * <p>
	 * Each result row is applied via the target object's public fields and setter methods.
	 * <p>
	 * For instance, if there is a column is a row called "color", its value
	 * will be applied via the public field "color" or the setter "setColor()". Public
	 * fields take precedence over setters.
	 * <p>
	 * Only certain types are converted without issue. Below is a set of source types
	 * and their valid target types:
	 * <table>
	 * <tr>
	 * 		<td><b>Boolean</b></td>
	 * 		<td>
	 * 			Boolean, all numeric primitives and their autoboxed equivalents, String. 
	 * 		</td>
	 * </tr>
	 * <tr>
	 * 		<td><b>Number</b></td>
	 * 		<td>
	 * 			Boolean (zero is false, nonzero is true), all numeric primitives and their autoboxed equivalents, String,
	 * 			Date, Timestamp. 
	 * 		</td>
	 * </tr>
	 * <tr>
	 * 		<td><b>Timestamp</b></td>
	 * 		<td>
	 * 			Long (both primitive and object as milliseconds since the Epoch), Timestamp, Date, String 
	 * 		</td>
	 * </tr>
	 * <tr>
	 * 		<td><b>Date</b></td>
	 * 		<td>
	 * 			Long (both primitive and object as milliseconds since the Epoch), Timestamp, Date, String 
	 * 		</td>
	 * </tr>
	 * <tr>
	 * 		<td><b>String</b></td>
	 * 		<td>
	 * 			Boolean, all numeric primitives and their autoboxed equivalents, 
	 * 			String, byte[], char[]. 
	 * 		</td>
	 * </tr>
	 * <tr>
	 * 		<td><b>Clob</b></td>
	 * 		<td>
	 * 			Boolean, all numeric primitives and their autoboxed equivalents, 
	 * 			String, byte[], char[]. 
	 * 		</td>
	 * </tr>
	 * <tr>
	 * 		<td><b>Blob</b></td>
	 * 		<td> 
	 * 			String, byte[], char[]. 
	 * 		</td>
	 * </tr>
	 * <tr>
	 * 		<td><b>Clob</b></td>
	 * 		<td>
	 * 			Boolean, all numeric primitives and their autoboxed equivalents, 
	 * 			String, byte[], char[]. 
	 * 		</td>
	 * </tr>
	 * <tr>
	 * 		<td><b>byte[]</b></td>
	 * 		<td>
	 *			String, byte[], char[]. 
	 * 		</td>
	 * </tr>
	 * <tr>
	 * 		<td><b>char[]</b></td>
	 * 		<td>
	 * 			Boolean, all numeric primitives and their autoboxed equivalents, 
	 * 			String, byte[], char[]. 
	 * 		</td>
	 * </tr>
	 * </table>
	 * @param type the class type to instantiate.
	 * @param query the query to execute.
	 * @param parameters list of parameters for parameterized queries.
	 * @return an instantiated object with the pertinent fields set, or null if no rows.
	 * @throws SQLException if the query cannot be executed or the query causes an error.
	 * @throws ClassCastException if one object type cannot be converted to another.
	 */
	public <T> T getSingle(Class<T> type, String query, Object ... parameters) throws SQLException
	{
		T result = null;
		try {
			result = SQL.getSingle(connection, type, query, parameters); 
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return result;
	}

	/**
	 * Performs a query on this transaction.
	 * @param query the query to execute.
	 * @param parameters list of parameters for parameterized queries.
	 * @return the result of the query.
	 * @throws SQLException if the query cannot be executed or the query causes an error.
	 */
	public SQLResult getMulti(String query, Object ... parameters) throws SQLException
	{
		return SQL.getMulti(connection, query, parameters);
	}

	/**
	 * Attempts to grab an available connection from the default servlet connection pool 
	 * and performs a query and creates objects from it, setting relevant fields.
	 * <p>
	 * Each result row is applied via the target object's public fields and setter methods.
	 * <p>
	 * For instance, if there is a column is a row called "color", its value
	 * will be applied via the public field "color" or the setter "setColor()". Public
	 * fields take precedence over setters.
	 * <p>
	 * Only certain types are converted without issue. Below is a set of source types
	 * and their valid target types:
	 * <table>
	 * <tr>
	 * 		<td><b>Boolean</b></td>
	 * 		<td>
	 * 			Boolean, all numeric primitives and their autoboxed equivalents, String. 
	 * 		</td>
	 * </tr>
	 * <tr>
	 * 		<td><b>Number</b></td>
	 * 		<td>
	 * 			Boolean (zero is false, nonzero is true), all numeric primitives and their autoboxed equivalents, String,
	 * 			Date, Timestamp. 
	 * 		</td>
	 * </tr>
	 * <tr>
	 * 		<td><b>Timestamp</b></td>
	 * 		<td>
	 * 			Long (both primitive and object as milliseconds since the Epoch), Timestamp, Date, String 
	 * 		</td>
	 * </tr>
	 * <tr>
	 * 		<td><b>Date</b></td>
	 * 		<td>
	 * 			Long (both primitive and object as milliseconds since the Epoch), Timestamp, Date, String 
	 * 		</td>
	 * </tr>
	 * <tr>
	 * 		<td><b>String</b></td>
	 * 		<td>
	 * 			Boolean, all numeric primitives and their autoboxed equivalents, 
	 * 			String, byte[], char[]. 
	 * 		</td>
	 * </tr>
	 * <tr>
	 * 		<td><b>Clob</b></td>
	 * 		<td>
	 * 			Boolean, all numeric primitives and their autoboxed equivalents, 
	 * 			String, byte[], char[]. 
	 * 		</td>
	 * </tr>
	 * <tr>
	 * 		<td><b>Blob</b></td>
	 * 		<td> 
	 * 			String, byte[], char[]. 
	 * 		</td>
	 * </tr>
	 * <tr>
	 * 		<td><b>Clob</b></td>
	 * 		<td>
	 * 			Boolean, all numeric primitives and their autoboxed equivalents, 
	 * 			String, byte[], char[]. 
	 * 		</td>
	 * </tr>
	 * <tr>
	 * 		<td><b>byte[]</b></td>
	 * 		<td>
	 *			String, byte[], char[]. 
	 * 		</td>
	 * </tr>
	 * <tr>
	 * 		<td><b>char[]</b></td>
	 * 		<td>
	 * 			Boolean, all numeric primitives and their autoboxed equivalents, 
	 * 			String, byte[], char[]. 
	 * 		</td>
	 * </tr>
	 * </table>
	 * @param type the class type to instantiate.
	 * @param query the query to execute.
	 * @param parameters list of parameters for parameterized queries.
	 * @return an array of instantiated objects with the pertinent fields set for each row.
	 * @throws SQLException if the query cannot be executed or the query causes an error.
	 * @throws ClassCastException if one object type cannot be converted to another.
	 */
	public <T> T[] getMulti(Class<T> type, String query, Object ... parameters) throws SQLException
	{
		return SQL.getMulti(connection, type, query, parameters);
	}

	/**
	 * Performs an update query on this transaction.
	 * @param query the query to execute.
	 * @param parameters list of parameters for parameterized queries.
	 * @return the update result returned (usually number of rows affected and or generated ids).
	 * @throws SQLException if the query cannot be executed or the query causes an error.
	 */
	public SQLResult update(String query, Object ... parameters) throws SQLException
	{
		return SQL.update(connection, query, parameters);
	}

	/**
	 * If this transaction is not finished, this aborts it.
	 * @see AutoCloseable#close()
	 * @see #isFinished()
	 * @see #abort()
	 */
	@Override
	public void close()
	{
		if (!isFinished())
		{
			try {abort();} catch (SQLException e) { /* Eat exception. */ }
		}
	}
	
}