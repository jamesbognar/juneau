// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau;

import static org.apache.juneau.internal.ClassUtils.*;

import org.apache.juneau.json.*;

/**
 * General invalid conversion exception.
 * 
 * <p>
 * Exception that gets thrown if you try to perform an invalid conversion, such as when calling
 * {@code ObjectMap.getInt(...)} on a non-numeric <code>String</code>.
 */
public final class InvalidDataConversionException extends FormattedRuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * @param toType Attempting to convert to this class type.
	 * @param cause The cause.
	 * @param value The value being converted.
	 */
	public InvalidDataConversionException(Object value, Class<?> toType, Exception cause) {
		super(cause, "Invalid data conversion from type ''{0}'' to type ''{1}''.  Value={2}.",
			getReadableClassNameForObject(value), getReadableClassName(toType), getValue(value));
	}

	/**
	 * @param toType Attempting to convert to this class type.
	 * @param cause The cause.
	 * @param value The value being converted.
	 */
	public InvalidDataConversionException(Object value, ClassMeta<?> toType, Exception cause) {
		super(cause, "Invalid data conversion from type ''{0}'' to type ''{1}''.  Value={2}.",
			getReadableClassNameForObject(value), toType.toString(), getValue(value));
	}

	private static String getValue(Object o) {
		if (o instanceof Class)
			return "'" + getReadableClassName((Class<?>)o) + "'";
		return JsonSerializer.DEFAULT_LAX == null ? "'" + o.toString() + "'" : JsonSerializer.DEFAULT_LAX.toString(o);
	}
}
