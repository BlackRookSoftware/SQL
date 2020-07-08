/*******************************************************************************
 * Copyright (c) 2019-2020 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.sql;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.blackrook.sql.struct.Utils;
import com.blackrook.sql.util.SQLRuntimeException;

/**
 * SQLRow object. 
 * Represents one row in a query result, mapped using column names.
 * Contains methods for auto-casting or converting the row data.
 */
public class SQLRow
{
	private static final ThreadLocal<char[]> CHARBUFFER = ThreadLocal.withInitial(()->new char[1024 * 8]);
	private static final ThreadLocal<byte[]> BYTEBUFFER = ThreadLocal.withInitial(()->new byte[1024 * 32]);
	
	/** Column index to SQL object. */
	private List<Object> columnList;
	/** Map of column name to index. */
	private Map<String, Integer> columnMap;
	
	/**
	 * Constructor for a SQL row.
	 * @param rs the open {@link ResultSet}, set to the row to create a SQLRow from.
	 * @param columnNames the names given to the columns in the {@link ResultSet}, gathered ahead of time.
	 * @throws SQLException if a parse exception occurred.
	 */
	SQLRow(ResultSet rs, String[] columnNames) throws SQLException
	{
		this.columnList = new ArrayList<>(rs.getMetaData().getColumnCount());
		this.columnMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		for (int i = 0; i < columnNames.length; i++)
		{
			Object sqlobj = rs.getObject(i + 1); // 1-based
			
			// Blobs, Clobs, and NClobs need to be converted while the connection is open.
			if (sqlobj instanceof Blob)
				sqlobj = getByteArray(sqlobj);
			else if (sqlobj instanceof Clob)
				sqlobj = getString(sqlobj);
			
			columnList.add(sqlobj);
			columnMap.put(columnNames[i], i);
		}
	}
	
	// Get a column by index.
	private Object getByIndex(Integer columnIndex)
	{
		if (columnIndex == null || columnIndex < 0 || columnIndex >= columnList.size())
			return null;
		return columnList.get(columnIndex);
	}

	// Get a column by name.
	private Object getByName(String columnName)
	{
		return getByIndex(columnMap.get(columnName));
	}
	
	/**
	 * Gets if a column's value is null.
	 * @param columnIndex the column index to read (0-based).
	 * @return the resultant value, or null if not a valid column name.
	 */
	public boolean getNull(int columnIndex)
	{
		return getByIndex(columnIndex) == null;
	}

	/**
	 * Gets if a column's value is null.
	 * @param columnName the column name to read (case-insensitive).
	 * @return the resultant value, or null if not a valid column name.
	 */
	public boolean getNull(String columnName)
	{
		return getByName(columnName) == null;
	}

	/**
	 * Gets the boolean value of a column.
	 * Can convert from Booleans, Numbers, and Strings.
	 * @param columnIndex the column index to read (0-based).
	 * @return the resultant value, or null if not a valid column name.
	 */
	public boolean getBoolean(int columnIndex)
	{
		return getBoolean(getByIndex(columnIndex));
	}

	/**
	 * Gets the boolean value of a column.
	 * Can convert from Booleans, Numbers, and Strings.
	 * @param columnName the column name to read (case-insensitive).
	 * @return the resultant value, or null if not a valid column name.
	 */
	public boolean getBoolean(String columnName)
	{
		return getBoolean(getByName(columnName));
	}
	
	/**
	 * Gets the byte value of a column.
	 * Can convert from Booleans, Numbers, and Strings.
	 * Booleans convert to 1 if true, 0 if false.
	 * @param columnIndex the column index to read (0-based).
	 * @return the resultant value, or null if not a valid column name.
	 */
	public byte getByte(int columnIndex)
	{
		return getByte(getByIndex(columnIndex));
	}
	
	/**
	 * Gets the byte value of a column.
	 * Can convert from Booleans, Numbers, and Strings.
	 * Booleans convert to 1 if true, 0 if false.
	 * @param columnName the column name to read (case-insensitive).
	 * @return the resultant value, or null if not a valid column name.
	 */
	public byte getByte(String columnName)
	{
		return getByte(getByName(columnName));
	}
	
	/**
	 * Gets the byte array value of a column, if this
	 * can be represented as such (usually {@link Blob}s).
	 * Can convert from Blobs.
	 * @param columnIndex the column index to read (0-based).
	 * @return the resultant value, or null if not a valid column name.
	 */
	public byte[] getByteArray(int columnIndex)
	{
		return getByteArray(getByIndex(columnIndex));
	}
	
	/**
	 * Gets the byte array value of a column, if this
	 * can be represented as such (usually {@link Blob}s).
	 * Can convert from Blobs.
	 * @param columnName the column name to read (case-insensitive).
	 * @return the resultant value, or null if not a valid column name.
	 */
	public byte[] getByteArray(String columnName)
	{
		return getByteArray(getByName(columnName));
	}

	/**
	 * Gets the short value of a column.
	 * Can convert from Booleans, Numbers, and Strings.
	 * Booleans convert to 1 if true, 0 if false.
	 * @param columnIndex the column index to read (0-based).
	 * @return the resultant value, or null if not a valid column name.
	 */
	public short getShort(int columnIndex)
	{
		return getShort(getByIndex(columnIndex));
	}
	
	/**
	 * Gets the short value of a column.
	 * Can convert from Booleans, Numbers, and Strings.
	 * Booleans convert to 1 if true, 0 if false.
	 * @param columnName the column name to read (case-insensitive).
	 * @return the resultant value, or null if not a valid column name.
	 */
	public short getShort(String columnName)
	{
		return getShort(getByName(columnName));
	}

	/**
	 * Gets the integer value of a column.
	 * Can convert from Booleans, Numbers, and Strings.
	 * Booleans convert to 1 if true, 0 if false.
	 * @param columnIndex the column index to read (0-based).
	 * @return the resultant value, or null if not a valid column name.
	 */
	public int getInt(int columnIndex)
	{
		return getInt(getByIndex(columnIndex));		
	}
	
	/**
	 * Gets the integer value of a column.
	 * Can convert from Booleans, Numbers, and Strings.
	 * Booleans convert to 1 if true, 0 if false.
	 * @param columnName the column name to read (case-insensitive).
	 * @return the resultant value, or null if not a valid column name.
	 */
	public int getInt(String columnName)
	{
		return getInt(getByName(columnName));
	}

	/**
	 * Gets the float value of a column.
	 * Can convert from Booleans, Numbers, and Strings.
	 * Booleans convert to 1 if true, 0 if false.
	 * @param columnIndex the column index to read (0-based).
	 * @return the resultant value, or null if not a valid column name.
	 */
	public float getFloat(int columnIndex)
	{
		return getFloat(getByIndex(columnIndex));
	}

	/**
	 * Gets the float value of a column.
	 * Can convert from Booleans, Numbers, and Strings.
	 * Booleans convert to 1 if true, 0 if false.
	 * @param columnName the column name to read (case-insensitive).
	 * @return the resultant value, or null if not a valid column name.
	 */
	public float getFloat(String columnName)
	{
		return getFloat(getByName(columnName));
	}

	/**
	 * Gets the long value of a column.
	 * Can convert from Booleans, Numbers, Strings, and Dates/Timestamps.
	 * Booleans convert to 1 if true, 0 if false.
	 * Dates and Timestamps convert to milliseconds since the Epoch.
	 * @param columnIndex the column index to read (0-based).
	 * @return the resultant value, or null if not a valid column name.
	 */
	public long getLong(int columnIndex)
	{
		return getLong(getByIndex(columnIndex));
	}

	/**
	 * Gets the long value of a column.
	 * Can convert from Booleans, Numbers, Strings, and Dates/Timestamps.
	 * Booleans convert to 1 if true, 0 if false.
	 * Dates and Timestamps convert to milliseconds since the Epoch.
	 * @param columnName the column name to read (case-insensitive).
	 * @return the resultant value, or null if not a valid column name.
	 */
	public long getLong(String columnName)
	{
		return getLong(getByName(columnName));
	}

	/**
	 * Gets the double value of a column.
	 * Can convert from Booleans, Numbers, and Strings.
	 * Booleans convert to 1 if true, 0 if false.
	 * @param columnIndex the column index to read (0-based).
	 * @return the resultant value, or null if not a valid column name.
	 */
	public double getDouble(int columnIndex)
	{
		return getDouble(getByIndex(columnIndex));
	}
	
	/**
	 * Gets the double value of a column.
	 * Can convert from Booleans, Numbers, and Strings.
	 * Booleans convert to 1 if true, 0 if false.
	 * @param columnName the column name to read (case-insensitive).
	 * @return the resultant value, or null if not a valid column name.
	 */
	public double getDouble(String columnName)
	{
		return getDouble(getByName(columnName));
	}

	/**
	 * Gets the string value of a column.
	 * Can convert from Booleans, Numbers, byte and char arrays, Blobs, and Clobs.
	 * Booleans convert to 1 if true, 0 if false.
	 * Byte arrays and Blobs are converted using the native charset encoding.
	 * Char arrays and Clobs are read entirely and converted to Strings.
	 * @param columnIndex the column index to read (0-based).
	 * @return the resultant value, or null if not a valid column name.
	 * @see String#valueOf(Object)
	 */
	public String getString(int columnIndex)
	{
		return getString(getByIndex(columnIndex));
	}

	/**
	 * Gets the string value of a column.
	 * Can convert from Booleans, Numbers, byte and char arrays, Blobs, and Clobs.
	 * Booleans convert to 1 if true, 0 if false.
	 * Byte arrays and Blobs are converted using the native charset encoding.
	 * Char arrays and Clobs are read entirely and converted to Strings.
	 * @param columnName the column name to read (case-insensitive).
	 * @return the resultant value, or null if not a valid column name.
	 * @see String#valueOf(Object)
	 */
	public String getString(String columnName)
	{
		return getString(getByName(columnName));
	}

	/**
	 * Gets the Timestamp value of the object, or null if not a Timestamp or Date.
	 * @param columnIndex the column index to read (0-based).
	 * @return the resultant value, or null if not a valid column name.
	 */
	public Timestamp getTimestamp(int columnIndex)
	{
		return getTimestamp(getByIndex(columnIndex));
	}

	/**
	 * Gets the Timestamp value of the object, or null if not a Timestamp or Date.
	 * @param columnName the column name to read (case-insensitive).
	 * @return the resultant value, or null if not a valid column name.
	 */
	public Timestamp getTimestamp(String columnName)
	{
		return getTimestamp(getByName(columnName));
	}

	/**
	 * Gets the Date value of the object, or null if not a Date.
	 * @param columnIndex the column index to read (0-based).
	 * @return the resultant value, or null if not a valid column name.
	 */
	public Date getDate(int columnIndex)
	{
		return getDate(getByIndex(columnIndex));
	}
	
	/**
	 * Gets the Date value of the object, or null if not a Date.
	 * @param columnName the column name to read (case-insensitive).
	 * @return the resultant value, or null if not a valid column name.
	 */
	public Date getDate(String columnName)
	{
		return getDate(getByName(columnName));
	}

	private boolean getBoolean(Object obj)
	{
		if (obj == null)
			return false;
		else if (obj instanceof Boolean)
			return (Boolean)obj;
		else if (obj instanceof Number)
			return ((Number)obj).doubleValue() != 0.0f;
		else if (obj instanceof String)
			return Utils.parseBoolean((String)obj);
		return false;
	}

	private byte getByte(Object obj)
	{
		if (obj == null)
			return (byte)0;
		else if (obj instanceof Boolean)
			return ((Boolean)obj) ? (byte)1 : (byte)0;
		else if (obj instanceof Number)
			return ((Number)obj).byteValue();
		else if (obj instanceof String)
			return Utils.parseByte((String)obj);
		return (byte)0;
	}

	private byte[] getByteArray(Object obj)
	{
		if (obj == null)
			return null;
		else if (obj instanceof Blob)
		{
			Blob blob = (Blob)obj;
			ByteArrayOutputStream bos = null;
			try (InputStream in = blob.getBinaryStream()) 
			{
				bos = new ByteArrayOutputStream();
				byte[] buffer = BYTEBUFFER.get();
				int buf = 0;
				while ((buf = in.read(buffer)) > 0)
					bos.write(buffer, 0, buf);
			} 
			catch (SQLException e) 
			{
				throw new SQLRuntimeException("Blob conversion to String failed.", e);
			}
			catch (IOException e) 
			{
				throw new SQLRuntimeException("Blob conversion to String failed.", e);
			}
			
			return bos.toByteArray();
		}
		return null;
	}

	private short getShort(Object obj)
	{
		if (obj == null)
			return (short)0;
		else if (obj instanceof Boolean)
			return ((Boolean)obj) ? (short)1 : (short)0;
		else if (obj instanceof Number)
			return ((Number)obj).shortValue();
		else if (obj instanceof String)
			return Utils.parseShort((String)obj);
		return (short)0;
	}

	private int getInt(Object obj)
	{
		if (obj == null)
			return 0;
		else if (obj instanceof Boolean)
			return ((Boolean)obj) ? 1 : 0;
		else if (obj instanceof Number)
			return ((Number)obj).intValue();
		else if (obj instanceof String)
			return Utils.parseInt((String)obj);
		return 0;
	}

	private float getFloat(Object obj)
	{
		if (obj == null)
			return 0f;
		else if (obj instanceof Boolean)
			return ((Boolean)obj) ? 1f : 0f;
		else if (obj instanceof Number)
			return ((Number)obj).floatValue();
		else if (obj instanceof String)
			return Utils.parseFloat((String)obj);
		return 0f;
	}

	private long getLong(Object obj)
	{
		if (obj == null)
			return 0L;
		else if (obj instanceof Boolean)
			return ((Boolean)obj) ? 1L : 0L;
		else if (obj instanceof Number)
			return ((Number)obj).longValue();
		else if (obj instanceof String)
			return Utils.parseLong((String)obj);
		else if (obj instanceof Date)
			return ((Date)obj).getTime();
		return 0L;
	}

	private double getDouble(Object obj)
	{
		if (obj == null)
			return 0.0;
		else if (obj instanceof Boolean)
			return ((Boolean)obj) ? 1.0 : 0.0;
		else if (obj instanceof Number)
			return ((Number)obj).doubleValue();
		else if (obj instanceof String)
			return Utils.parseDouble((String)obj);
		return 0.0;
	}

	private String getString(Object obj)
	{
		if (Utils.isArray(obj))
		{
			if (Utils.getArrayType(obj) == Byte.TYPE)
				return new String((byte[])obj);
			else if (Utils.getArrayType(obj) == Character.TYPE)
				return new String((char[])obj);
			else
				return null;
		}
		else if (obj instanceof Clob)
		{
			Clob clob = (Clob)obj;
			StringWriter sw = null;
			try (Reader reader = clob.getCharacterStream()) 
			{
				sw = new StringWriter();
				char[] charBuffer = CHARBUFFER.get();
				int cbuf = 0;
				while ((cbuf = reader.read(charBuffer)) > 0)
					sw.write(charBuffer, 0, cbuf);
			} 
			catch (SQLException e) 
			{
				throw new SQLRuntimeException("Clob conversion to String failed.", e);
			}
			catch (IOException e) 
			{
				throw new SQLRuntimeException("Clob conversion to String failed.", e);
			}
			
			return sw.toString();
		}
		else if (obj instanceof Blob)
		{
			Blob blob = (Blob)obj;
			ByteArrayOutputStream bos = null;
			try (InputStream in = blob.getBinaryStream()) 
			{
				bos = new ByteArrayOutputStream();
				byte[] buffer = BYTEBUFFER.get();
				int buf = 0;
				while ((buf = in.read(buffer)) > 0)
					bos.write(buffer, 0, buf);
			}
			catch (SQLException e) 
			{
				throw new SQLRuntimeException("Blob conversion to String failed.", e);
			}
			catch (IOException e) 
			{
				throw new SQLRuntimeException("Blob conversion to String failed.", e);
			}
			
			return new String(bos.toByteArray());
		}
		else
			return obj != null ? String.valueOf(obj) : null;
	}

	private Timestamp getTimestamp(Object obj)
	{
		if (obj instanceof Timestamp)
			return ((Timestamp)obj);
		else if (obj instanceof Date)
			return new Timestamp(((Date)obj).getTime());
		return null;
	}

	private Date getDate(Object obj)
	{
		if (obj instanceof Date)
			return (Date)obj;
		return null;
	}
	
}

