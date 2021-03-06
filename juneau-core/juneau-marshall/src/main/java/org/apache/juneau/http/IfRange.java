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
package org.apache.juneau.http;

import static org.apache.juneau.internal.StringUtils.*;

import org.apache.juneau.internal.*;

/**
 * Represents a parsed <l>If-Range</l> HTTP request header.
 * 
 * <p>
 * If the entity is unchanged, send me the part(s) that I am missing; otherwise, send me the entire new entity.
 * 
 * <h6 class='figure'>Example</h6>
 * <p class='bcode'>
 * 	If-Range: "737060cd8c284d8af7ad3082f209582d"
 * </p>
 * 
 * <h6 class='topic'>RFC2616 Specification</h6>
 * 
 * If a client has a partial copy of an entity in its cache, and wishes to have an up-to-date copy of the entire entity
 * in its cache, it could use the Range request-header with a conditional GET (using either or both of
 * If-Unmodified-Since and If-Match.)
 * However, if the condition fails because the entity has been modified, the client would then have to make a second
 * request to obtain the entire current entity-body.
 * 
 * <p>
 * The If-Range header allows a client to "short-circuit" the second request.
 * Informally, its meaning is `if the entity is unchanged, send me the part(s) that I am missing; otherwise, send me
 * the entire new entity'.
 * <p class='bcode'>
 * 	If-Range = "If-Range" ":" ( entity-tag | HTTP-date )
 * </p>
 * 
 * <p>
 * If the client has no entity tag for an entity, but does have a Last- Modified date, it MAY use that date in an
 * If-Range header.
 * (The server can distinguish between a valid HTTP-date and any form of entity-tag by examining no more than two
 * characters.)
 * The If-Range header SHOULD only be used together with a Range header, and MUST be ignored if the request does not
 * include a Range header, or if the server does not support the sub-range operation.
 * 
 * <p>
 * If the entity tag given in the If-Range header matches the current entity tag for the entity, then the server SHOULD
 * provide the specified sub-range of the entity using a 206 (Partial content) response.
 * If the entity tag does not match, then the server SHOULD return the entire entity using a 200 (OK) response.
 * 
 * <h6 class='topic'>Additional Information</h6>
 * <ul class='doctree'>
 * 	<li class='jp'>
 * 		<a class='doclink' href='package-summary.html#TOC'>org.apache.juneau.http</a>
 * 	<li class='extlink'>
 * 		<a class='doclink' href='https://www.w3.org/Protocols/rfc2616/rfc2616.html'>
 * 		Hypertext Transfer Protocol -- HTTP/1.1</a>
 * </ul>
 */
public final class IfRange extends HeaderString {

	/**
	 * Returns a parsed <code>If-Range</code> header.
	 * 
	 * @param value The <code>If-Range</code> header string.
	 * @return The parsed <code>If-Range</code> header, or <jk>null</jk> if the string was null.
	 */
	public static IfRange forString(String value) {
		if (value == null)
			return null;
		return new IfRange(value);
	}

	private IfRange(String value) {
		super(value);
	}

	/**
	 * Returns this header value as a {@link java.util.Date} object.
	 * 
	 * @return This header value as a {@link java.util.Date} object, or <jk>null</jk> if the value is not a date.
	 */
	public java.util.Date asDate() {
		char c0 = charAt(value, 0), c1 = charAt(value, 1);
		if (c0 == '*' || c0 == '"' || (c0 == 'W' && c1 == '/'))
			return null;
		return DateUtils.parseDate(toString());
	}

	/**
	 * Returns this header value as an {@link EntityValidator} object.
	 * 
	 * @return
	 * 	This header value as a {@link EntityValidator} object, or <jk>null</jk> if the value is not an entity
	 * 	validator.
	 */
	public EntityValidator asValidator() {
		char c0 = charAt(value, 0), c1 = charAt(value, 1);
		if (c0 == '*' || c0 == '"' || (c0 == 'W' && c1 == '/'))
			return new EntityValidator(value);
		return null;
	}
}
