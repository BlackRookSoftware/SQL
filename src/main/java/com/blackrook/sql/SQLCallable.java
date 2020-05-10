/*******************************************************************************
 * Copyright (c) 2019-2020 Black Rook Software
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at 
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.sql;

import java.util.Arrays;
import java.util.Collection;

import com.blackrook.sql.util.SQLRuntimeException;

/**
 * The interface used for all SQL connections that can have queries performed through them.
 * @author Matthew Tropiano
 */
public interface SQLCallable
{
	/** Default batch size. */
	static final int DEFAULT_BATCH_SIZE = 1024;

	/**
	 * Performs a query and extracts the first row result into a single {@link SQLRow}.
	 * @param query the query statement to execute.
	 * @param parameters list of parameters for parameterized queries.
	 * @return the single result row returned, or null if no row returned.
	 * @throws SQLRuntimeException if the query cannot be executed or the query causes an error.
	 */
	SQLRow getRow(String query, Object ... parameters);

	/**
	 * Performs a query and creates an object from it from the first result row extracted, setting relevant fields.
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
	 * @param type the class type to instantiate.
	 * @param query the query to execute.
	 * @param parameters list of parameters for parameterized queries.
	 * @return an instantiated object with the pertinent fields set, or null if no rows.
	 * @throws SQLRuntimeException if the query cannot be executed or the query causes an error.
	 * @throws ClassCastException if one object type cannot be converted to another.
	 */
	<T> T getRow(Class<T> type, String query, Object ... parameters);

	/**
	 * Performs a query that returns rows and extracts them into a result.
	 * @param query the query to execute.
	 * @param parameters list of parameters for parameterized queries.
	 * @return the result of the query.
	 * @throws SQLRuntimeException if the query cannot be executed or the query causes an error.
	 */
	SQLResult getResult(String query, Object ... parameters);

	/**
	 * Performs a query and creates objects from the resultant rows, setting relevant fields on them.
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
	 * @param type the class type to instantiate.
	 * @param query the query to execute.
	 * @param parameters list of parameters for parameterized queries.
	 * @return an array of instantiated objects with the pertinent fields set for each row.
	 * @throws SQLRuntimeException if the query cannot be executed or the query causes an error.
	 * @throws ClassCastException if one object type cannot be converted to another.
	 */
	<T> T[] getResult(Class<T> type, String query, Object ... parameters);

	/**
	 * Performs an update query (INSERT, DELETE, UPDATE, or other commands that do not return rows)
	 * and extracts the data/affected data/generated data into a SQLResult.
	 * @param query the query to execute.
	 * @param parameters list of parameters for parameterized queries.
	 * @return the update result returned (usually number of rows affected and or generated ids).
	 * @throws SQLRuntimeException if the query cannot be executed or the query causes an error.
	 */
	SQLResult getUpdateResult(String query, Object ... parameters);
	
	/**
	 * Performs a series of update queries on a single statement on a connection and returns the batch result.
	 * @param query the query statement to execute.
	 * @param parameterList the list of parameter sets to pass to the query for each update. 
	 * @return the update result returned (usually number of rows affected and or generated ids).
	 * @throws SQLRuntimeException if the query cannot be executed or the query causes an error.
	 * @since 1.1.0
	 * @since 1.2.0, returns <code>int[]</code>. See {@link #getUpdateLargeBatch(String, Object[][])} for <code>long[]</code>.
	 */
	default int[] getUpdateBatch(String query, Object[][] parameterList)
	{
		return getUpdateBatch(query, DEFAULT_BATCH_SIZE, Arrays.asList(parameterList));
	}
	
	/**
	 * Performs a series of update queries on a single statement on a connection and returns the batch result.
	 * @param query the query statement to execute.
	 * @param granularity the amount of statements to execute at a time. If 0 or less, no granularity.
	 * @param parameterList the list of parameter sets to pass to the query for each update. 
	 * @return the update result returned (usually number of rows affected and or generated ids).
	 * @throws SQLRuntimeException if the query cannot be executed or the query causes an error.
	 * @since 1.1.0
	 * @since 1.2.0, returns <code>int[]</code>. See {@link #getUpdateLargeBatch(String, int, Object[][])} for <code>long[]</code>.
	 */
	default int[] getUpdateBatch(String query, int granularity, Object[][] parameterList)
	{
		return getUpdateBatch(query, granularity, Arrays.asList(parameterList));
	}
	
	/**
	 * Performs a series of update queries on a single statement on a connection and returns the batch result.
	 * @param query the query statement to execute.
	 * @param parameterList the list of parameter sets to pass to the query for each update. 
	 * @return the update result returned (usually number of rows affected and or generated ids).
	 * @throws SQLRuntimeException if the query cannot be executed or the query causes an error.
	 * @since 1.1.0
	 * @since 1.2.0, returns <code>int[]</code>. See {@link #getUpdateLargeBatch(String, Collection)} for <code>long[]</code>.
	 */
	default int[] getUpdateBatch(String query, Collection<Object[]> parameterList)
	{
		return getUpdateBatch(query, DEFAULT_BATCH_SIZE, parameterList);
	}
	
	/**
	 * Performs a series of update queries on a single statement on a connection and returns the batch result.
	 * @param query the query statement to execute.
	 * @param granularity the amount of statements to execute at a time. If 0 or less, no granularity.
	 * @param parameterList the list of parameter sets to pass to the query for each update. 
	 * @return the update result returned (usually number of rows affected and or generated ids).
	 * @throws SQLRuntimeException if the query cannot be executed or the query causes an error.
	 * @since 1.1.0
	 * @since 1.2.0, returns <code>int[]</code>. See {@link #getUpdateLargeBatch(String, int, Collection)} for <code>long[]</code>.
	 */
	int[] getUpdateBatch(String query, int granularity, Collection<Object[]> parameterList);

	/**
	 * Performs a series of update queries on a single statement on a connection and returns the batch result.
	 * @param query the query statement to execute.
	 * @param parameterList the list of parameter sets to pass to the query for each update. 
	 * @return the update result returned (usually number of rows affected and or generated ids).
	 * @throws SQLRuntimeException if the query cannot be executed or the query causes an error.
	 * @since 1.2.0
	 */
	default long[] getUpdateLargeBatch(String query, Object[][] parameterList)
	{
		return getUpdateLargeBatch(query, DEFAULT_BATCH_SIZE, Arrays.asList(parameterList));
	}
	
	/**
	 * Performs a series of update queries on a single statement on a connection and returns the batch result.
	 * @param query the query statement to execute.
	 * @param granularity the amount of statements to execute at a time. If 0 or less, no granularity.
	 * @param parameterList the list of parameter sets to pass to the query for each update. 
	 * @return the update result returned (usually number of rows affected and or generated ids).
	 * @throws SQLRuntimeException if the query cannot be executed or the query causes an error.
	 * @since 1.2.0
	 */
	default long[] getUpdateLargeBatch(String query, int granularity, Object[][] parameterList)
	{
		return getUpdateLargeBatch(query, granularity, Arrays.asList(parameterList));
	}
	
	/**
	 * Performs a series of update queries on a single statement on a connection and returns the batch result.
	 * @param query the query statement to execute.
	 * @param parameterList the list of parameter sets to pass to the query for each update. 
	 * @return the update result returned (usually number of rows affected and or generated ids).
	 * @throws SQLRuntimeException if the query cannot be executed or the query causes an error.
	 * @since 1.2.0
	 */
	default long[] getUpdateLargeBatch(String query, Collection<Object[]> parameterList)
	{
		return getUpdateLargeBatch(query, DEFAULT_BATCH_SIZE, parameterList);
	}
	
	/**
	 * Performs a series of update queries on a single statement on a connection and returns the batch result.
	 * @param query the query statement to execute.
	 * @param granularity the amount of statements to execute at a time. If 0 or less, no granularity.
	 * @param parameterList the list of parameter sets to pass to the query for each update. 
	 * @return the update result returned (usually number of rows affected and or generated ids).
	 * @throws SQLRuntimeException if the query cannot be executed or the query causes an error.
	 * @since 1.2.0
	 */
	long[] getUpdateLargeBatch(String query, int granularity, Collection<Object[]> parameterList);

	/**
	 * Performs an update query (INSERT, DELETE, UPDATE, or other commands that do not return rows)
	 * and extracts each set of result data into a SQLResult.
	 * <p>This is usually more efficient than multiple calls of {@link #getUpdateResult(String, Object...)},
	 * since it uses the same prepared statement. However, it is not as efficient as {@link #getUpdateBatch(String, int, Collection)},
	 * but for this method, you will get the generated ids in each result, if any.
	 * @param query the query statement to execute.
	 * @param parameterList the list of parameter sets to pass to the query for each update. 
	 * @return the list of update results returned, each corresponding to an update.
	 * @throws SQLRuntimeException if the query cannot be executed or the query causes an error.
	 * @since 1.1.0
	 */
	default SQLResult[] getUpdateBatchResult(String query, Object[][] parameterList)
	{
		return getUpdateBatchResult(query, Arrays.asList(parameterList));
	}
	
	/**
	 * Performs an update query (INSERT, DELETE, UPDATE, or other commands that do not return rows)
	 * and extracts each set of result data into a SQLResult.
	 * <p>This is usually more efficient than multiple calls of {@link #getUpdateResult(String, Object...)},
	 * since it uses the same prepared statement. However, it is not as efficient as {@link #getUpdateBatch(String, int, Collection)},
	 * but for this method, you will get the generated ids in each result, if any.
	 * @param query the query statement to execute.
	 * @param parameterList the list of parameter sets to pass to the query for each update. 
	 * @return the list of update results returned, each corresponding to an update.
	 * @throws SQLRuntimeException if the query cannot be executed or the query causes an error.
	 * @since 1.1.0
	 */
	SQLResult[] getUpdateBatchResult(String query, Collection<Object[]> parameterList);
	
}
