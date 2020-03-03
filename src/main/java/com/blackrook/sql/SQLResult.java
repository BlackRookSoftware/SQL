/*******************************************************************************
 * Copyright (c) 2019-2020 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.sql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The data encapsulation of the result of a query {@link java.sql.ResultSet}.
 * @author Matthew Tropiano
 */
public class SQLResult implements Iterable<SQLRow>
{
	private static final String[] EMPTY_ARRAY = new String[0];
	
	/** Query Columns. */
	protected String[] columnNames;
	/** Rows affected or returned in the query. */
	protected int rowCount;
	/** Next id, if generated. */
	protected long[] nextId;
	/** Was this an update query? */
	protected boolean update;
	/** List of rows of associative data. */
	protected List<SQLRow> rows;
	
	/**
	 * Creates a new query result from an update query, plus generated keys. 
	 */
	SQLResult(int rowsAffected, ResultSet genKeys) throws SQLException
	{
		this.columnNames = EMPTY_ARRAY;
		this.update = true;
		this.rowCount = rowsAffected;
		this.rows = null;
		
		List<Long> vect = new ArrayList<Long>();
		while (genKeys.next())
			vect.add(genKeys.getLong(1));
		
		this.nextId = new long[vect.size()];
		int x = 0;
		for (long lng : vect)
			this.nextId[x++] = lng; 
	}

	/**
	 * Creates a new query result from a result set. 
	 */
	SQLResult(ResultSet rs) throws SQLException
	{
		this.columnNames = SQL.getAllColumnNamesFromResultSet(rs);
		this.update = false;
		this.rowCount = 0;
		this.rows = new ArrayList<SQLRow>();
		
		while (rs.next())
		{
			this.rows.add(new SQLRow(rs, columnNames));
			this.rowCount++;
		}
	}
	
	/**
	 * Gets the names of the columns.
	 * @return the column names in this result.
	 */
	public String[] getColumnNames()
	{
		return columnNames;
	}

	/**
	 * Gets the amount of affected/returned rows from this query. 
	 * @return the amount of records in this result.
	 */
	public int getRowCount()
	{
		return rowCount;
	}

	/**
	 * @return true if this came from an update, false otherwise.
	 */
	public boolean isUpdate()
	{
		return update;
	}
	
	/**
	 * Retrieves the rows from the query result.
	 * @return a list of the rows in this result.
	 */
	public List<SQLRow> getRows()
	{
		return rows;
	}

	/**
	 * Gets the first row, or only row in this result, or null if no rows.
	 * @return the row in this result.
	 */
	public SQLRow getRow()
	{
		return rows.size() > 0 ? rows.get(0) : null;
	}
	
	/**
	 * @return the generated id from the last query, if any, or null if none.
	 */
	public Long getId()
	{
		return nextId.length > 0 ? nextId[0] : null;
	}
	
	/**
	 * @return the list of generated ids from the last query.
	 */
	public long[] getIds()
	{
		return nextId;
	}
	
	@Override
	public SQLResultIterator iterator()
	{
		return new SQLResultIterator();
	}

	/**
	 * The iterator returned to iterate through this result. 
	 */
	public class SQLResultIterator implements Iterator<SQLRow>
	{
		private int current;
		
		public void reset()
		{
			current = 0;
		}
		
		@Override
		public boolean hasNext()
		{
			return current < rows.size();
		}

		@Override
		public SQLRow next()
		{
			return rows.get(current++);
		}	
	}
	
}
