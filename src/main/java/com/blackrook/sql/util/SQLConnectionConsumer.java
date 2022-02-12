/*******************************************************************************
 * Copyright (c) 2019-2022 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
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

