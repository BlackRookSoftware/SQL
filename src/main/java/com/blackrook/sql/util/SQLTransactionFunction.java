/*******************************************************************************
 * Copyright (c) 2019-2022 Black Rook Software
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
 * @param <R> the result type.
 */
@FunctionalInterface
public interface SQLTransactionFunction<R>
{
	/**
	 * Accepts the transaction, does something with it, and returns a result. 
	 * @param transaction the open connection.
	 * @return the result from the call.
	 * @throws SQLException if a SQLException occurs.
	 */
	R apply(Transaction transaction) throws SQLException;
	
}

