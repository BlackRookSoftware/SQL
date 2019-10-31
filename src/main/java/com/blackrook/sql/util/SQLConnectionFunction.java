package com.blackrook.sql.util;

import java.sql.SQLException;

import com.blackrook.sql.SQLConnection;

/**
 * A special consumer that takes a SQLConnection, but throws {@link SQLException}s. 
 * @param <R> the result type.
 */
@FunctionalInterface
public interface SQLConnectionFunction<R>
{
	/**
	 * Accepts the connection, does something with it, and returns a result. 
	 * @param connection the open connection.
	 * @return the result from the call.
	 * @throws SQLException if a SQLException occurs.
	 */
	R apply(SQLConnection connection) throws SQLException;
	
}

