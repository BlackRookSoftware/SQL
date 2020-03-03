/*******************************************************************************
 * Copyright (c) 2019-2020 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.sql;

import java.io.File;
import java.sql.SQLException;

public final class SQLTest
{
	public static class Data
	{
		public long id;
		public String value;
	}
	
	@SuppressWarnings("unused")
	public static void main(String[] args) throws SQLException
	{
		SQLConnector connector = new SQLConnector("org.sqlite.JDBC", "jdbc:sqlite:./test.db");
		try 
		{
			connector.getConnectionAnd((c)->
			{
				c.getUpdateResult("CREATE TABLE test (id INTEGER PRIMARY KEY AUTOINCREMENT, value TEXT DEFAULT NULL)");
				c.getUpdateResult("INSERT INTO test (value) VALUES ('apple')");
				SQLResult r = c.getUpdateResult("INSERT INTO test (value) VALUES ('banana')");
				c.getUpdateResult("INSERT INTO test (value) VALUES ('coffee')");
				c.getUpdateResult("INSERT INTO test (value) VALUES ('durian')");
				c.getUpdateResult("INSERT INTO test (value) VALUES ('eucalyptus')");
				SQLRow r2 = c.getRow("SELECT * FROM test WHERE id = ?", 3);
				Data d = c.getRow(Data.class, "SELECT * FROM test WHERE id = ?", 3);
				SQLResult r3 = c.getResult("SELECT * FROM test");
				Data[] e = c.getResult(Data.class, "SELECT * FROM test");
				System.out.println();
			});
		} 
		finally 
		{
			new File("test.db").delete();
		}
	}
}
