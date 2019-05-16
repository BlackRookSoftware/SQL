/*******************************************************************************
 * Copyright (c) 2019 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package com.blackrook.sql;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.blackrook.sql.SQLTypeProfileFactory.MemberPolicy;
import com.blackrook.sql.SQLTypeProfileFactory.Profile;
import com.blackrook.sql.hints.DBIgnore;
import com.blackrook.sql.hints.DBName;

/**
 * A utility class that holds several helpful Reflection
 * functions and methods, mostly for passive error-handling.
 * @author Matthew Tropiano
 */
final class SQLReflect
{
	/** Default converter for {@link #createForType(Object, Class)}. */
	private static final SQLTypeProfileFactory PROFILE_FACTORY = new SQLTypeProfileFactory(new MemberPolicy()
	{
		@Override
		public boolean isIgnored(Field field)
		{
			return field.getAnnotation(DBIgnore.class) != null;
		}

		@Override
		public boolean isIgnored(Method method)
		{
			return method.getAnnotation(DBIgnore.class) != null;
		}

		@Override
		public String getAlias(Field field)
		{
			DBName anno = field.getAnnotation(DBName.class);
			return anno != null ? anno.value() : null;
		}

		@Override
		public String getAlias(Method method)
		{
			DBName anno = method.getAnnotation(DBName.class);
			return anno != null ? anno.value() : null;
		}
	});
	
	/** Default converter for {@link #createForType(Object, Class)}. */
	private static final SQLTypeConverter DEFAULT_CONVERTER = new SQLTypeConverter(PROFILE_FACTORY);

	private SQLReflect() {}
	
	/**
	 * Creates a new profile for a provided type.
	 * Generated profiles are stored in memory, and retrieved again by class type.
	 * <p>This method is thread-safe.
	 * @param <T> the class type.
	 * @param clazz the class.
	 * @return a new profile.
	 */
	public static <T> Profile<T> getProfile(Class<T> clazz)
	{
		return PROFILE_FACTORY.getProfile(clazz);
	}

	/**
	 * Creates a new instance of an object for placement in a POJO or elsewhere.
	 * @param <T> the return object type.
	 * @param object the object to convert to another object
	 * @param targetType the target class type to convert to, if the types differ.
	 * @return a suitable object of type <code>targetType</code>. 
	 * @throws ClassCastException if the incoming type cannot be converted.
	 */
	public static <T> T createForType(Object object, Class<T> targetType)
	{
		return DEFAULT_CONVERTER.createForType("source", object, targetType);
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
	public static <T> T createForType(String memberName, Object object, Class<T> targetType)
	{
		return DEFAULT_CONVERTER.createForType(memberName, object, targetType);
	}
	
	

}
