package com.blackrook.sql.util;

import java.sql.SQLException;

import com.blackrook.sql.SQLConnection;

/**
 * A special consumer that takes a SQLConnection, but throws {@link SQLException}s. 
 */
@FunctionalInterface
public interface SQLConnectionConsumer
{
	/**
	 * Accepts the connection, does something with it, and returns nothing. 
	 * @param connection the open connection.
	 * @throws SQLException if a SQLException occurs.
	 */
	void accept(SQLConnection connection) throws SQLException;
	
}

