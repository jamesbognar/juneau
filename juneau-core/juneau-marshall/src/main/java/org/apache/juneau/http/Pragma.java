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
 * Represents a parsed <l>Pragma</l> HTTP request/response header.
 * 
 * <p>
 * Implementation-specific fields that may have various effects anywhere along the request-response chain.
 * 
 * <h6 class='figure'>Example</h6>
 * <p class='bcode'>
 * 	Pragma: no-cache
 * </p>
 * 
 * <h6 class='topic'>RFC2616 Specification</h6>
 * 
 * The Pragma general-header field is used to include implementation- specific directives that might apply to any
 * recipient along the request/response chain.
 * All pragma directives specify optional behavior from the viewpoint of the protocol; however, some systems MAY
 * require that behavior be consistent with the directives.
 * 
 * <p class='bcode'>
 * 	Pragma            = "Pragma" ":" 1#pragma-directive
 * 	pragma-directive  = "no-cache" | extension-pragma
 * 	extension-pragma  = token [ "=" ( token | quoted-string ) ]
 * </p>
 * 
 * <p>
 * When the no-cache directive is present in a request message, an application SHOULD forward the request toward the
 * origin server even if it has a cached copy of what is being requested.
 * This pragma directive has the same semantics as the no-cache cache-directive (see section 14.9) and is defined here
 * for backward compatibility with HTTP/1.0.
 * Clients SHOULD include both header fields when a no-cache request is sent to a server not known to be HTTP/1.1
 * compliant.
 * 
 * <p>
 * Pragma directives MUST be passed through by a proxy or gateway application, regardless of their significance to that
 * application, since the directives might be applicable to all recipients along the request/response chain.
 * It is not possible to specify a pragma for a specific recipient; however, any pragma directive not relevant to a
 * recipient SHOULD be ignored by that recipient.
 * 
 * <p>
 * HTTP/1.1 caches SHOULD treat "Pragma: no-cache" as if the client had sent "Cache-Control: no-cache".
 * No new Pragma directives will be defined in HTTP.
 * 
 * <p>
 * Note: because the meaning of "Pragma: no-cache as a response header field is not actually specified, it does not
 * provide a reliable replacement for "Cache-Control: no-cache" in a response.
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
public final class Pragma extends HeaderString {

	/**
	 * Returns a parsed <code>Pragma</code> header.
	 * 
	 * @param value The <code>Pragma</code> header string.
	 * @return The parsed <code>Pragma</code> header, or <jk>null</jk> if the string was null.
	 */
	public static Pragma forString(String value) {
		if (value == null)
			return null;
		return new Pragma(value);
	}

	private Pragma(String value) {
		super(value);
	}
}
