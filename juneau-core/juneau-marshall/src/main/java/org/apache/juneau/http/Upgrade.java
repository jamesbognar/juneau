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
 * Represents a parsed <l>Upgrade</l> HTTP request header.
 * 
 * <p>
 * Ask the client to upgrade to another protocol.
 * 
 * <h6 class='figure'>Example</h6>
 * <p class='bcode'>
 * 	Upgrade: HTTP/2.0, HTTPS/1.3, IRC/6.9, RTA/x11, websocket
 * </p>
 * 
 * <h6 class='topic'>RFC2616 Specification</h6>
 * 
 * The Upgrade general-header allows the client to specify what additional communication protocols it supports and
 * would like to use if the server finds it appropriate to switch protocols.
 * The server MUST use the Upgrade header field within a 101 (Switching Protocols) response to indicate which
 * protocol(s) are being switched.
 * 
 * <p class='bcode'>
 * 	Upgrade        = "Upgrade" ":" 1#product
 * </p>
 * 
 * <p>
 * For example,
 * <p class='bcode'>
 * 	Upgrade: HTTP/2.0, SHTTP/1.3, IRC/6.9, RTA/x11
 * </p>
 * 
 * <p>
 * The Upgrade header field is intended to provide a simple mechanism for transition from HTTP/1.1 to some other,
 * incompatible protocol.
 * It does so by allowing the client to advertise its desire to use another protocol, such as a later version of HTTP
 * with a higher major version number, even though the current request has been made using HTTP/1.1.
 * This eases the difficult transition between incompatible protocols by allowing the client to initiate a request in
 * the more commonly supported protocol while indicating to the server that it would like to use a "better" protocol if
 * available (where "better" is determined by the server, possibly according to the nature of the method and/or resource
 * being requested).
 * 
 * <p>
 * The Upgrade header field only applies to switching application-layer protocols upon the existing transport-layer
 * connection.
 * Upgrade cannot be used to insist on a protocol change; its acceptance and use by the server is optional.
 * The capabilities and nature of the application-layer communication after the protocol change is entirely dependent
 * upon the new protocol chosen, although the first action after changing the protocol MUST be a response to the initial
 * HTTP request containing the Upgrade header field.
 * 
 * <p>
 * The Upgrade header field only applies to the immediate connection.
 * Therefore, the upgrade keyword MUST be supplied within a Connection header field (section 14.10) whenever Upgrade is
 * present in an HTTP/1.1 message.
 * 
 * <p>
 * The Upgrade header field cannot be used to indicate a switch to a protocol on a different connection.
 * For that purpose, it is more appropriate to use a 301, 302, 303, or 305 redirection response.
 * 
 * <p>
 * This specification only defines the protocol name "HTTP" for use by the family of Hypertext Transfer Protocols, as
 * defined by the HTTP version rules of section 3.1 and future updates to this specification.
 * Any token can be used as a protocol name; however, it will only be useful if both the client and server associate
 * the name with the same protocol.
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
public final class Upgrade extends HeaderStringArray {

	/**
	 * Returns a parsed <code>Upgrade</code> header.
	 * 
	 * @param value The <code>Upgrade</code> header string.
	 * @return The parsed <code>Upgrade</code> header, or <jk>null</jk> if the string was null.
	 */
	public static Upgrade forString(String value) {
		if (value == null)
			return null;
		return new Upgrade(value);
	}

	private Upgrade(String value) {
		super(value);
	}
}
