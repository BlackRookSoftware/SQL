/*******************************************************************************
 * Copyright (c) 2019 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
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
	/** Set of hash maps of associative data. */
	protected List<SQLRow> rows;
	
	/**
	 * Creates a new query result from an update query. 
	 */
	public SQLResult(int rowsAffected) throws SQLException
	{
		this.columnNames = EMPTY_ARRAY;
		this.update = true;
		this.rowCount = rowsAffected;
		this.rows = null;
	}

	/**
	 * Creates a new query result from an update query, plus generated keys. 
	 */
	public SQLResult(int rowsAffected, ResultSet genKeys) throws SQLException
	{
		this(rowsAffected);
		
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
	public SQLResult(ResultSet rs) throws SQLException
	{
		this.update = false;
		this.rowCount = 0;

		this.columnNames = SQL.getAllColumnNamesFromResultSet(rs);

		this.rows = new ArrayList<SQLRow>();
		while (rs.next())
		{
			this.rows.add(new SQLRow(rs, columnNames));
			this.rowCount++;
		}
	}
	
	/**
	 * Gets the names of the columns.
	 */
	public String[] getColumnNames()
	{
		return columnNames;
	}

	/**
	 * Gets the amount of affected/returned rows from this query. 
	 */
	public int getRowCount()
	{
		return rowCount;
	}

	/**
	 * Returns true if this came from an update.
	 */
	public boolean isUpdate()
	{
		return update;
	}
	
	/**
	 * Retrieves the rows from the query result.
	 */
	public List<SQLRow> getRows()
	{
		return rows;
	}

	/**
	 * Gets the first row, or only row in this result,
	 * or null if no rows.
	 * @return the row in this result.
	 */
	public SQLRow getRow()
	{
		return rows.size() > 0 ? rows.get(0) : null;
	}
	
	/**
	 * @return the generated id from the last query, if any, or 0L if none.
	 */
	public long getId()
	{
		return nextId.length > 0 ? nextId[0] : 0L;
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
