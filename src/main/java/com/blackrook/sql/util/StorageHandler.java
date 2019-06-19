package com.blackrook.sql.util;

import java.util.List;
import java.util.Map;

/**
 * Special Component singleton that manages retrieval and creation of individual data records.
 * @param <T> the object type returned from each retrieval.
 * @param <K> the primary key type.
 * @author Matthew Tropiano
 */
public interface StorageHandler<T extends Object, K extends Object>
{
	/**
	 * Adds an object to data storage, and returns it.
	 * The implementation must decide what constitutes a "new" record,
     * and MUST fail if this would either replace or add an existing record.
     * The returned object may contain additional data that a full
     * record should contain - this is what would be returned if an
     * add-plus-get was called. The PRIMARY KEY must either be taken from
	 * this record, or GENERATED using this record.
	 * @param object the object to add.
	 * @return the object that was added, represented in the storage layer, or
     * 		null if not added.
     */
	public T add(T object);

	/**
     * Retrieves an object from storage using an object's primary key.
     * The key should return at MOST one object and the SAME object for
     * the SAME key, as a one-to-one relationship.
     * @param primaryKey the key to use for retrieval.
     * @return the object corresponding to the key, or null if not found.
     */
	public T get(K primaryKey);

	/**
     * Returns multiple records for multiple primary keys.
     * The output array should always have the same length as the amount
     * of primary keys passed in, and the output array may contain
     * nulls in corresponding positions if the objects assigned to those
     * keys do not exist. Duplicate keys return duplicate records.
     * @param primaryKeys the keys to use for retrieval.
     * @return an array of the results of retrieving each respective record
     * 		for each key.
     * @see #get(Object)
     */
	public T[] getMultiple(@SuppressWarnings("unchecked") K ... primaryKeys);

	/**
	 * Returns a series of records that match a set of provided
	 * criteria. The key is the name of the field. 
	 * <p>
	 * It is up to the implementor of this method to determine how to 
	 * interpret the criteria values. This method must NEVER return null. 
	 * An empty list must be returned if none are found.
	 * @return a list of the retrieved data that fit the provided criteria.
	 * 		This will never return a null value.
	 */ 
	public List<T> getWhere(Map<String, Object> criteria);

	/**
	 * Like {@link #getWhere(Map)}, this returns
	 * data corresponding to provided criteria, however, this only returns
	 * the primary keys associated with the records found.
	 * <p>
	 * It is up to the implementor of this method to determine how to 
	 * interpret the criteria values. This method must NEVER return null. 
	 * An empty list must be returned if none are found.
	 * @return a list of the primary keys of retrieved data that fit the 
	 * provided criteria. This will never return a null value.
	 */ 
	public List<K> getKeys(Map<String, Object> criteria);

	/**
	 * Checks if a record exists by its primary key.
	 * The intent for this method is to be a lightweight check.
	 * Some implementations may force a retrieval, diminishing the
	 * effectiveness of this call. If this returns true, {@link #get(Object)} must
	 * succeed, if conditions do not change.
	 * @return true if the record exists in storage, false if not.
	 * @see #get(Object)
	 */
	public boolean contains(K key);

	/**
	 * Checks if all records that correspond to the provided keys in
	 * storage exist.
	 * The intent for this method is to be a lightweight check.
	 * Some implementations may force a retrieval, diminishing the
	 * effectiveness of this call. If this returns true, {@link #getMultiple(Object...)} must
	 * succeed and not contain nulls, if conditions do not change.
	 * @return true if the record exists in storage, false if not.
	 * @see #getMultiple(Object...)
	 */
	public boolean containsAll(@SuppressWarnings("unchecked") K ... keys);

	/**
	 * Checks if at least one record that corresponds to the provided 
	 * keys in storage exist.
	 * The intent for this method is to be a lightweight check.
	 * Some implementations may force a retrieval, diminishing the
	 * effectiveness of this call. If this returns true, {@link #getMultiple(Object...)} must
	 * succeed and contain at least one non-null record, if conditions do not change.
	 * @return true if the record exists in storage, false if not.
	 * @see #getMultiple(Object...)
	 */
	public boolean containsOne(@SuppressWarnings("unchecked") K ... keys);

	/**
	 * Updates an existing record in storage.
	 * This method should only update if and only if the record
	 * still exists, using the primary key taken from the record, as though
	 * {@link #update(Object, Object)} were called with the record key and the input object. 
	 * An attempt to update a record that doesn't exist MUST fail.
	 * @return true on successful update, false if not.
	 */ 
	public boolean update(T object);

	/**
	 * Updates an existing record in storage.
	 * This method should only update if and only if the record
	 * still exists. An attempt to update a record that doesn't exist
	 * MUST fail.
	 * @return true on successful update, false if not.
	 */ 
	public boolean update(K key, T object);

	/**
	 * Removes an existing record in storage.
	 * If successful, a call to {@link #contains(Object)} MUST return false,
	 * and a call to {@link #get(Object)} must return null, if conditions do not change.
	 * @return the record corresponding to the key that was removed,
	 *		or null if no corresponding key.
	 */ 
	public T remove(K key);

	/**
	 * Removes a set of existing records in storage.
	 * If one or more records do not exist, they show up as nulls in
	 * the output array.
	 * If successful, a call to {@link #containsAll(Object...)} MUST return false,
	 * and a call to {@link #getMultiple(Object...)} must return all nulls, if conditions do not change.
	 * @return the records corresponding to the keys that were removed.
	 */ 
	public T[] removeAll(@SuppressWarnings("unchecked") K ... key);

}
