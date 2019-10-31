package com.blackrook.sql.util;

import java.sql.SQLException;

/**
 * An exception thrown to wrap a SQLException. 
 * @author Matthew Tropiano
 */
public class SQLRuntimeException extends RuntimeException
{
	private static final long serialVersionUID = 4210466948855267673L;

	/**
	 * Creates a new SQLRuntimeException from a {@link SQLException}.
	 * @param e the exception to wrap.
	 */
	public SQLRuntimeException(SQLException e)
	{
		super(e);
	}
	
	/**
	 * @return the SQLException that caused this exception.
	 */
	public SQLException getSQLException()
	{
		return (SQLException)getCause();
	}
	
}
