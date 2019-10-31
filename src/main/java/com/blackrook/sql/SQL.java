/*******************************************************************************
 * Copyright (c) 2019 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.sql;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.blackrook.sql.hints.SQLIgnore;
import com.blackrook.sql.hints.SQLName;
import com.blackrook.sql.struct.Utils;
import com.blackrook.sql.util.SQLRuntimeException;

/**
 * Core database utilities object.
 * @author Matthew Tropiano
 */
public final class SQL
{
	private SQL() {}
	
	/** Default converter for {@link #createForType(Object, Class)}. */
	private static final SQLTypeProfileFactory PROFILE_FACTORY = new SQLTypeProfileFactory(new SQLTypeProfileFactory.MemberPolicy()
	{
		@Override
		public boolean isIgnored(Field field)
		{
			return field.getAnnotation(SQLIgnore.class) != null;
		}
	
		@Override
		public boolean isIgnored(Method method)
		{
			return method.getAnnotation(SQLIgnore.class) != null;
		}
	
		@Override
		public String getAlias(Field field)
		{
			SQLName anno = field.getAnnotation(SQLName.class);
			return anno != null ? anno.value() : null;
		}
	
		@Override
		public String getAlias(Method method)
		{
			SQLName anno = method.getAnnotation(SQLName.class);
			return anno != null ? anno.value() : null;
		}
	});
	
	/** Default converter for {@link #createForType(Object, Class)}. */
	private static final SQLTypeConverter DEFAULT_CONVERTER = new SQLTypeConverter(PROFILE_FACTORY);

	/**
	 * Performs a query on a connection and extracts the data into a single SQLRow.
	 * @param connection the connection to create a prepared statement and execute from.
	 * @param query the query statement to execute.
	 * @param parameters list of parameters for parameterized queries.
	 * @return the single result row returned, or null if no row returned.
	 * @throws SQLRuntimeException if the query cannot be executed or the query causes an error.
	 * @see #getResult(Connection, String, Object...)
	 */
	public static SQLRow getRow(Connection connection, String query, Object ... parameters)
	{
		return getResult(connection, query, parameters).getRow();
	}
	
	/**
	 * Performs a query on a connection and creates an object from it from the first row, setting relevant fields.
	 * <p>
	 * Each result row is applied via the target object's public fields and setter methods.
	 * <p>
	 * For instance, if there is a column is a row called "color", its value
	 * will be applied via the public field "color" or the setter "setColor()". Public
	 * fields take precedence over setters.
	 * <p>
	 * Only certain types are converted without issue. Below is a set of source types
	 * and their valid target types:
	 * <table>
	 * <caption>Conversion of Types</caption>
	 * <tr>
	 * 		<td><b>Boolean</b></td>
	 * 		<td>
	 * 			Boolean, all numeric primitives and their autoboxed equivalents, String. 
	 * 		</td>
	 * </tr>
	 * <tr>
	 * 		<td><b>Number</b></td>
	 * 		<td>
	 * 			Boolean (zero is false, nonzero is true), all numeric primitives and their autoboxed equivalents, String,
	 * 			Date, Timestamp. 
	 * 		</td>
	 * </tr>
	 * <tr>
	 * 		<td><b>Timestamp</b></td>
	 * 		<td>
	 * 			Long (both primitive and object as milliseconds since the Epoch), Timestamp, Date, String 
	 * 		</td>
	 * </tr>
	 * <tr>
	 * 		<td><b>Date</b></td>
	 * 		<td>
	 * 			Long (both primitive and object as milliseconds since the Epoch), Timestamp, Date, String 
	 * 		</td>
	 * </tr>
	 * <tr>
	 * 		<td><b>String</b></td>
	 * 		<td>
	 * 			Boolean, all numeric primitives and their autoboxed equivalents, 
	 * 			String, byte[], char[]. 
	 * 		</td>
	 * </tr>
	 * <tr>
	 * 		<td><b>Clob</b></td>
	 * 		<td>
	 * 			Boolean, all numeric primitives and their autoboxed equivalents, 
	 * 			String, byte[], char[]. 
	 * 		</td>
	 * </tr>
	 * <tr>
	 * 		<td><b>Blob</b></td>
	 * 		<td> 
	 * 			String, byte[], char[]. 
	 * 		</td>
	 * </tr>
	 * <tr>
	 * 		<td><b>NClob</b></td>
	 * 		<td>
	 * 			Boolean, all numeric primitives and their autoboxed equivalents, 
	 * 			String, byte[], char[]. 
	 * 		</td>
	 * </tr>
	 * <tr>
	 * 		<td><b>byte[]</b></td>
	 * 		<td>
	 *			String, byte[], char[]. 
	 * 		</td>
	 * </tr>
	 * <tr>
	 * 		<td><b>char[]</b></td>
	 * 		<td>
	 * 			Boolean, all numeric primitives and their autoboxed equivalents, 
	 * 			String, byte[], char[]. 
	 * 		</td>
	 * </tr>
	 * </table>
	 * @param <T> the returned data type.
	 * @param connection the connection to create a prepared statement and execute from.
	 * @param type the class type to instantiate.
	 * @param query the query statement to execute.
	 * @param parameters list of parameters for parameterized queries.
	 * @return an instantiated object with the pertinent fields set, or null if no rows.
	 * @throws SQLRuntimeException if the query cannot be executed or the query causes an error.
	 * @throws ClassCastException if one object type cannot be converted to another.
	 */
	public static <T> T getRow(Connection connection, Class<T> type, String query, Object ... parameters)
	{
		T out = null;
		try (PreparedStatement statement = connection.prepareStatement(query))
		{
			int i = 1;
			for (Object obj : parameters)
				statement.setObject(i++, obj);

			try (ResultSet resultSet = statement.executeQuery())
			{
				String[] columnNames = getAllColumnNamesFromResultSet(resultSet);
				if (resultSet.next())
					out = createObjectFromResultRow(type, resultSet, columnNames);
			}
		}
		catch (SQLException e)
		{
			throw new SQLRuntimeException(e);
		}
		
		return out;
	}
	
	/**
	 * Performs a query on a connection and extracts the data into a SQLResult.
	 * @param connection the connection to create a prepared statement and execute from.
	 * @param query the query statement to execute.
	 * @param parameters list of parameters for parameterized queries.
	 * @return the result of the query.
	 * @throws SQLRuntimeException if the query cannot be executed or the query causes an error.
	 */
	public static SQLResult getResult(Connection connection, String query, Object ... parameters)
	{
		try (PreparedStatement statement = connection.prepareStatement(query))
		{
			return callStatement(statement, false, parameters);
		}
		catch (SQLException e)
		{
			throw new SQLRuntimeException(e);
		}
	}
	
	/**
	 * Performs a query on a connection and creates objects from it, setting relevant fields.
	 * <p>
	 * Each result row is applied via the target object's public fields and setter methods.
	 * <p>
	 * For instance, if there is a column is a row called "color", its value
	 * will be applied via the public field "color" or the setter "setColor()". Public
	 * fields take precedence over setters.
	 * <p>
	 * Only certain types are converted without issue. Below is a set of source types
	 * and their valid target types:
	 * <table>
	 * <caption>Conversion of Types</caption>
	 * <tr>
	 * 		<td><b>Boolean</b></td>
	 * 		<td>
	 * 			Boolean, all numeric primitives and their autoboxed equivalents, String. 
	 * 		</td>
	 * </tr>
	 * <tr>
	 * 		<td><b>Number</b></td>
	 * 		<td>
	 * 			Boolean (zero is false, nonzero is true), all numeric primitives and their autoboxed equivalents, String,
	 * 			Date, Timestamp. 
	 * 		</td>
	 * </tr>
	 * <tr>
	 * 		<td><b>Timestamp</b></td>
	 * 		<td>
	 * 			Long (both primitive and object as milliseconds since the Epoch), Timestamp, Date, String 
	 * 		</td>
	 * </tr>
	 * <tr>
	 * 		<td><b>Date</b></td>
	 * 		<td>
	 * 			Long (both primitive and object as milliseconds since the Epoch), Timestamp, Date, String 
	 * 		</td>
	 * </tr>
	 * <tr>
	 * 		<td><b>String</b></td>
	 * 		<td>
	 * 			Boolean, all numeric primitives and their autoboxed equivalents, 
	 * 			String, byte[], char[]. 
	 * 		</td>
	 * </tr>
	 * <tr>
	 * 		<td><b>Clob</b></td>
	 * 		<td>
	 * 			Boolean, all numeric primitives and their autoboxed equivalents, 
	 * 			String, byte[], char[]. 
	 * 		</td>
	 * </tr>
	 * <tr>
	 * 		<td><b>Blob</b></td>
	 * 		<td> 
	 * 			String, byte[], char[]. 
	 * 		</td>
	 * </tr>
	 * <tr>
	 * 		<td><b>NClob</b></td>
	 * 		<td>
	 * 			Boolean, all numeric primitives and their autoboxed equivalents, 
	 * 			String, byte[], char[]. 
	 * 		</td>
	 * </tr>
	 * <tr>
	 * 		<td><b>byte[]</b></td>
	 * 		<td>
	 *			String, byte[], char[]. 
	 * 		</td>
	 * </tr>
	 * <tr>
	 * 		<td><b>char[]</b></td>
	 * 		<td>
	 * 			Boolean, all numeric primitives and their autoboxed equivalents, 
	 * 			String, byte[], char[]. 
	 * 		</td>
	 * </tr>
	 * </table>
	 * @param <T> the returned data type.
	 * @param connection the connection to create a prepared statement and execute from.
	 * @param type the class type to instantiate.
	 * @param query the query statement to execute.
	 * @param parameters list of parameters for parameterized queries.
	 * @return an array of instantiated objects with the pertinent fields set for each row.
	 * @throws SQLRuntimeException if the query cannot be executed or the query causes an error.
	 * @throws ClassCastException if one object type cannot be converted to another.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] getResult(Connection connection, Class<T> type, String query, Object ... parameters)
	{
		T[] out = null;
		List<T> rows = null;
		try (PreparedStatement statement = connection.prepareStatement(query))
		{
			int i = 1;
			for (Object obj : parameters)
				statement.setObject(i++, obj);

			try (ResultSet resultSet = statement.executeQuery())
			{
				String[] columnNames = getAllColumnNamesFromResultSet(resultSet);
				rows = new ArrayList<T>();
				while (resultSet.next())
					rows.add(createObjectFromResultRow(type, resultSet, columnNames));
			}
		}
		catch (SQLException e)
		{
			throw new SQLRuntimeException(e);
		}

		rows.toArray(out = (T[])Array.newInstance(type, rows.size()));
		return out;
	}

	/**
	 * Performs an update query (INSERT, DELETE, UPDATE, or other commands that do not return rows)
	 * on a connection and extracts the data/affected data/generated data into a SQLResult.
	 * @param connection the connection to create a prepared statement and execute from.
	 * @param query the query statement to execute.
	 * @param parameters list of parameters for parameterized queries.
	 * @return the update result returned (usually number of rows affected and or generated ids).
	 * @throws SQLRuntimeException if the query cannot be executed or the query causes an error.
	 */
	public static SQLResult getUpdateResult(Connection connection, String query, Object ... parameters)
	{
		try (PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS))
		{
			return callStatement(statement, true, parameters);
		}
		catch (SQLException e)
		{
			throw new SQLRuntimeException(e);
		}
	}

	/**
	 * Returns the names of the columns in a ResultSet in the order that they appear in the result.
	 * @param set the ResultSet to get the columns from.
	 * @return an array of all of the columns.
	 * @throws SQLException if something goes wrong.
	 */
	public static String[] getAllColumnNamesFromResultSet(ResultSet set) throws SQLException
	{
		ResultSetMetaData md = set.getMetaData();
		String[] out = new String[md.getColumnCount()];
		for (int i = 0; i < out.length; i++)
			out[i] = md.getColumnName(i+1);
		return out;
	}

	/**
	 * Performs a query on a connection and extracts the data into a SQLResult object.
	 * @param statement the statement to execute.
	 * @param update if true, this is an update query. If false, it is a result query.
	 * @param parameters the parameters to pass to the 
	 * @return the query result returned.
	 * @throws SQLException if a SQL exception occurs.
	 */
	public static SQLResult callStatement(PreparedStatement statement, boolean update, Object ... parameters) throws SQLException
	{
		SQLResult out = null;
	
		int i = 1;
		for (Object obj : parameters)
			statement.setObject(i++, obj);
		
		if (update)
		{
			int rows = statement.executeUpdate();
			try (ResultSet resultSet = statement.getGeneratedKeys())
			{
				out = createResult(resultSet, true, rows);
			}
		}
		else
		{
			try (ResultSet resultSet = statement.executeQuery())
			{
				out = createResult(resultSet, false, -1);
			}
		}
		return out;
	}

	/**
	 * Creates a new profile for a provided type.
	 * Generated profiles are stored in memory, and retrieved again by class type.
	 * <p>This method is thread-safe.
	 * @param <T> the class type.
	 * @param clazz the class.
	 * @return a new profile.
	 */
	private static <T> SQLTypeProfileFactory.Profile<T> getProfile(Class<T> clazz)
	{
		return PROFILE_FACTORY.getProfile(clazz);
	}

	/**
	 * Creates a new instance of an object for placement in a POJO or elsewhere.
	 * @param <T> the return object type.
	 * @param memberName the name of the member that is being converted (for reporting). 
	 * @param object the object to convert to another object
	 * @param targetType the target class type to convert to, if the types differ.
	 * @return a suitable object of type <code>targetType</code>. 
	 * @throws ClassCastException if the incoming type cannot be converted.
	 */
	private static <T> T createForType(String memberName, Object object, Class<T> targetType)
	{
		return DEFAULT_CONVERTER.createForType(memberName, object, targetType);
	}

	/**
	 * Creates a new object from a result row and sets the fields on it using row information.
	 * @param objectType the object type to instantiate.
	 * @param resultSet the result set.
	 * @param columnNames the names of the columns in the set.
	 * @return a new object with the relevant fields set.
	 * @throws SQLException if a SQL exception occurs.
	 * @throws ClassCastException if any incoming types cannot be converted.
	 */
	private static <T> T createObjectFromResultRow(Class<T> objectType, ResultSet resultSet, String[] columnNames) throws SQLException
	{
		T object = Utils.create(objectType);
		SQLTypeProfileFactory.Profile<T> profile = getProfile(objectType);
	
		for (int i = 0; i < columnNames.length; i++)
		{
			String column = columnNames[i];
	
			SQLTypeProfileFactory.Profile.FieldInfo fieldInfo = null; 
			SQLTypeProfileFactory.Profile.MethodInfo setterInfo = null;
			
			if ((fieldInfo = Utils.isNull(profile.getPublicFieldsByAlias().get(column), (profile.getPublicFieldsByName().get(column)))) != null)
			{
				Class<?> type = fieldInfo.getType();
				Utils.setFieldValue(object, fieldInfo.getField(), createForType(column, resultSet.getObject(i + 1), type));
			}
			else if ((setterInfo = Utils.isNull(profile.getSetterMethodsByAlias().get(column), (profile.getSetterMethodsByName().get(column)))) != null)
			{
				Class<?> type = setterInfo.getType();
				Utils.invokeBlind(setterInfo.getMethod(), object, createForType(column, resultSet.getObject(i + 1), type));
			}
		}
	
		return object;
	}

	/**
	 * Creates a {@link SQLResult} from a result set.
	 * The result set is assumed to be at the beginning of the set.
	 */
	private static SQLResult createResult(ResultSet resultSet, boolean update, int rows) throws SQLException
	{
		SQLResult out = null;
	
		if (update)
			out = new SQLResult(rows, resultSet);
		else
			out = new SQLResult(resultSet);
		
		return out;
	}

	/**
	 * Type converter class for converting types to others.
	 */
	private static class SQLTypeConverter
	{
		/** The profile factory to use for caching factory. */
		private SQLTypeProfileFactory profileFactory;
		
		/**
		 * Creates a type profiler.
		 * @param profileFactory the profile factory to use for caching reflection info.
		 */
		SQLTypeConverter(SQLTypeProfileFactory profileFactory)
		{
			this.profileFactory = profileFactory;
		}
		
		/**
		 * Reflect.creates a new instance of an object for placement in a POJO or elsewhere.
		 * @param <T> the return object type.
		 * @param object the object to convert to another object
		 * @param targetType the target class type to convert to, if the types differ.
		 * @return a suitable object of type <code>targetType</code>. 
		 * @throws ClassCastException if the incoming type cannot be converted.
		 */
		public <T> T createForType(Object object, Class<T> targetType)
		{
			return createForType("source", object, targetType);
		}
	
		/**
		 * Reflect.creates a new instance of an object for placement in a POJO or elsewhere.
		 * @param <T> the return object type.
		 * @param memberName the name of the member that is being converted (for reporting). 
		 * @param object the object to convert to another object
		 * @param targetType the target class type to convert to, if the types differ.
		 * @return a suitable object of type <code>targetType</code>. 
		 * @throws ClassCastException if the incoming type cannot be converted.
		 */
		@SuppressWarnings("unchecked")
		public <T> T createForType(String memberName, Object object, Class<T> targetType)
		{
			if (object == null)
			{
				if (targetType == Boolean.TYPE)
					return (T)Boolean.valueOf(false);
				else if (targetType == Byte.TYPE)
					return (T)Byte.valueOf((byte)0x00);
				else if (targetType == Short.TYPE)
					return (T)Short.valueOf((short)0);
				else if (targetType == Integer.TYPE)
					return (T)Integer.valueOf(0);
				else if (targetType == Float.TYPE)
					return (T)Float.valueOf(0f);
				else if (targetType == Long.TYPE)
					return (T)Long.valueOf(0L);
				else if (targetType == Double.TYPE)
					return (T)Double.valueOf(0.0);
				else if (targetType == Character.TYPE)
					return (T)Character.valueOf('\0');
				return null;
			}
			
			if (targetType.isAssignableFrom(object.getClass()))
				return targetType.cast(object);
			else if (Object.class == targetType)
				return targetType.cast(object);
			else if (Utils.isArray(object.getClass()))
				return convertArray(memberName, object, targetType);
			else if (object instanceof Map)
			{
				T out = Utils.create(targetType);
				for (Map.Entry<?, ?> pair : ((Map<?,?>)object).entrySet())
					applyMemberToObject(String.valueOf(pair.getKey()), pair.getValue(), out);
				return out;
			}
			else if (object instanceof Iterable)
				return convertIterable(memberName, (Iterable<?>)object, targetType);
			else if (object instanceof Enum<?>)
				return convertEnum(memberName, (Enum<?>)object, targetType);
			else if (object instanceof Boolean)
				return convertBoolean(memberName, (Boolean)object, targetType);
			else if (object instanceof Number)
				return convertNumber(memberName, (Number)object, targetType);
			else if (object instanceof Character)
				return convertCharacter(memberName, (Character)object, targetType);
			else if (object instanceof Date)
				return convertDate(memberName, (Date)object, targetType);
			else if (object instanceof String)
				return convertString(memberName, (String)object, targetType);
			else if (object instanceof Timestamp)
				return convertTimestamp(memberName, (Timestamp)object, targetType);
			else if (object instanceof Blob)
				return convertBlob(memberName, (Blob)object, targetType);
			else if (object instanceof Clob)
				return convertClob(memberName, (Clob)object, targetType);
			
			throw new ClassCastException("Object could not be converted: "+memberName+" is "+object.getClass()+", target is "+targetType);
		}
	
		/**
		 * Applies an object value to a target object via a "field" name (setter/field).
		 * @param <T> the target object type.
		 * @param name the field/setter name - this can be a member name or an alias.
		 * @param value the value to apply/convert.
		 * @param targetObject the target object to set stuff on.
		 */
		public <T> void applyMemberToObject(String name, Object value, T targetObject)
		{
			@SuppressWarnings("unchecked")
			SQLTypeProfileFactory.Profile<T> profile = profileFactory.getProfile((Class<T>)targetObject.getClass());
		
			SQLTypeProfileFactory.Profile.FieldInfo field = null; 
			SQLTypeProfileFactory.Profile.MethodInfo setter = null;
			if ((field = Utils.isNull(profile.getPublicFieldsByAlias().get(name), profile.getPublicFieldsByName().get(name))) != null)
			{
				Class<?> type = field.getType();
				Utils.setFieldValue(targetObject, field.getField(), createForType(name, value, type));
			}
			else if ((setter = Utils.isNull(profile.getSetterMethodsByAlias().get(name), profile.getSetterMethodsByName().get(name))) != null)
			{
				Class<?> type = setter.getType();
				Method method = setter.getMethod();
				Utils.invokeBlind(method, targetObject, createForType(name, value, type));
			}			
		}
	
		/**
		 * Converts a boolean to another type.
		 * @param <T> the target value type.
		 * @param memberName the name of the member being converted (for logging).
		 * @param b the value to convert.
		 * @param targetType the target type.
		 * @return the resultant type.
		 */
		@SuppressWarnings("unchecked")
		protected final <T> T convertBoolean(String memberName, Boolean b, Class<T> targetType)
		{
			if (targetType == Boolean.TYPE)
				return (T)Boolean.valueOf(b);
			else if (targetType == Boolean.class)
				return targetType.cast(b);
			else if (targetType == Byte.TYPE)
				return (T)Byte.valueOf((byte)(b ? 1 : 0));
			else if (targetType == Byte.class)
				return targetType.cast(b ? 1 : 0);
			else if (targetType == Short.TYPE)
				return (T)Short.valueOf((short)(b ? 1 : 0));
			else if (targetType == Short.class)
				return targetType.cast(b ? 1 : 0);
			else if (targetType == Integer.TYPE)
				return (T)Integer.valueOf(b ? 1 : 0);
			else if (targetType == Integer.class)
				return targetType.cast(b ? 1 : 0);
			else if (targetType == Float.TYPE)
				return (T)Float.valueOf(b ? 1f : 0f);
			else if (targetType == Float.class)
				return targetType.cast(b ? 1f : 0f);
			else if (targetType == Long.TYPE)
				return (T)Long.valueOf(b ? 1L : 0L);
			else if (targetType == Long.class)
				return targetType.cast(b ? 1L : 0L);
			else if (targetType == Double.TYPE)
				return (T)Double.valueOf(b ? 1.0 : 0.0);
			else if (targetType == Double.class)
				return targetType.cast(b ? 1.0 : 0.0);
			else if (targetType == Character.TYPE)
				return (T)Character.valueOf(b ? (char)1 : '\0');
			else if (targetType == Character.class)
				return targetType.cast(b ? (char)1 : '\0');
			else if (targetType == String.class)
				return targetType.cast(String.valueOf(b));
			else if (Utils.isArray(targetType))
			{
				Class<?> atype = Utils.getArrayType(targetType);
				Object out = Array.newInstance(atype, 1);
				Array.set(out, 0, createForType(b, atype));
				return targetType.cast(out);
			}
			
			throw new ClassCastException("Object could not be converted: "+memberName+" is Boolean, target is "+targetType);
		}
	
		/**
		 * Converts a numeric value to a target type.
		 * @param <T> the target value type.
		 * @param memberName the name of the member being converted (for logging).
		 * @param n the value to convert.
		 * @param targetType the target type.
		 * @return the resultant type.
		 */
		@SuppressWarnings("unchecked")
		protected final <T> T convertNumber(String memberName, Number n, Class<T> targetType)
		{
			if (targetType == Boolean.TYPE)
				return (T)Boolean.valueOf(n.intValue() != 0);
			else if (targetType == Boolean.class)
				return targetType.cast(n.intValue() != 0);
			else if (targetType == Byte.TYPE)
				return (T)Byte.valueOf(n.byteValue());
			else if (targetType == Byte.class)
				return targetType.cast(n.byteValue());
			else if (targetType == Short.TYPE)
				return (T)Short.valueOf(n.shortValue());
			else if (targetType == Short.class)
				return targetType.cast(n.shortValue());
			else if (targetType == Integer.TYPE)
				return (T)Integer.valueOf(n.intValue());
			else if (targetType == Integer.class)
				return targetType.cast(n.intValue());
			else if (targetType == Float.TYPE)
				return (T)Float.valueOf(n.floatValue());
			else if (targetType == Float.class)
				return targetType.cast(n.floatValue());
			else if (targetType == Long.TYPE)
				return (T)Long.valueOf(n.longValue());
			else if (targetType == Long.class)
				return targetType.cast(n.longValue());
			else if (targetType == Date.class)
				return targetType.cast(new Date(n.longValue()));
			else if (targetType == Double.TYPE)
				return (T)Double.valueOf(n.doubleValue());
			else if (targetType == Double.class)
				return targetType.cast(n.doubleValue());
			else if (targetType == Character.TYPE)
				return (T)Character.valueOf((char)(n.shortValue()));
			else if (targetType == Character.class)
				return targetType.cast((char)(n.shortValue()));
			else if (targetType == String.class)
				return targetType.cast(String.valueOf(n));
			else if (Utils.isArray(targetType))
			{
				Class<?> atype = Utils.getArrayType(targetType);
				Object out = Array.newInstance(atype, 1);
				Array.set(out, 0, createForType(n, atype));
				return targetType.cast(out);
			}
		
			throw new ClassCastException("Object could not be converted: "+memberName+" is numeric, target is "+targetType);
		}
	
		/**
		 * Converts a character value to a target type.
		 * @param <T> the target value type.
		 * @param memberName the name of the member being converted (for logging).
		 * @param c the value to convert.
		 * @param targetType the target type.
		 * @return the resultant type.
		 */
		@SuppressWarnings("unchecked")
		protected final <T> T convertCharacter(String memberName, Character c, Class<T> targetType)
		{
			char cv = c.charValue();
			
			if (targetType == Character.TYPE)
				return (T)Character.valueOf(cv);
			else if (targetType == Character.class)
				return targetType.cast(cv);
			else if (targetType == Boolean.TYPE)
				return (T)Boolean.valueOf(c != 0);
			else if (targetType == Boolean.class)
				return targetType.cast(c != 0);
			else if (targetType == Byte.TYPE)
				return (T)Byte.valueOf((byte)cv);
			else if (targetType == Byte.class)
				return targetType.cast((byte)cv);
			else if (targetType == Short.TYPE)
				return (T)Short.valueOf((short)cv);
			else if (targetType == Short.class)
				return targetType.cast((short)cv);
			else if (targetType == Integer.TYPE)
				return (T)Integer.valueOf((int)cv);
			else if (targetType == Integer.class)
				return targetType.cast((int)cv);
			else if (targetType == Float.TYPE)
				return (T)Float.valueOf((float)cv);
			else if (targetType == Float.class)
				return targetType.cast((float)cv);
			else if (targetType == Long.TYPE)
				return (T)Long.valueOf((long)cv);
			else if (targetType == Long.class)
				return targetType.cast((long)cv);
			else if (targetType == Double.TYPE)
				return (T)Double.valueOf((double)cv);
			else if (targetType == Double.class)
				return targetType.cast((double)cv);
			else if (targetType == String.class)
				return targetType.cast(String.valueOf(c));
			else if (Utils.isArray(targetType))
			{
				Class<?> atype = Utils.getArrayType(targetType);
				Object out = Array.newInstance(atype, 1);
				Array.set(out, 0, createForType(c, atype));
				return targetType.cast(out);
			}
		
			throw new ClassCastException("Object could not be converted: "+memberName+" is numeric, target is "+targetType);
		}
	
		/**
		 * Converts a date value to a target type.
		 * @param <T> the target value type.
		 * @param memberName the name of the member being converted (for logging).
		 * @param d the value to convert.
		 * @param targetType the target type.
		 * @return the resultant type.
		 */
		@SuppressWarnings("unchecked")
		protected final <T> T convertDate(String memberName, Date d, Class<T> targetType)
		{
			if (targetType == Long.TYPE)
				return (T)Long.valueOf(d.getTime());
			else if (targetType == Long.class)
				return targetType.cast(d.getTime());
			else if (targetType == String.class)
				return targetType.cast(String.valueOf(d));
			else if (targetType == Date.class)
				return targetType.cast(new Date(d.getTime()));
			else if (Utils.isArray(targetType))
			{
				Class<?> atype = Utils.getArrayType(targetType);
				Object out = Array.newInstance(atype, 1);
				Array.set(out, 0, createForType(d, atype));
				return targetType.cast(out);
			}
		
			throw new ClassCastException("Object could not be converted: "+memberName+" is Date, target is "+targetType);
		}
	
		/**
		 * Converts an enum value to a target type.
		 * @param <T> the target value type.
		 * @param memberName the name of the member being converted (for logging).
		 * @param e the value to convert.
		 * @param targetType the target type.
		 * @return the resultant type.
		 */
		@SuppressWarnings({ "unchecked", "rawtypes" })
		protected final <T> T convertEnum(String memberName, Enum<?> e, Class<T> targetType)
		{
			if (targetType == Byte.TYPE)
				return (T)Byte.valueOf((byte)e.ordinal());
			else if (targetType == Byte.class)
				return targetType.cast((byte)e.ordinal());
			else if (targetType == Short.TYPE)
				return (T)Short.valueOf((short)e.ordinal());
			else if (targetType == Short.class)
				return targetType.cast((short)e.ordinal());
			else if (targetType == Integer.TYPE)
				return (T)Integer.valueOf(e.ordinal());
			else if (targetType == Integer.class)
				return targetType.cast(e.ordinal());
			else if (targetType == Float.TYPE)
				return (T)Float.valueOf(e.ordinal());
			else if (targetType == Float.class)
				return targetType.cast(e.ordinal());
			else if (targetType == Long.TYPE)
				return (T)Long.valueOf(e.ordinal());
			else if (targetType == Long.class)
				return targetType.cast(e.ordinal());
			else if (targetType == Double.TYPE)
				return (T)Double.valueOf(e.ordinal());
			else if (targetType == Double.class)
				return targetType.cast(e.ordinal());
			else if (targetType == String.class)
				return targetType.cast(e.name());
			else if (targetType.isEnum())
				return targetType.cast(Utils.getEnumInstance(e.name(), (Class<Enum>)targetType));
			
			throw new ClassCastException("Object could not be converted: "+memberName+" is Enum, target is "+targetType);
		}
	
		/**
		 * Converts a string value to a target type.
		 * @param <T> the target value type.
		 * @param memberName the name of the member being converted (for logging).
		 * @param s the value to convert.
		 * @param targetType the target type.
		 * @return the resultant type.
		 */
		@SuppressWarnings({ "unchecked", "rawtypes" })
		protected final <T> T convertString(String memberName, String s, Class<T> targetType)
		{
			if (targetType == Boolean.TYPE)
				return (T)Boolean.valueOf(Utils.parseBoolean(s));
			else if (targetType == Boolean.class)
				return targetType.cast(Utils.parseBoolean(s));
			else if (targetType == Byte.TYPE)
				return (T)Byte.valueOf(Utils.parseByte(s));
			else if (targetType == Byte.class)
				return targetType.cast(Utils.parseByte(s));
			else if (targetType == Short.TYPE)
				return (T)Short.valueOf(Utils.parseShort(s));
			else if (targetType == Short.class)
				return targetType.cast(Utils.parseShort(s));
			else if (targetType == Integer.TYPE)
				return (T)Integer.valueOf(Utils.parseInt(s));
			else if (targetType == Integer.class)
				return targetType.cast(Utils.parseInt(s));
			else if (targetType == Float.TYPE)
				return (T)Float.valueOf(Utils.parseFloat(s));
			else if (targetType == Float.class)
				return targetType.cast(Utils.parseFloat(s));
			else if (targetType == Long.TYPE)
				return (T)Long.valueOf(Utils.parseLong(s));
			else if (targetType == Long.class)
				return targetType.cast(Utils.parseLong(s));
			else if (targetType == Double.TYPE)
				return (T)Double.valueOf(Utils.parseDouble(s));
			else if (targetType == Double.class)
				return targetType.cast(Utils.parseDouble(s));
			else if (targetType == Character.TYPE && s.length() == 1)
				return (T)Character.valueOf(s.charAt(0));
			else if (targetType == Character.class && s.length() == 1)
				return targetType.cast(s.charAt(0));
			else if (targetType == String.class)
				return targetType.cast(s);
			else if (targetType.isEnum())
				return targetType.cast(Utils.getEnumInstance(s, (Class<Enum>)targetType));
			else if (Utils.isArray(targetType))
			{
				if (Utils.getArrayType(targetType) == Character.TYPE)
					return targetType.cast(s.toCharArray());
				else if (Utils.getArrayType(targetType) == Byte.TYPE)
					return targetType.cast(s.getBytes());
				else if (Utils.isArray(targetType))
				{
					Class<?> atype = Utils.getArrayType(targetType);
					Object out = Array.newInstance(atype, 1);
					Array.set(out, 0, createForType(s, atype));
					return targetType.cast(out);
				}
			}
			
			throw new ClassCastException("Object could not be converted: "+memberName+" is String, target is "+targetType);
		}
	
		/**
		 * Converts an array value to a target type.
		 * @param <T> the target value type.
		 * @param memberName the name of the member being converted (for logging).
		 * @param array the value to convert.
		 * @param targetType the target type.
		 * @return the resultant type.
		 */
		protected final <T> T convertArray(String memberName, Object array, Class<T> targetType)
		{
			Class<?> arrayType = Utils.getArrayType(array);
			int arrayDimensions = Utils.getArrayDimensions(array);
			
			if (arrayDimensions == 1)
			{
				if (arrayType == Character.TYPE)
				{
					return convertCharArray(memberName, (char[])array, targetType);
				}
				else if (arrayType == Character.class)
				{
					Character[] chars = (Character[])array;
					char[] charArray = new char[chars.length];
					for (int i = 0; i < charArray.length; i++)
						charArray[i] = chars[i];
					return convertCharArray(memberName, charArray, targetType);
				}
				else if (arrayType == Byte.TYPE)
				{
					return convertByteArray(memberName, (byte[])array, targetType);
				}
				else if (arrayType == Byte.class)
				{
					Byte[] bytes = (Byte[])array;
					byte[] byteArray = new byte[bytes.length];
					for (int i = 0; i < byteArray.length; i++)
						byteArray[i] = bytes[i];
					return convertByteArray(memberName, byteArray, targetType);
				}
				else
					return convertOtherArray(memberName, array, targetType);
			}
			else
				return convertOtherArray(memberName, array, targetType);
		}
	
		/**
		 * Converts a char array value to a target type.
		 * @param <T> the target value type.
		 * @param memberName the name of the member being converted (for logging).
		 * @param charArray the value to convert.
		 * @param targetType the target type.
		 * @return the resultant type.
		 */
		protected final <T> T convertCharArray(String memberName, char[] charArray, Class<T> targetType)
		{
			if (Utils.isArray(targetType))
			{
				if (Utils.getArrayType(targetType) == Character.TYPE)
					return targetType.cast(charArray);
				else if (Utils.getArrayType(targetType) == Byte.TYPE)
					return targetType.cast((new String(charArray)).getBytes());
				else
					return convertOtherArray(memberName, charArray, targetType);
			}
			else if (targetType == String.class)
				return targetType.cast(new String(charArray));
			else
				return convertString(memberName, new String(charArray), targetType);
		}
	
		/**
		 * Converts a byte array value to a target type.
		 * @param <T> the target value type.
		 * @param memberName the name of the member being converted (for logging).
		 * @param byteArray the value to convert.
		 * @param targetType the target type.
		 * @return the resultant type.
		 */
		protected final <T> T convertByteArray(String memberName, byte[] byteArray, Class<T> targetType)
		{
			if (Utils.isArray(targetType))
			{
				if (Utils.getArrayType(targetType) == Character.TYPE)
					return targetType.cast((new String(byteArray)).toCharArray());
				else if (Utils.getArrayType(targetType) == Byte.TYPE)
					return targetType.cast(Arrays.copyOf(byteArray, byteArray.length));
				else
					return convertOtherArray(memberName, byteArray, targetType);
			}
			else if (targetType == String.class)
				return targetType.cast(new String(byteArray));
			else
				return convertOtherArray(memberName, byteArray, targetType);
		}
	
		/**
		 * Converts a totally different array type.
		 * @param <T> the target value type.
		 * @param memberName the name of the member being converted (for logging).
		 * @param array the value to convert.
		 * @param targetType the target type.
		 * @return the resultant type.
		 */
		protected final <T> T convertOtherArray(String memberName, Object array, Class<T> targetType)
		{
			Class<?> atype = Utils.getArrayType(targetType);
			if (atype == null)
				throw new ClassCastException("Array cannot be converted; "+memberName+" is array and target is not array typed.");
			
			int alen = Array.getLength(array);
			Object newarray = Array.newInstance(atype, alen);
			for (int i = 0; i < alen; i++)
				Array.set(newarray, i, createForType(String.format("%s[%d]", memberName, i), Array.get(array, i), atype));
				
			return targetType.cast(newarray);
		}
	
		/**
		 * Converts an iterable to another type (like an array).
		 * @param <T> the target value type.
		 * @param memberName the name of the member being converted (for logging).
		 * @param iter the value to convert.
		 * @param targetType the target type.
		 * @return the resultant type.
		 */
		protected final <T> T convertIterable(String memberName, Iterable<?> iter, Class<T> targetType)
		{
			if (Utils.isArray(targetType) && Utils.getArrayDimensions(targetType) == 1)
			{
				List<Object> templist = new ArrayList<Object>(64);
				for (Object obj : iter)
					templist.add(obj);
				
				Class<?> atype = Utils.getArrayType(targetType);
				int alen = templist.size();
				Object newarray = Array.newInstance(atype, alen);
				for (int i = 0; i < alen; i++)
					Array.set(newarray, i, createForType(String.format("%s, index %d", memberName, i), templist.get(i), atype));
				
				return targetType.cast(newarray);
			}
			else
				throw new ClassCastException("Object could not be converted: "+memberName+" is Iterable, target is "+targetType);
		}
	
		/**
		 * Converts a timestamp value to a target type.
		 * @param memberName the name of the member being converted (for logging).
		 * @param t the value to convert.
		 * @param targetType the target type.
		 * @param <T> the target type.
		 * @return the resultant type.
		 */
		@SuppressWarnings("unchecked")
		protected final <T> T convertTimestamp(String memberName, Timestamp t, Class<T> targetType)
		{
			if (targetType == Long.TYPE)
				return t != null ? (T)Long.valueOf(t.getTime()) : (T)Long.valueOf(0);
			else if (targetType == Long.class)
				return targetType.cast(t.getTime());
			else if (targetType == String.class)
				return targetType.cast(String.valueOf(t));
			else if (targetType == Date.class)
				return targetType.cast(new Date(t.getTime()));
			else if (targetType == Timestamp.class)
				return targetType.cast(t);
			else if (Utils.isArray(targetType))
			{
				Class<?> atype = Utils.getArrayType(targetType);
				Object out = Array.newInstance(atype, 1);
				Array.set(out, 0, createForType(t, atype));
				return targetType.cast(out);
			}
		
			throw new ClassCastException("Object could not be converted: "+memberName+" is Timestamp, target is "+targetType);
		}
	
		/**
		 * Converts an SQL Clob value to a target type.
		 * @param memberName the name of the member being converted (for logging).
		 * @param clob the value to convert.
		 * @param targetType the target type.
		 * @param <T> the target type.
		 * @return the resultant type.
		 */
		protected final <T> T convertClob(String memberName, Clob clob, Class<T> targetType)
		{
			StringWriter sw = null;
			try (Reader reader = clob.getCharacterStream()) 
			{
				sw = new StringWriter();
				char[] charBuffer = new char[1024 * 8];
				int cbuf = 0;
				while ((cbuf = reader.read(charBuffer)) > 0)
					sw.write(charBuffer, 0, cbuf);
			} 
			catch (SQLException | IOException e) 
			{
				return null;
			}
		
			char[] out = new char[sw.getBuffer().length()];
			sw.getBuffer().getChars(0, out.length, out, 0);
			return convertArray(memberName, out, targetType);
		}
	
		/**
		 * Converts an SQL Blob value to a target type.
		 * @param memberName the name of the member being converted (for logging).
		 * @param blob the value to convert.
		 * @param targetType the target type.
		 * @param <T> the target type.
		 * @return the resultant type.
		 */
		protected final <T> T convertBlob(String memberName, Blob blob, Class<T> targetType)
		{
			ByteArrayOutputStream bos = null;
			try (InputStream in = blob.getBinaryStream()) 
			{
				bos = new ByteArrayOutputStream();
				byte[] buffer = new byte[65536];
				int buf = 0;
				while ((buf = in.read(buffer)) > 0)
					bos.write(buffer, 0, buf);
			} 
			catch (SQLException | IOException e) 
			{
				return null;
			}
			
			return convertArray(memberName, bos.toByteArray(), targetType);
		}
	
	}

	/**
	 * A factory that produces type profiles for POJOs and data objects.
	 */
	private static class SQLTypeProfileFactory
	{
		/** The policy used by this factory. */
		private MemberPolicy policy;
		/** Generated profiles. */
		private HashMap<Class<?>, Profile<?>> generatedProfiles;
		
		/**
		 * Creates a new TypeProfileFactory.
		 * This creates type profiles using an additional policy that changes method/field handling
		 * for each type.
		 * @param policy the member policy.
		 */
		SQLTypeProfileFactory(MemberPolicy policy)
		{
			this.policy = policy;
			this.generatedProfiles = new HashMap<>(8);
		}
	
		/**
		 * Creates a new profile for a provided type.
		 * Generated profiles are stored in memory, and retrieved again by class type.
		 * <p>This method is thread-safe.
		 * @param <T> the class type.
		 * @param clazz the class.
		 * @return a new profile.
		 */
		@SuppressWarnings("unchecked")
		public <T> Profile<T> getProfile(Class<T> clazz)
		{
			Profile<T> out = null;
			if ((out = (Profile<T>)generatedProfiles.get(clazz)) == null)
			{
				synchronized (generatedProfiles)
				{
					// early out.
					if ((out = (Profile<T>)generatedProfiles.get(clazz)) == null)
					{
						out = new Profile<>(clazz, policy);
						generatedProfiles.put(clazz, out);
					}
				}
			}
			else
				out = (Profile<T>)generatedProfiles.get(clazz);
			
			return out;
		}
		
		/**
		 * Returns the field name for a getter/setter method.
		 * If the method name is not a getter or setter name, then this will return <code>methodName</code>
		 * <p>
		 * For example, the field name "setColor" will return "color" and "isHidden" returns "hidden". 
		 * (note the change in camel case).
		 * @param methodName the name of the method.
		 * @return the modified method name.
		 */
		private static String getFieldName(String methodName)
		{
			if (isGetterName(methodName))
			{
				if (methodName.startsWith("is"))
					return truncateMethodName(methodName, true);
				else if (methodName.startsWith("get"))
					return truncateMethodName(methodName, false);
			}
			else if (isSetterName(methodName))
				return truncateMethodName(methodName, false);
			
			return methodName;
		}
	
		/**
		 * Checks if a method name describes a "setter" method. 
		 * @param methodName the name of the method.
		 * @return true if so, false if not.
		 */
		private static boolean isSetterName(String methodName)
		{
			if (methodName.startsWith("set"))
			{
				if (methodName.length() < 4)
					return false;
				else
					return Character.isUpperCase(methodName.charAt(3));
			}
			return false;
		}
	
		/**
		 * Checks if a method name describes a "getter" method (also detects "is" methods). 
		 * @param methodName the name of the method.
		 * @return true if so, false if not.
		 */
		private static boolean isGetterName(String methodName)
		{
			if (methodName.startsWith("is"))
			{
				if (methodName.length() < 3)
					return false;
				else
					return Character.isUpperCase(methodName.charAt(2));
			}
			else if (methodName.startsWith("get"))
			{
				if (methodName.length() < 4)
					return false;
				else
					return Character.isUpperCase(methodName.charAt(3));
			}
			return false;
		}
	
		/**
		 * Checks if a method is a "setter" method.
		 * This checks its name, if it returns a void value or the source class type (chain setters), takes one argument, and if it is <b>public</b>.
		 * @param method the method to inspect.
		 * @return true if so, false if not.
		 */
		private static boolean isSetter(Method method, Class<?> sourceType)
		{
			Class<?> rettype = method.getReturnType();
			return isSetterName(method.getName()) 
				&& method.getParameterTypes().length == 1
				&& (rettype == Void.TYPE || rettype == Void.class || rettype == sourceType) 
				&& (method.getModifiers() & Modifier.PUBLIC) != 0;
		}
	
		/**
		 * Checks if a method is a "getter" method.
		 * This checks its name, if it returns a non-void value, takes no arguments, and if it is <b>public</b>.
		 * @param method the method to inspect.
		 * @return true if so, false if not.
		 */
		private static boolean isGetter(Method method)
		{
			return isGetterName(method.getName()) 
				&& method.getParameterTypes().length == 0
				&& !(method.getReturnType() == Void.TYPE || method.getReturnType() == Void.class) 
				&& (method.getModifiers() & Modifier.PUBLIC) != 0;
		}
	
		// truncator method
		private static String truncateMethodName(String methodName, boolean is)
		{
			return is 
				? Character.toLowerCase(methodName.charAt(2)) + methodName.substring(3)
				: Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
		}
	
		/**
		 * An interface for figuring out whether to ignore certain fields or methods
		 * on an object that a profile is being generated for (if they are already 
		 * going to pass testing for include-able fields).
		 */
		interface MemberPolicy
		{
			/**
			 * Checks if a field is ignored by the profile generator.
			 * @param field the field to test.
			 * @return true if so, false if not.
			 */
			boolean isIgnored(Field field);
		
			/**
			 * Checks if a method is ignored by the profile generator.
			 * @param method the method to test.
			 * @return true if so, false if not.
			 */
			boolean isIgnored(Method method);
		
			/**
			 * Gets an alias name for the provided field.
			 * This is used for mapping this field to another name, usually for type/object conversions or the like.
			 * @param field the field to use.
			 * @return the name to use, or null for no alias.
			 */
			String getAlias(Field field);
		
			/**
			 * Gets an alias name for the provided method.
			 * This is used for mapping this method to another name, usually for type/object conversions or the like.
			 * @param method the method to use.
			 * @return the name to use, or null for no alias.
			 */
			String getAlias(Method method);
		}
	
		/**
		 * Type profile for an unknown object that has an ambiguous signature for 
		 * applying values to POJOs and beans.
		 * This only cares about setter methods with one argument and public fields.
		 * @param <T> the type that this for.
		 */
		static class Profile<T>
		{
			/** Map of Public fields by name. */
			private Map<String, FieldInfo> publicFieldsByName;
			/** Map of getters by name. */
			private Map<String, MethodInfo> getterMethodsByName;
			/** Map of setters by name. */
			private Map<String, MethodInfo> setterMethodsByName;
	
			/** Map of Public fields by alias. */
			private Map<String, FieldInfo> publicFieldsByAlias;
			/** Map of getters by alias. */
			private Map<String, MethodInfo> getterMethodsByAlias;
			/** Map of setters by alias. */
			private Map<String, MethodInfo> setterMethodsByAlias;
			
			// Creates a profile from a class. 
			private Profile(Class<? extends T> inputClass, MemberPolicy policy)
			{
				publicFieldsByName = new HashMap<String, FieldInfo>(4);
				getterMethodsByName = new HashMap<String, MethodInfo>(4);
				setterMethodsByName = new HashMap<String, MethodInfo>(4);
				publicFieldsByAlias = new HashMap<String, FieldInfo>(4);
				getterMethodsByAlias = new HashMap<String, MethodInfo>(4);
				setterMethodsByAlias = new HashMap<String, MethodInfo>(4);
	
				for (Field f : inputClass.getFields())
				{
					if (Modifier.isStatic(f.getModifiers()))
						continue;
						
					if (!policy.isIgnored(f))
					{
						String alias = policy.getAlias(f);
						FieldInfo fi = new FieldInfo(f.getType(), f, alias);
						publicFieldsByName.put(f.getName(), fi);
						if (alias != null)
							publicFieldsByAlias.put(alias, fi);
					}
				}
				
				for (Method m : inputClass.getMethods())
				{
					if (!policy.isIgnored(m) && isGetter(m) && !m.getName().equals("getClass"))
					{
						String alias = policy.getAlias(m);
						MethodInfo mi = new MethodInfo(m.getReturnType(), m, alias);
						getterMethodsByName.put(getFieldName(m.getName()), mi);
						if (alias != null)
							getterMethodsByAlias.put(alias, mi);
					}
					else if (!policy.isIgnored(m) && isSetter(m, inputClass))
					{
						String alias = policy.getAlias(m);
						MethodInfo mi = new MethodInfo(m.getParameterTypes()[0], m, alias);
						setterMethodsByName.put(getFieldName(m.getName()), mi);
						if (alias != null)
							setterMethodsByAlias.put(alias, mi);
					}
				}
			}
			
			/** 
			 * Returns a reference to the map that contains this profile's public fields.
			 * Maps "field name" to {@link FieldInfo} object.
			 * @return the map of field name to field.  
			 */
			public Map<String, FieldInfo> getPublicFieldsByName()
			{
				return publicFieldsByName;
			}
	
			/** 
			 * Returns a reference to the map that contains this profile's getter methods.
			 * Maps "field name" to {@link MethodInfo} object, which contains the {@link Class} type and the {@link Method} itself.
			 * @return the map of getter name to method.  
			 */
			@SuppressWarnings("unused")
			public Map<String, MethodInfo> getGetterMethodsByName()
			{
				return getterMethodsByName;
			}
	
			/** 
			 * Returns a reference to the map that contains this profile's setter methods.
			 * Maps "field name" to {@link MethodInfo} object, which contains the {@link Class} type and the {@link Method} itself. 
			 * @return the map of setter name to method.  
			 */
			public Map<String, MethodInfo> getSetterMethodsByName()
			{
				return setterMethodsByName;
			}
	
			/** 
			 * Returns a reference to the map that contains this profile's public fields.
			 * Maps "field name" to {@link FieldInfo} object.
			 * @return the map of field name to field.  
			 */
			public Map<String, FieldInfo> getPublicFieldsByAlias()
			{
				return publicFieldsByAlias;
			}
	
			/** 
			 * Returns a reference to the map that contains this profile's getter methods.
			 * Maps "field name" to {@link MethodInfo} object, which contains the {@link Class} type and the {@link Method} itself.
			 * @return the map of getter name to method.  
			 */
			@SuppressWarnings("unused")
			public Map<String, MethodInfo> getGetterMethodsByAlias()
			{
				return getterMethodsByAlias;
			}
	
			/** 
			 * Returns a reference to the map that contains this profile's setter methods.
			 * Maps "field name" to {@link MethodInfo} object, which contains the {@link Class} type and the {@link Method} itself. 
			 * @return the map of setter name to method.  
			 */
			public Map<String, MethodInfo> getSetterMethodsByAlias()
			{
				return setterMethodsByAlias;
			}
	
			/**
			 * Field information.
			 * Contains the relevant type and getter/setter method.
			 */
			public static class FieldInfo
			{
				/** Object Type. */
				private Class<?> type;
				/** Field reference. */
				private Field field;
				/** Alias, if any. */
				private String alias;
	
				private FieldInfo(Class<?> type, Field field, String alias)
				{
					this.type = type;
					this.field = field;
					this.alias = alias;
				}
	
				/**
				 * @return the type that this setter takes as an argument, or this getter returns.
				 */
				public Class<?> getType()
				{
					return type;
				}
	
				/**
				 * @return the setter method itself.
				 */
				public Field getField()
				{
					return field;
				}
				
				/**
				 * @return the alias for the field info, if any.
				 */
				@SuppressWarnings("unused")
				public String getAlias()
				{
					return alias;
				}
				
			}
			
			/**
			 * Method signature.
			 * Contains the relevant type and getter/setter method.
			 */
			public static class MethodInfo
			{
				/** Object Type. */
				private Class<?> type;
				/** Method reference. */
				private Method method;
				/** Alias, if any. */
				private String alias;
	
				private MethodInfo(Class<?> type, Method method, String alias)
				{
					this.type = type;
					this.method = method;
					this.alias = alias;
				}
	
				/**
				 * @return the type that this setter takes as an argument, or this getter returns.
				 */
				public Class<?> getType()
				{
					return type;
				}
	
				/**
				 * @return the setter method itself.
				 */
				public Method getMethod()
				{
					return method;
				}
				
				/**
				 * @return the alias for the field info, if any.
				 */
				@SuppressWarnings("unused")
				public String getAlias()
				{
					return alias;
				}
				
			}
			
		}
	
	}

}
