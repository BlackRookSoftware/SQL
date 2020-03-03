/*******************************************************************************
 * Copyright (c) 2019-2020 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
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

