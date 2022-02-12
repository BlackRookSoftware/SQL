package com.blackrook.sql;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.blackrook.sql.SQLConnection.TransactionLevel;
import com.blackrook.sql.util.SQLConnectionFunction;
import com.blackrook.sql.util.SQLRuntimeException;
import com.blackrook.sql.util.SQLTransactionFunction;

/**
 * Abstract DAO class.
 * Contains convenience methods for database pool access.
 * @author Matthew Tropiano
 * @since 1.3.0
 */
public abstract class SQLAbstractDAO
{
	/** Database Connection Pool. */
	private SQLPool pool;
	/** Database Connection Acquisition Timeout. */
	private long acquireTimeout;
	
	/**
	 * Creates this DAO attached to the provided pool, no connection acquisition timeout.
	 * @param pool the pool to acquire connections from.
	 */
	protected SQLAbstractDAO(SQLPool pool)
	{
		this(pool, 0L);
	}

	/**
	 * Creates this DAO attached to the provided pool, no connection acquisition timeout.
	 * @param pool the pool to acquire connections from.
	 * @param acquireTimeout the connection acquisition timeout in milliseconds.
	 */
	protected SQLAbstractDAO(SQLPool pool, long acquireTimeout)
	{
		this.pool = pool;
		this.acquireTimeout = acquireTimeout;
	}

	/**
	 * Sets this DAO's connection acquisition timeout.
	 * @param acquireTimeout the new timeout in milliseconds.
	 * @see SQLPool#getAvailableConnection(long)
	 */
	protected void setAcquireTimeout(long acquireTimeout)
	{
		this.acquireTimeout = acquireTimeout;
	}
	
	/**
	 * Creates a query builder that is pre-populated with a query fragment.
	 * @param queryFragment the query resource name.
	 * @return a new query builder.
	 */
	protected QueryStringBuilder buildQuery(String queryFragment)
	{
		return new QueryStringBuilder(queryFragment);
	}

	/**
	 * Gets a connection and performs a function on it, returning the result.
	 * @param <R> the return type.
	 * @param handler the connection handler function.
	 * @return the return object.
	 * @throws DataAccessTimeoutException if a connection timeout occurs.
	 * @throws DataAccessFailureException if any other exception occurs.
	 */
	protected <R> R call(SQLConnectionFunction<R> handler)
	{
		try {
			return pool.getConnectionAnd(acquireTimeout, handler);
		} catch (TimeoutException e) {
			throw new DataAccessTimeoutException("Fetching an available connection timed out!", e);
		} catch (InterruptedException e) {
			throw new DataAccessFailureException("Fetching an available connection was interrupted!", e);
		} catch (SQLRuntimeException e) {
			throw new DataAccessFailureException("A SQL exception occurred!", e);
		} catch (SQLException e) {
			throw new DataAccessFailureException("A SQL exception occurred!", e);
		} 
	}

	/**
	 * Gets a connection and performs a function on it, returning the result.
	 * @param <R> the return type.
	 * @param level the transaction level.
	 * @param handler the connection handler function.
	 * @return the return object.
	 * @throws DataAccessTimeoutException if a connection timeout occurs.
	 * @throws DataAccessFailureException if any other exception occurs.
	 */
	protected <R> R transaction(TransactionLevel level, SQLTransactionFunction<R> handler)
	{
		return call((conn) -> conn.startTransactionAnd(level, handler));
	}

	/**
	 * Gets a connection and performs a function on it, returning the altered row count.
	 * @param handler the connection handler function.
	 * @return the row count.
	 * @throws DataAccessTimeoutException if a connection timeout occurs.
	 * @throws DataAccessFailureException if any other exception occurs.
	 */
	protected int rowCount(SQLConnectionFunction<SQLResult> handler)
	{
		return call(handler).getRowCount();
	}

	/**
	 * Gets a connection and performs a function on it, and if the affected row count is 1, return true.
	 * @param handler the connection handler function.
	 * @return true if the row count is 1.
	 * @throws DataAccessTimeoutException if a connection timeout occurs.
	 * @throws DataAccessFailureException if any other exception occurs.
	 */
	protected boolean updated(SQLConnectionFunction<SQLResult> handler)
	{
		return rowCount(handler) == 1;
	}

	/**
	 * Gets a connection and performs a function on it, and if the affected row count is 1, return the id.
	 * @param handler the connection handler function.
	 * @return the returned id, or null if row count isn't 1.
	 * @throws DataAccessTimeoutException if a connection timeout occurs.
	 * @throws DataAccessFailureException if any other exception occurs.
	 */
	protected Object id(SQLConnectionFunction<SQLResult> handler)
	{
		SQLResult result = call(handler);
		return result.getRowCount() == 1 ? result.getId() : null;
	}

	/**
	 * Gets a connection and performs a function on it, and returns the generated ids.
	 * @param handler the connection handler function.
	 * @return the array of returned id objects.
	 * @throws DataAccessTimeoutException if a connection timeout occurs.
	 * @throws DataAccessFailureException if any other exception occurs.
	 */
	protected Object[] ids(SQLConnectionFunction<SQLResult> handler)
	{
		return call(handler).getIds();
	}

	/**
	 * Lifts a single value from a queried {@link SQLRow} and returns it, returning null if the row is null.
	 * If the row is null, the extractor is not called.
	 * @param <R> the return type.
	 * @param handler the handler function for returning a row from a query.
	 * @param extractor the value returner function (called if row is not null).
	 * @return the value returned, or <code>null</code> if the row is null.
	 * @throws DataAccessTimeoutException if a connection timeout occurs.
	 * @throws DataAccessFailureException if any other exception occurs.
	 */
	protected <R> R value(SQLConnectionFunction<SQLRow> handler, Function<SQLRow, R> extractor)
	{
		SQLRow row;
		if ((row = call(handler)) != null)
			return extractor.apply(row);
		return null;
	}
	
	/**
	 * Lifts a list of single values from a queried {@link SQLResult} and returns it as an immutable list.
	 * If the result has zero rows, the extractor is never called.
	 * @param <R> the return type.
	 * @param handler the handler function for returning a row from a query.
	 * @param extractor the function to call per row for conversion.
	 * @return the List generated.
	 * @throws DataAccessTimeoutException if a connection timeout occurs.
	 * @throws DataAccessFailureException if any other exception occurs.
	 */
	protected <R> List<R> valueList(SQLConnectionFunction<SQLResult> handler, Function<SQLRow, R> extractor)
	{
		SQLResult result = call(handler);
		List<R> out = new ArrayList<>(result.getRowCount());
		for (SQLRow row : result)
			out.add(extractor.apply(row));
		return Collections.unmodifiableList(out);
	}
	
	/**
	 * Lifts a list of single values from a queried {@link SQLResult} and returns it as an immutable set of unique values.
	 * If the result has zero rows, the extractor is never called.
	 * @param <R> the return type.
	 * @param handler the handler function for returning a row from a query.
	 * @param extractor the value returner function (called if row is not null).
	 * @return the Set generated.
	 * @throws DataAccessTimeoutException if a connection timeout occurs.
	 * @throws DataAccessFailureException if any other exception occurs.
	 */
	protected <R> Set<R> valueSet(SQLConnectionFunction<SQLResult> handler, Function<SQLRow, R> extractor)
	{
		SQLResult result = call(handler);
		Set<R> out = new HashSet<>(Math.max(result.getRowCount(), 1), 1f);
		for (SQLRow row : result)
			out.add(extractor.apply(row));
		return Collections.unmodifiableSet(out);
	}
	
	/**
	 * Lifts a list of single values from a queried {@link SQLResult} and returns it as an immutable sorted set of unique values.
	 * If the result has zero rows, the extractor is never called.
	 * @param <R> the return type.
	 * @param handler the handler function for returning a row from a query.
	 * @param extractor the value returner function (called if row is not null).
	 * @return the Set generated.
	 * @throws DataAccessTimeoutException if a connection timeout occurs.
	 * @throws DataAccessFailureException if any other exception occurs.
	 */
	protected <R> SortedSet<R> valueSortedSet(SQLConnectionFunction<SQLResult> handler, Function<SQLRow, R> extractor)
	{
		SQLResult result = call(handler);
		SortedSet<R> out = new TreeSet<>();
		for (SQLRow row : result)
			out.add(extractor.apply(row));
		return Collections.unmodifiableSortedSet(out);
	}

	/**
	 * Lifts a map of values from a queried {@link SQLResult} and returns it as an immutable map of key to value.
	 * If the result has zero rows, the extractor is never called.
	 * @param <K> the key type.
	 * @param <V> the value type.
	 * @param handler the handler function for returning a row from a query.
	 * @param extractor the function for adding the map entries from a row (second parameter is the Map to add to).
	 * @return the Map generated.
	 * @throws DataAccessTimeoutException if a connection timeout occurs.
	 * @throws DataAccessFailureException if any other exception occurs.
	 */
	protected <K, V> Map<K, V> valueMap(SQLConnectionFunction<SQLResult> handler, BiConsumer<SQLRow, Map<K, V>> extractor)
	{
		SQLResult result = call(handler);
		Map<K, V> out = new HashMap<>(Math.max(result.getRowCount(), 1), 1f);
		for (SQLRow row : result)
			extractor.accept(row, out);
		return Collections.unmodifiableMap(out);
	}
	
	/**
	 * Lifts a sorted map of values from a queried {@link SQLResult} and returns it as an immutable map of key to value.
	 * If the result has zero rows, the extractor is never called.
	 * @param <V> the value type.
	 * @param handler the handler function for returning a row from a query.
	 * @param extractor the function for adding the map entries from a row (second parameter is the SortedMap to add to).
	 * @return the Map generated.
	 * @throws DataAccessTimeoutException if a connection timeout occurs.
	 * @throws DataAccessFailureException if any other exception occurs.
	 */
	protected <K, V> SortedMap<K, V> valueSortedMap(SQLConnectionFunction<SQLResult> handler, BiConsumer<SQLRow, SortedMap<K, V>> extractor)
	{
		SQLResult result = call(handler);
		SortedMap<K, V> out = new TreeMap<>();
		for (SQLRow row : result)
			extractor.accept(row, out);
		return Collections.unmodifiableSortedMap(out);
	}
	
	/**
	 * Lifts a map of values from a queried {@link SQLResult} and returns it as an immutable map of case-insensitive string key to value.
	 * If the result has zero rows, the extractor is never called.
	 * @param <V> the value type.
	 * @param handler the handler function for returning a row from a query.
	 * @param extractor the function for adding the map entries from a row (second parameter is the SortedMap to add to).
	 * @return the Map generated.
	 * @throws DataAccessTimeoutException if a connection timeout occurs.
	 * @throws DataAccessFailureException if any other exception occurs.
	 */
	protected <V> SortedMap<String, V> valueCaseInsenstiveMap(SQLConnectionFunction<SQLResult> handler, BiConsumer<SQLRow, SortedMap<String, V>> extractor)
	{
		SQLResult result = call(handler);
		SortedMap<String, V> out = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		for (SQLRow row : result)
			extractor.accept(row, out);
		return Collections.unmodifiableSortedMap(out);
	}
	
	/**
	 * Creates a null column criterion.
	 * @param columnName the column name.
	 * @return the created criterion.
	 */
	protected static Criterion columnIsNull(String columnName)
	{
		return new Criterion(columnName);
	}
	
	/**
	 * Creates a column "in" criterion.
	 * @param columnName the column name.
	 * @param values the values.
	 * @return the created criterion.
	 */
	protected static Criterion column(String columnName, Object ... values)
	{
		return new Criterion(columnName, values);
	}
	
	/**
	 * Creates a column criterion.
	 * @param columnName the column name.
	 * @param operator the operator.
	 * @param value the value.
	 * @return the created criterion.
	 */
	protected static Criterion column(String columnName, Operator operator, Object value)
	{
		return new Criterion(columnName, operator, value);
	}
	
	/**
	 * Extracts the values from a set of criteria (to be used as parameters).
	 * @param criteria the list of criteria.
	 * @return the array of parameters.
	 */
	protected static Object[] parameters(Criterion ... criteria)
	{
		List<Object> out = new LinkedList<Object>();
		for (int i = 0; i < criteria.length; i++)
		{
			Object criteriaValue = criteria[i].value;
			if (criteriaValue != null)
			{
				if (criteriaValue.getClass().isArray())
				{
					Object[] values = (Object[])criteriaValue;
					for (int x = 0; x < values.length; x++)
						out.add(values[x]);
				}
				else
				{
					out.add(criteriaValue);
				}
			}
			
		}
		return out.toArray(new Object[out.size()]);
	}

	/**
	 * A criterion operator. 
	 */
	public static enum Operator
	{
		EQUAL("="),
		NOT_EQUAL("<>"),
		LESS("<"),
		LESS_EQUAL("<="),
		GREATER(">"),
		GREATER_EQUAL(">="),
		LIKE("LIKE"),
		NOT_LIKE("NOT LIKE"),
		;
		
		private final String op;
		
		private Operator(String op)
		{
			this.op = op;
		}
		
		@Override
		public String toString()
		{
			return op;
		}
	}
	
	/**
	 * A single criterion. 
	 */
	public static class Criterion
	{
		private String columnName;
		private Operator operator;
		private Object value;
		
		private Criterion(String columnName)
		{
			this.columnName = columnName;
			this.operator = null;
			this.value = null;
		}

		private Criterion(String columnName, Object ... values)
		{
			this.columnName = columnName;
			this.operator = null;
			this.value = values;
		}

		private Criterion(String columnName, Operator operator, Object value)
		{
			this.columnName = columnName;
			this.operator = operator;
			this.value = value;
		}
		
		public String toParameterizedString()
		{
			if (operator == null)
			{
				return "\"" + columnName + "\"" + " IS NULL";
			}
			else if (value.getClass().isArray())
			{
				StringBuilder sb = new StringBuilder();
				Object[] values = (Object[])value;
				for (int i = 0; i < values.length; i++)
				{
					sb.append("?");
					if (i < values.length - 1)
						sb.append(", ");
				}
				return "\"" + columnName + "\"" + " IN (" + sb.toString() + ")";
			}
			else
			{
				return "\"" + columnName + "\"" + " " + operator.toString() + " ?";
			}
		}
		
	}
	
	/**
	 * A single ordering criterion. 
	 */
	public static class Ordering
	{
		private String columnName;
		private Boolean ascending;
		
		private Ordering(String columnName)
		{
			this.columnName = columnName;
			this.ascending = null;
		}
		
		private Ordering(String columnName, boolean ascending)
		{
			this.columnName = columnName;
			this.ascending = ascending;
		}
		
		public String toClauseString()
		{
			return "\"" + columnName + "\"" + (ascending != null ? (ascending ? " ASC" : " DESC") : "");
		}
	}
	
	/**
	 * Query string builder.
	 */
	public static class QueryStringBuilder
	{
		private StringBuilder buffer;
		
		private QueryStringBuilder(String base)
		{
			this.buffer = new StringBuilder(base);
		}

		/**
		 * Adds "where" criteria.
		 * @param criteria the criteria. 
		 * @return this builder.
		 */
		public QueryStringBuilder where(Criterion ... criteria)
		{
			buffer.append("\nWHERE\n");
			for (int i = 0; i < criteria.length; i++)
			{
				buffer.append(criteria[i].toParameterizedString());
				if (i < criteria.length - 1)
					buffer.append('\n').append("AND ");
			}
			return this;
		}
		
		/**
		 * Adds "order by" criteria.
		 * @param ordering the ordering criteria. 
		 * @return this builder.
		 */
		public QueryStringBuilder orderBy(Ordering ... ordering)
		{
			buffer.append("\nORDER BY\n");
			for (int i = 0; i < ordering.length; i++)
			{
				buffer.append(ordering[i].toClauseString());
				if (i < ordering.length - 1)
					buffer.append(", ");
			}
			return this;
		}
		
		/**
		 * Adds "limit" criteria.
		 * @param value the limit value. 
		 * @return this builder.
		 */
		public QueryStringBuilder limit(int value)
		{
			buffer.append("\nLIMIT ").append(value);
			return this;
		}
		
		/**
		 * Adds "offset" criteria.
		 * @param value the limit value. 
		 * @return this builder.
		 */
		public QueryStringBuilder offset(int value)
		{
			buffer.append("\nOFFSET ").append(value);
			return this;
		}
		
		@Override
		public String toString()
		{
			return buffer.toString();
		}
		
	}

	/**
	 * Exception thrown when a Data Access Object fails at storage or retrieval unexpectedly.
	 */
	public class DataAccessFailureException extends RuntimeException
	{
		private static final long serialVersionUID = 7226646908480550007L;

		/**
		 * Creates a new exception.
		 * @param message the exception message.
		 */
		public DataAccessFailureException(String message)
		{
			super(message);
		}

		/**
		 * Creates a new exception.
		 * @param message the exception message.
		 * @param exception the exception cause.
		 */
		public DataAccessFailureException(String message, Throwable exception)
		{
			super(message, exception);
		}

	}

	/**
	 * Exception thrown when a Data Access Object fails at storage or retrieval unexpectedly.
	 */
	public class DataAccessTimeoutException extends DataAccessFailureException
	{
		private static final long serialVersionUID = 4130028520983746149L;

		/**
		 * Creates a new exception.
		 * @param message the exception message.
		 */
		public DataAccessTimeoutException(String message)
		{
			super(message);
		}

		/**
		 * Creates a new exception.
		 * @param message the exception message.
		 * @param exception the exception cause.
		 */
		public DataAccessTimeoutException(String message, Throwable exception)
		{
			super(message, exception);
		}

	}

}
