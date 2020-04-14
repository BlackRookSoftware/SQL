/*******************************************************************************
 * Copyright (c) 2019-2020 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.sql.util;

import java.io.IOException;
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
	 * Creates a new SQLRuntimeException from an {@link IOException}.
	 * @param e the exception to wrap.
	 * @since 1.1.0
	 */
	public SQLRuntimeException(IOException e)
	{
		super(e);
	}
	
	/**
	 * Creates a new SQLRuntimeException from a {@link SQLException}.
	 * @param message the message.
	 * @param e the exception to wrap.
	 * @since 1.1.0
	 */
	public SQLRuntimeException(String message, SQLException e)
	{
		super(message, e);
	}
	
	/**
	 * Creates a new SQLRuntimeException from an {@link IOException}.
	 * @param message the message.
	 * @param e the exception to wrap.
	 * @since 1.1.0
	 */
	public SQLRuntimeException(String message, IOException e)
	{
		super(message, e);
	}
	
	/**
	 * @return the SQLException that caused this exception.
	 */
	public SQLException getSQLException()
	{
		return (SQLException)getCause();
	}
	
}
