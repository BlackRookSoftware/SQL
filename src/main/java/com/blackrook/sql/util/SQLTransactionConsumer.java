package com.blackrook.sql.util;

import java.sql.SQLException;

import com.blackrook.sql.SQLConnection.Transaction;

/**
 * A special consumer that takes a SQLTransaction, but throws {@link SQLException}s. 
 */
@FunctionalInterface
public interface SQLTransactionConsumer
{
	/**
	 * Accepts the transaction, does something with it, and returns nothing. 
	 * @param transaction the open connection.
	 * @throws SQLException if a SQLException occurs.
	 */
	void accept(Transaction transaction) throws SQLException;
	
}

