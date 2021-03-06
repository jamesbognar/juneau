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

/**
 * Represents a parsed <l>Trailer</l> HTTP response header.
 * 
 * <p>
 * The Trailer general field value indicates that the given set of header fields is present in the trailer of a message
 * encoded with chunked transfer coding.
 * 
 * <h6 class='figure'>Example</h6>
 * <p class='bcode'>
 * 	Trailer: Max-Forwards
 * </p>
 * 
 * <h6 class='topic'>RFC2616 Specification</h6>
 * 
 * The Trailer general field value indicates that the given set of header fields is present in the trailer of a message
 * encoded with chunked transfer-coding.
 * 
 * <p class='bcode'>
 * 	Trailer  = "Trailer" ":" 1#field-name
 * </p>
 * 
 * <p>
 * An HTTP/1.1 message SHOULD include a Trailer header field in a message using chunked transfer-coding with a non-empty
 * trailer.
 * Doing so allows the recipient to know which header fields to expect in the trailer.
 * 
 * <p>
 * If no Trailer header field is present, the trailer SHOULD NOT include any header fields.
 * See section 3.6.1 for restrictions on the use of trailer fields in a "chunked" transfer-coding.
 * 
 * <p>
 * Message header fields listed in the Trailer header field MUST NOT include the following header fields:
 * <ul>
 * 	<li>Transfer-Encoding
 * 	<li>Content-Length
 * 	<li>Trailer
 * </ul>
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
public final class Trailer extends HeaderString {

	/**
	 * Returns a parsed <code>Trailer</code> header.
	 * 
	 * @param value The <code>Trailer</code> header string.
	 * @return The parsed <code>Trailer</code> header, or <jk>null</jk> if the string was null.
	 */
	public static Trailer forString(String value) {
		if (value == null)
			return null;
		return new Trailer(value);
	}

	private Trailer(String value) {
		super(value);
	}
}
