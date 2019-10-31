package com.blackrook.sql;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;

import com.blackrook.sql.util.SQLTransactionConsumer;
import com.blackrook.sql.util.SQLTransactionFunction;

/**
 * A wrapped SQL connection for ease-of-querying.
 * @author Matthew Tropiano
 */
public class SQLConnection implements SQLCallable, AutoCloseable
{
	/** 
	 * Enumeration of transaction levels. 
	 */
	public enum TransactionLevel
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
		private TransactionLevel(int id)
		{
			this.id = id;
		}
	}

	/** The encapsulated connection. */
	Connection connection;
	/** The current transaction on this connection. */
	private Transaction transaction;
		
	SQLConnection(Connection connection)
	{
		this.connection = connection;
	}

	/**
	 * Starts a transaction with an provided level.
	 * <p>While this transaction is active, calls to this connection's query handling methods will throw an {@link IllegalStateException}.
	 * <p>The connection gets {@link Connection#setAutoCommit(boolean)} called on it with a FALSE parameter,
	 * and sets the transaction isolation level. These settings are restored when the transaction is 
	 * finished via {@link Transaction#close()}, {@link Transaction#commit()}, or {@link Transaction#abort()}.
	 * It is recommended to use an auto-closing mechanism to ensure that the transaction is completed and the connection transaction
	 * state is restored.
	 * @param transactionLevel the transaction level to set on this transaction.
	 * @return a new transaction.
	 * @throws SQLException if this transaction could not be prepared.
	 */
	public Transaction startTransaction(TransactionLevel transactionLevel) throws SQLException
	{
		return new Transaction(transactionLevel);
	}

	/**
	 * Starts a transaction with an provided level, performs actions on it, then auto-closes it.
	 * <p>While this transaction is active, calls to this connection's query handling methods will throw an {@link IllegalStateException}.
	 * <p>The connection gets {@link Connection#setAutoCommit(boolean)} called on it with a FALSE parameter,
	 * and sets the transaction isolation level. These settings are restored when the transaction is 
	 * finished via {@link Transaction#close()}, {@link Transaction#commit()}, or {@link Transaction#abort()}.
	 * It is recommended to use an auto-closing mechanism to ensure that the transaction is completed and the connection transaction
	 * state is restored.
	 * @param transactionLevel the transaction level to set on this transaction.
	 * @param handler the consumer function that accepts the retrieved connection and returns a value.
	 * @throws SQLException if this transaction could not be prepared.
	 */
	public void startTransactionAnd(TransactionLevel transactionLevel, SQLTransactionConsumer handler) throws SQLException
	{
		try (Transaction transaction = new Transaction(transactionLevel))
		{
			handler.accept(transaction);
		}
	}

	/**
	 * Starts a transaction with an provided level, performs actions on it, returns a value, then auto-closes it.
	 * <p>While this transaction is active, calls to this connection's query handling methods will throw an {@link IllegalStateException}.
	 * <p>The connection gets {@link Connection#setAutoCommit(boolean)} called on it with a FALSE parameter,
	 * and sets the transaction isolation level. These settings are restored when the transaction is 
	 * finished via {@link Transaction#close()}, {@link Transaction#commit()}, or {@link Transaction#abort()}.
	 * It is recommended to use an auto-closing mechanism to ensure that the transaction is completed and the connection transaction
	 * state is restored.
	 * @param <R> the return type.
	 * @param transactionLevel the transaction level to set on this transaction.
	 * @param handler the consumer function that accepts the retrieved connection and returns a value.
	 * @return the return value of the handler function.
	 * @throws SQLException if this transaction could not be prepared.
	 */
	public <R> R startTransactionAnd(TransactionLevel transactionLevel, SQLTransactionFunction<R> handler) throws SQLException
	{
		try (Transaction transaction = new Transaction(transactionLevel))
		{
			return handler.apply(transaction);
		}
	}

	/**
	 * Ends the transaction (called by SQLTransaction, SQLPool).
	 */
	void endTransaction()
	{
		if (transaction != null)
		{
			transaction.close();
			transaction = null;
		}
	}
	
	/**
	 * @return true if this connection is in a transaction, false if not.
	 */
	public boolean inTransaction()
	{
		return transaction != null;
	}
	
	@Override
	public SQLRow getRow(String query, Object ... parameters)
	{
		if (inTransaction())
			throw new IllegalStateException("A transaction is active and must be closed before this can be called.");
		return SQL.getRow(connection, query, parameters);
	}

	@Override
	public <T> T getRow(Class<T> type, String query, Object ... parameters)
	{
		if (inTransaction())
			throw new IllegalStateException("A transaction is active and must be closed before this can be called.");
		return SQL.getRow(connection, type, query, parameters);
	}

	@Override
	public SQLResult getResult(String query, Object ... parameters)
	{
		if (inTransaction())
			throw new IllegalStateException("A transaction is active and must be closed before this can be called.");
		return SQL.getResult(connection, query, parameters);
	}

	@Override
	public <T> T[] getResult(Class<T> type, String query, Object ... parameters)
	{
		if (inTransaction())
			throw new IllegalStateException("A transaction is active and must be closed before this can be called.");
		return SQL.getResult(connection, type, query, parameters);
	}

	@Override
	public SQLResult getUpdateResult(String query, Object ... parameters)
	{
		if (inTransaction())
			throw new IllegalStateException("A transaction is active and must be closed before this can be called.");
		return SQL.getUpdateResult(connection, query, parameters);
	}

	/**
	 * @return true if this connection is closed, false if open.
	 * @throws SQLException if checking the connection status results in an error.
	 */
	public boolean isClosed() throws SQLException
	{
		return connection.isClosed();
	}
	
	/**
	 * Closes this connection.
	 * If a transaction is active, and the transaction is not finished, this aborts it.
	 * @see Transaction#abort()
	 */
	@Override
	public void close()
	{
		try {
			endTransaction();
			if (!connection.isClosed())
				connection.close();
		} catch (SQLException e) {
			// Do nothing.
		}
	}

	/**
	 * A transaction object that holds a connection that guarantees an isolation level
	 * of some kind. Queries can be made through this object until it has been released. 
	 * <p>
	 * This object's {@link #finalize()} method attempts to roll back the transaction if it hasn't already
	 * been finished.
	 * @author Matthew Tropiano
	 */
	public class Transaction implements SQLCallable, AutoCloseable
	{
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
		private Transaction(TransactionLevel transactionLevel) throws SQLException
		{
			this.previousLevelState = connection.getTransactionIsolation();
			this.previousAutoCommit = connection.getAutoCommit();
			connection.setAutoCommit(false);
			connection.setTransactionIsolation(transactionLevel.id);
		}

		@Override
		public SQLRow getRow(String query, Object... parameters)
		{
			return SQL.getRow(connection, query, parameters);
		}

		@Override
		public <T> T getRow(Class<T> type, String query, Object... parameters)
		{
			return SQL.getRow(connection, type, query, parameters);
		}

		@Override
		public SQLResult getResult(String query, Object... parameters)
		{
			return SQL.getResult(connection, query, parameters);
		}

		@Override
		public <T> T[] getResult(Class<T> type, String query, Object... parameters)
		{
			return SQL.getResult(connection, type, query, parameters);
		}

		@Override
		public SQLResult getUpdateResult(String query, Object... parameters)
		{
			return SQL.getUpdateResult(connection, query, parameters);
		}

		/**
		 * @return true if this transaction has been completed or false if more methods can be invoked on it.
		 */
		public boolean isFinished()
		{
			return inTransaction(); 
		}	
		
		private void finish() throws SQLException
		{
			connection.setTransactionIsolation(previousLevelState);
			connection.setAutoCommit(previousAutoCommit);
			endTransaction();
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
			finish();
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
			finish();
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
}
