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
package org.apache.juneau.xml;

import static org.apache.juneau.internal.StringUtils.*;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.xml.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.xml.annotation.*;

/**
 * XML utility methods.
 */
public final class XmlUtils {

	//--------------------------------------------------------------------------------
	// XML element names
	//--------------------------------------------------------------------------------

	/**
	 * Encodes any invalid XML element name characters to <code>_x####_</code> sequences.
	 * 
	 * @param w The writer to send the output to.
	 * @param o The object being encoded.
	 * @return The same writer passed in.
	 * @throws IOException Throw by the writer.
	 */
	public static final Writer encodeElementName(Writer w, Object o) throws IOException {

		if (o == null)
			return w.append("_x0000_");

		String s = o.toString();

		if (needsElementNameEncoding(s))
			return encodeElementNameInner(w, s);

		w.append(s);
		return w;
	}

	/**
	 * Encodes any invalid XML element name characters to <code>_x####_</code> sequences.
	 * 
	 * @param o The object being encoded.
	 * @return The encoded element name string.
	 */
	public static final String encodeElementName(Object o) {
		if (o == null)
			return "_x0000_";

		String s = o.toString();
		if (s.isEmpty())
			return "_xE000_";
		try {
			if (needsElementNameEncoding(s))
				try (Writer w = new StringBuilderWriter(s.length() * 2)) {
					return encodeElementNameInner(w, s).toString();
				}
		} catch (IOException e) {
			throw new RuntimeException(e); // Never happens
		}

		return s;
	}

	private static final Writer encodeElementNameInner(Writer w, String s) throws IOException {
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if ((c >= 'A' && c <= 'Z')
					|| (c == '_' && ! isEscapeSequence(s,i))
					|| (c >= 'a' && c <= 'z')
					|| (i != 0 && (
							c == '-'
							|| c == '.'
							|| (c >= '0' && c <= '9')
							|| c == '\u00b7'
							|| (c >= '\u0300' && c <= '\u036f')
							|| (c >= '\u203f' && c <= '\u2040')
						))
					|| (c >= '\u00c0' && c <= '\u00d6')
					|| (c >= '\u00d8' && c <= '\u00f6')
					|| (c >= '\u00f8' && c <= '\u02ff')
					|| (c >= '\u0370' && c <= '\u037d')
					|| (c >= '\u037f' && c <= '\u1fff')
					|| (c >= '\u200c' && c <= '\u200d')
					|| (c >= '\u2070' && c <= '\u218f')
					|| (c >= '\u2c00' && c <= '\u2fef')
					|| (c >= '\u3001' && c <= '\ud7ff')
					|| (c >= '\uf900' && c <= '\ufdcf')
					|| (c >= '\ufdf0' && c <= '\ufffd')) {
				w.append(c);
			}  else {
				appendPaddedHexChar(w, c);
			}
		}
		return w;
	}

	private static final boolean needsElementNameEncoding(String s) {
		// Note that this doesn't need to be perfect, just fast.
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (! (c >= '0' && c <= '9' || c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z'))
				return true;
			if (i == 0 && (c >= '0' && c <= '9'))
				return true;
		}
		return false;
	}

	//--------------------------------------------------------------------------------
	// XML element text
	//--------------------------------------------------------------------------------

	/**
	 * Escapes invalid XML text characters to <code>_x####_</code> sequences.
	 * 
	 * @param o The object being encoded.
	 * @return The encoded string.
	 */
	public static final String escapeText(Object o) {

		if (o == null)
			return "_x0000_";

		String s = o.toString();

		try {
			if (! needsTextEncoding(s))
				return s;
			final int len = s.length();
			StringWriter sw = new StringWriter(s.length()*2);
			for (int i = 0; i < len; i++) {
				char c = s.charAt(i);
				if ((i == 0 || i == len-1) && Character.isWhitespace(c))
					appendPaddedHexChar(sw, c);
				else if (c == '_' && isEscapeSequence(s,i))
					appendPaddedHexChar(sw, c);
				else if (isValidXmlCharacter(c))
					sw.append(c);
				else
					appendPaddedHexChar(sw, c);
			}
			return sw.toString();
		} catch (IOException e) {
			throw new RuntimeException(e); // Never happens
		}
	}

	/**
	 * Encodes the specified element text and sends the results to the specified writer.
	 * 
	 * <p>
	 * Encodes any invalid XML text characters to <code>_x####_</code> sequences and sends the response to the specified
	 * writer.
	 * <br>Encodes <js>'&amp;'</js>, <js>'&lt;'</js>, and <js>'&gt;'</js> as XML entities.
	 * <br>Encodes invalid XML text characters to <code>_x####_</code> sequences.
	 * 
	 * @param w The writer to send the output to.
	 * @param o The object being encoded.
	 * @param trim Trim the text before serializing it.
	 * @param preserveWhitespace
	 * 	Specifies whether we're in preserve-whitespace mode.
	 * 	(e.g. {@link XmlFormat#MIXED_PWS} or {@link XmlFormat#TEXT_PWS}.
	 * 	If <jk>true</jk>, leading and trailing whitespace characters will be encoded.
	 * @return The same writer passed in.
	 * @throws IOException Thrown from the writer.
	 */
	public static final Writer encodeText(Writer w, Object o, boolean trim, boolean preserveWhitespace) throws IOException {

		if (o == null)
			return w.append("_x0000_");

		String s = o.toString();
		if (s.isEmpty())
			return w.append("_xE000_");
		if (trim)
			s = s.trim();

		if (needsTextEncoding(s)) {
			final int len = s.length();
			for (int i = 0; i < len; i++) {
				char c = s.charAt(i);
				if ((i == 0 || i == len-1) && Character.isWhitespace(c) && ! preserveWhitespace)
					appendPaddedHexChar(w, c);
				else if (REPLACE_TEXT.contains(c))
					w.append(REPLACE_TEXT.get(c));
				else if (c == '_' && isEscapeSequence(s,i))
					appendPaddedHexChar(w, c);
				else if (isValidXmlCharacter(c))
					w.append(c);
				else
					appendPaddedHexChar(w, c);
			}
		} else {
			w.append(s);
		}

		return w;
	}

	private static final boolean needsTextEncoding(String s) {
		// See if we need to convert the string.
		// Conversion is somewhat expensive, so make sure we need to do so before hand.
		final int len = s.length();
		for (int i = 0; i < len; i++) {
			char c = s.charAt(i);
			if ((i == 0 || i == len-1) && Character.isWhitespace(c))
				return true;
			if (REPLACE_TEXT.contains(c) || ! isValidXmlCharacter(c) || (c == '_' && isEscapeSequence(s,i)))
				return true;
		}
		return false;
	}

	private static AsciiMap REPLACE_TEXT = new AsciiMap()
		.append('&', "&amp;")
		.append('<', "&lt;")
		.append('>', "&gt;")
		.append((char)0x09, "&#x0009;")
		.append((char)0x0A, "&#x000a;")
		.append((char)0x0D, "&#x000d;");


	//--------------------------------------------------------------------------------
	// XML attribute names
	//--------------------------------------------------------------------------------

	/**
	 * Serializes and encodes the specified object as valid XML attribute name.
	 * 
	 * @param w The writer to send the output to.
	 * @param o The object being serialized.
	 * @return This object (for method chaining).
	 * @throws IOException If a problem occurred.
	 */
	public static final Writer encodeAttrName(Writer w, Object o) throws IOException {

		if (o == null)
			return w.append("_x0000_");

		String s = o.toString();

		if (needsAttrNameEncoding(s)) {
			for (int i = 0; i < s.length(); i++) {
				char c = s.charAt(i);
				if (i == 0) {
					if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c == ':')
						w.append(c);
					else if (c == '_' && ! isEscapeSequence(s,i))
						w.append(c);
					else
						appendPaddedHexChar(w, c);
				} else {
					if ((c >= '0' && c <= '9' || c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c == ':'))
						w.append(c);
					else if (c == '_' && ! isEscapeSequence(s,i))
						w.append(c);
					else
						appendPaddedHexChar(w, c);
				}
			}
		} else {
			w.append(s);
		}

		return w;
	}

	private static final boolean needsAttrNameEncoding(String s) {
		// Note that this doesn't need to be perfect, just fast.
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (! (c >= '0' && c <= '9' || c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z'))
				return true;
			if (i == 0 && ! (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z'))
				return true;
		}
		return false;
	}

	//--------------------------------------------------------------------------------
	// XML attribute values
	//--------------------------------------------------------------------------------

	/**
	 * Encodes the specified attribute value and sends the results to the specified writer.
	 * 
	 * <p>
	 * Encodes any invalid XML text characters to <code>_x####_</code> sequences and sends the response to the specified
	 * writer.
	 * <br>Encodes <js>'&amp;'</js>, <js>'&lt;'</js>, <js>'&gt;'</js>, <js>'"'</js>, and <js>'\''</js> as XML entities.
	 * <br>Encodes invalid XML text characters to <code>_x####_</code> sequences.
	 * 
	 * @param w The writer to send the output to.
	 * @param o The object being encoded.
	 * @param trim
	 * 	Trim the text before serializing it.
	 * 	If <jk>true</jk>, leading and trailing whitespace characters will be encoded.
	 * @return The same writer passed in.
	 * @throws IOException Thrown from the writer.
	 */
	public static final Writer encodeAttrValue(Writer w, Object o, boolean trim) throws IOException {
		if (o == null)
			return w.append("_x0000_");

		String s = o.toString();
		if (s.isEmpty())
			return w;
		if (trim)
			s = s.trim();

		if (needsAttrValueEncoding(s)) {
			final int len = s.length();
			for (int i = 0; i < len; i++) {
				char c = s.charAt(i);
				if ((i == 0 || i == len-1) && Character.isWhitespace(c))
					appendPaddedHexChar(w, c);
				else if (REPLACE_ATTR_VAL.contains(c))
					w.append(REPLACE_ATTR_VAL.get(c));
				else if (c == '_' && isEscapeSequence(s,i))
					appendPaddedHexChar(w, c);
				else if (isValidXmlCharacter(c))
					w.append(c);
				else
					appendPaddedHexChar(w, c);
			}
		} else {
			w.append(s);
		}

		return w;
	}

	private static final boolean needsAttrValueEncoding(String s) {
		// See if we need to convert the string.
		// Conversion is somewhat expensive, so make sure we need to do so before hand.
		final int len = s.length();
		for (int i = 0; i < len; i++) {
			char c = s.charAt(i);
			if ((i == 0 || i == len-1) && Character.isWhitespace(c))
				return true;
			if (REPLACE_ATTR_VAL.contains(c) || ! isValidXmlCharacter(c) || (c == '_' && isEscapeSequence(s,i)))
				return true;
		}
		return false;
	}

	private static AsciiMap REPLACE_ATTR_VAL = new AsciiMap()
		.append('&', "&amp;")
		.append('<', "&lt;")
		.append('>', "&gt;")
		.append('"', "&quot;")
		.append('\'', "&apos;")
		.append((char)0x09, "&#x0009;")
		.append((char)0x0A, "&#x000a;")
		.append((char)0x0D, "&#x000d;");


	//--------------------------------------------------------------------------------
	// Decode XML text
	//--------------------------------------------------------------------------------

	/**
	 * Translates any _x####_ sequences (introduced by the various encode methods) back into their original characters.
	 * 
	 * @param s The string being decoded.
	 * @param sb The string builder to use as a scratch pad.
	 * @return The decoded string.
	 */
	public static final String decode(String s, StringBuilder sb) {
		if (s == null) return null;
		if (s.length() == 0)
			return s;
		if (s.indexOf('_') == -1)
			return s;

		if (sb == null)
			sb = new StringBuilder(s.length());
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '_' && isEscapeSequence(s,i)) {

				int x = Integer.parseInt(s.substring(i+2, i+6), 16);

				// If we find _x0000_, then that means a null.
				// If we find _xE000_, then that means an empty string.
				if (x == 0)
					return null;
				else if (x != 0xE000)
					sb.append((char)x);

				i+=6;
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}


	/**
	 * Given a list of Strings and other Objects, combines Strings that are next to each other in the list.
	 * 
	 * @param l The list of text nodes to collapse.
	 * @return The same list.
	 */
	public static LinkedList<Object> collapseTextNodes(LinkedList<Object> l) {

		String prev = null;
		for (ListIterator<Object> i = l.listIterator(); i.hasNext();) {
			Object o = i.next();
			if (o instanceof String) {
				if (prev == null)
					prev = o.toString();
				else {
					prev += o;
					i.remove();
					i.previous();
					i.remove();
					i.add(prev);
				}
			} else {
				prev = null;
			}
		}
		return l;
	}

	//--------------------------------------------------------------------------------
	// Other methods
	//--------------------------------------------------------------------------------

	// Returns true if the specified character can safely be used in XML text or an attribute.
	private static final boolean isValidXmlCharacter(char c) {
		return (c >= 0x20 && c <= 0xD7FF) /*|| c == 0xA || c == 0xD*/ || (c >= 0xE000 && c <= 0xFFFD);
	}

	// Returns true if the string at the specified position is of the form "_x####_"
	// where '#' are hexadecimal characters.
	private static final boolean isEscapeSequence(String s, int i) {
		return s.length() > i+6
			&& s.charAt(i) == '_'
			&& s.charAt(i+1) == 'x'
			&& isHexCharacter(s.charAt(i+2))
			&& isHexCharacter(s.charAt(i+3))
			&& isHexCharacter(s.charAt(i+4))
			&& isHexCharacter(s.charAt(i+5))
			&& s.charAt(i+6) == '_';
	}

	// Returns true if the character is a hexadecimal character
	private static final boolean isHexCharacter(char c) {
		return (c >= '0' && c <= '9') || (c >= 'A' && c <= 'F');
	}

	// Converts an integer to a hexadecimal string padded to 4 places.
	private static final Writer appendPaddedHexChar(Writer out, int num) throws IOException {
		out.append("_x");
		for (char c : toHex(num))
			out.append(c);
		return out.append('_');
	}

	/**
	 * Find the namespace given a list of <ja>@Xml</ja> and <ja>@XmlSchema</ja> annotations.
	 * 
	 * <p>
	 * The annotations should be a child-to-parent ordering of annotations found on a class or method.
	 * 
	 * @param xmls The list of <ja>@Xml</ja> annotations.
	 * @param schemas The list of <ja>@XmlSchema</ja> annotations.
	 * @return The namespace, or <jk>null</jk> if it couldn't be found.
	 */
	public static Namespace findNamespace(List<Xml> xmls, List<XmlSchema> schemas) {

		for (Xml xml : xmls) {
			Namespace ns = findNamespace(xml.prefix(), xml.namespace(), xmls, schemas);
			if (ns != null)
				return ns;
		}

		for (XmlSchema schema : schemas) {
			Namespace ns = findNamespace(schema.prefix(), schema.namespace(), null, schemas);
			if (ns != null)
				return ns;
		}

		return null;
	}

	private static Namespace findNamespace(String prefix, String ns, List<Xml> xmls, List<XmlSchema> schemas) {

		// If both prefix and namespace specified, use that Namespace mapping.
		if (! (prefix.isEmpty() || ns.isEmpty()))
			return Namespace.create(prefix, ns);

		// If only prefix specified, need to search for namespaceURI.
		if (! prefix.isEmpty()) {
			if (xmls != null)
				for (Xml xml2 : xmls)
					if (xml2.prefix().equals(prefix) && ! xml2.namespace().isEmpty())
						return Namespace.create(prefix, xml2.namespace());
			for (XmlSchema schema : schemas) {
				if (schema.prefix().equals(prefix) && ! schema.namespace().isEmpty())
					return Namespace.create(prefix, schema.namespace());
				for (XmlNs xmlNs : schema.xmlNs())
					if (xmlNs.prefix().equals(prefix))
						return Namespace.create(prefix, xmlNs.namespaceURI());
			}
			throw new BeanRuntimeException("Found @Xml.prefix annotation with no matching URI.  prefix='"+prefix+"'");
		}

		// If only namespaceURI specified, need to search for prefix.
		if (! ns.isEmpty()) {
			if (xmls != null)
				for (Xml xml2 : xmls)
					if (xml2.namespace().equals(ns) && ! xml2.prefix().isEmpty())
						return Namespace.create(xml2.prefix(), ns);
			for (XmlSchema schema : schemas) {
				if (schema.namespace().equals(ns) && ! schema.prefix().isEmpty())
					return Namespace.create(schema.prefix(), ns);
				for (XmlNs xmlNs : schema.xmlNs())
					if (xmlNs.namespaceURI().equals(ns))
						return Namespace.create(xmlNs.prefix(), ns);
			}
		}

		return null;
	}

	/**
	 * Utility method that converts the current event on the XML stream to something human-readable for debug purposes.
	 * 
	 * @param r The XML stream reader whose current event is to be converted to a readable string.
	 * @return The event in human-readable form.
	 */
	public static final String toReadableEvent(XMLStreamReader r) {
		int t = r.getEventType();
		if (t == 1)
			return "<"+r.getLocalName()+">";
		if (t == 2)
			return "</"+r.getLocalName()+">";
		if (t == 3)
			return "PROCESSING_INSTRUCTION";
		if (t == 4)
			return "CHARACTERS=[" + r.getText() + "]";
		if (t == 5)
			return "COMMENTS=[" + r.getText() + "]";
		if (t == 6)
			return "SPACE=[" + r.getText() + "]";
		if (t == 7)
			return "START_DOCUMENT";
		if (t == 8)
			return "END_DOCUMENT";
		if (t == 9)
			return "ENTITY_REFERENCE";
		if (t == 10)
			return "ATTRIBUTE";
		if (t == 11)
			return "DTD";
		if (t == 12)
			return "CDATA=["+r.getText()+"]";
		if (t == 13)
			return "NAMESPACE";
		if (t == 14)
			return "NOTATION_DECLARATION";
		if (t == 15)
			return "ENTITY_DECLARATION";
		return "UNKNOWN";
	}

	/**
	 * Shortcut for calling <code>URLEncoder.<jsm>encode</jsm>(o.toString(), <js>"UTF-8"</js>)</code>.
	 * 
	 * @param o The object to encode.
	 * @return The URL encoded string, or <jk>null</jk> if the object was null.
	 */
	public static String urlEncode(Object o) {
		try {
			if (o != null)
				return URLEncoder.encode(o.toString(), "UTF-8");
		} catch (UnsupportedEncodingException e) {}
		return null;
	}

	/**
	 * Shortcut for calling <code>URLEncoder.<jsm>decode</jsm>(o.toString(), <js>"UTF-8"</js>)</code>.
	 * 
	 * @param s The string to decode.
	 * @return The decoded string, or <jk>null</jk> if the string was null.
	 */
	public static String urlDecode(String s) {
		try {
			if (s != null)
				return URLDecoder.decode(s, "UTF-8");
		} catch (UnsupportedEncodingException e) {}
		return null;
	}
}
