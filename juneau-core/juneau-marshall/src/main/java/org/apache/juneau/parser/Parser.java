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
package org.apache.juneau.parser;

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.json.*;
import org.apache.juneau.transform.*;
import org.apache.juneau.transforms.*;
import org.apache.juneau.utils.*;

/**
 * Parent class for all Juneau parsers.
 * 
 * <h6 class='topic'>Valid data conversions</h6>
 * 
 * Parsers can parse any parsable POJO types, as specified in the <a class="doclink"
 * href="../../../../overview-summary.html#juneau-marshall.PojoCategories">POJO Categories</a>.
 * 
 * <p>
 * Some examples of conversions are shown below...
 * </p>
 * <table class='styled'>
 * 	<tr>
 * 		<th>Data type</th>
 * 		<th>Class type</th>
 * 		<th>JSON example</th>
 * 		<th>XML example</th>
 * 		<th>Class examples</th>
 * 	</tr>
 * 	<tr>
 * 		<td>object</td>
 * 		<td>Maps, Java beans</td>
 * 		<td class='code'>{name:<js>'John Smith'</js>,age:21}</td>
 * 		<td class='code'><xt>&lt;object&gt;
 * 	&lt;name</xt> <xa>type</xa>=<xs>'string'</xs><xt>&gt;</xt>John Smith<xt>&lt;/name&gt;
 * 	&lt;age</xt> <xa>type</xa>=<xs>'number'</xs><xt>&gt;</xt>21<xt>&lt;/age&gt;
 * &lt;/object&gt;</xt></td>
 * 		<td class='code'>HashMap, TreeMap&lt;String,Integer&gt;</td>
 * 	</tr>
 * 	<tr>
 * 		<td>array</td>
 * 		<td>Collections, Java arrays</td>
 * 		<td class='code'>[1,2,3]</td>
 * 		<td class='code'><xt>&lt;array&gt;
 * 	&lt;number&gt;</xt>1<xt>&lt;/number&gt;
 * 	&lt;number&gt;</xt>2<xt>&lt;/number&gt;
 * 	&lt;number&gt;</xt>3<xt>&lt;/number&gt;
 * &lt;/array&gt;</xt></td>
 * 		<td class='code'>List&lt;Integer&gt;, <jk>int</jk>[], Float[], Set&lt;Person&gt;</td>
 * 	</tr>
 * 	<tr>
 * 		<td>number</td>
 * 		<td>Numbers</td>
 * 		<td class='code'>123</td>
 * 		<td class='code'><xt>&lt;number&gt;</xt>123<xt>&lt;/number&gt;</xt></td>
 * 		<td class='code'>Integer, Long, Float, <jk>int</jk></td>
 * 	</tr>
 * 	<tr>
 * 		<td>boolean</td>
 * 		<td>Booleans</td>
 * 		<td class='code'><jk>true</jk></td>
 * 		<td class='code'><xt>&lt;boolean&gt;</xt>true<xt>&lt;/boolean&gt;</xt></td>
 * 		<td class='code'>Boolean</td>
 * 	</tr>
 * 	<tr>
 * 		<td>string</td>
 * 		<td>CharSequences</td>
 * 		<td class='code'><js>'foobar'</js></td>
 * 		<td class='code'><xt>&lt;string&gt;</xt>foobar<xt>&lt;/string&gt;</xt></td>
 * 		<td class='code'>String, StringBuilder</td>
 * 	</tr>
 * </table>
 * 
 * <p>
 * In addition, any class types with {@link PojoSwap PojoSwaps} associated with them on the registered
 * bean context can also be passed in.
 * 
 * <p>
 * For example, if the {@link CalendarSwap} transform is used to generalize {@code Calendar} objects to {@code String}
 * objects.
 * When registered with this parser, you can construct {@code Calendar} objects from {@code Strings} using the
 * following syntax...
 * <p class='bcode'>
 * 	Calendar c = parser.parse(<js>"'Sun Mar 03 04:05:06 EST 2001'"</js>, GregorianCalendar.<jk>class</jk>);
 * </p>
 * 
 * <p>
 * If <code>Object.<jk>class</jk></code> is specified as the target type, then the parser automatically determines the
 * data types and generates the following object types...
 * <table class='styled'>
 * 	<tr><th>JSON type</th><th>Class type</th></tr>
 * 	<tr><td>object</td><td>{@link ObjectMap}</td></tr>
 * 	<tr><td>array</td><td>{@link ObjectList}</td></tr>
 * 	<tr><td>number</td><td>{@link Number}<br>(depending on length and format, could be {@link Integer},
 * 		{@link Double}, {@link Float}, etc...)</td></tr>
 * 	<tr><td>boolean</td><td>{@link Boolean}</td></tr>
 * 	<tr><td>string</td><td>{@link String}</td></tr>
 * </table>
 */
public abstract class Parser extends BeanContext {

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	private static final String PREFIX = "Parser.";

	/**
	 * Configuration property:  File charset.
	 * 
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Parser.fileCharset.s"</js>
	 * 	<li><b>Data type:</b>  <code>String</code>
	 * 	<li><b>Default:</b>  <js>"DEFAULT"</js>
	 * 	<li><b>Session-overridable:</b>  <jk>true</jk>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link ParserBuilder#fileCharset(String)}
	 * 			<li class='jm'>{@link ParserBuilder#fileCharset(Charset)}
	 * 		</ul>
	 * </ul>
	 * 
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * The character set to use for reading <code>Files</code> from the file system.
	 * 
	 * <p>
	 * Used when passing in files to {@link Parser#parse(Object, Class)}.
	 * 
	 * <p>
	 * <js>"DEFAULT"</js> can be used to indicate the JVM default file system charset.
	 * 
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Create a parser that reads UTF-8 files.</jc>
	 * 	ReaderParser p = JsonParser.
	 * 		.<jsm>create</jsm>()
	 * 		.fileCharset(<js>"UTF-8"</js>)
	 * 		.build();
	 * 	
	 * 	<jc>// Same, but use property.</jc>
	 * 	ReaderParser p = JsonParser.
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>PARSER_fileCharset</jsf>, <js>"UTF-8"</js>)
	 * 		.build();
	 * 
	 * 	<jc>// Use it to read a UTF-8 encoded file.</jc>
	 * 	MyBean myBean = p.parse(<jk>new</jk> File(<js>"MyBean.txt"</js>), MyBean.<jk>class</jk>);
	 * </p>
	 */
	public static final String PARSER_fileCharset = PREFIX + "fileCharset.s";

	/**
	 * Configuration property:  Input stream charset.
	 * 
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Parser.inputStreamCharset.s"</js>
	 * 	<li><b>Data type:</b>  <code>String</code>
	 * 	<li><b>Default:</b>  <js>"UTF-8"</js>
	 * 	<li><b>Session-overridable:</b>  <jk>true</jk>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link ParserBuilder#inputStreamCharset(String)}
	 * 			<li class='jm'>{@link ParserBuilder#inputStreamCharset(Charset)}
	 * 		</ul>
	 * </ul>
	 * 
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * The character set to use for converting <code>InputStreams</code> and byte arrays to readers.
	 * 
	 * <p>
	 * Used when passing in input streams and byte arrays to {@link Parser#parse(Object, Class)}.
	 * 
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Create a parser that reads UTF-8 files.</jc>
	 * 	ReaderParser p = JsonParser.
	 * 		.<jsm>create</jsm>()
	 * 		.inputStreamCharset(<js>"UTF-8"</js>)
	 * 		.build();
	 * 	
	 * 	<jc>// Same, but use property.</jc>
	 * 	ReaderParser p = JsonParser.
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>PARSER_inputStreamCharset</jsf>, <js>"UTF-8"</js>)
	 * 		.build();
	 * 
	 * 	<jc>// Use it to read a UTF-8 encoded input stream.</jc>
	 * 	MyBean myBean = p.parse(<jk>new</jk> FileInputStream(<js>"MyBean.txt"</js>), MyBean.<jk>class</jk>);
	 * </p>
	 */
	public static final String PARSER_inputStreamCharset = PREFIX + "inputStreamCharset.s";

	/**
	 * Configuration property:  Parser listener.
	 * 
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Parser.listener.c"</js>
	 * 	<li><b>Data type:</b>  <code>Class&lt;? extends ParserListener&gt;</code>
	 * 	<li><b>Default:</b>  <jk>null</jk>
	 * 	<li><b>Session-overridable:</b>  <jk>true</jk>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link ParserBuilder#listener(Class)}
	 * 		</ul>
	 * </ul>
	 * 
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Class used to listen for errors and warnings that occur during parsing.
	 * 
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Define our parser listener.</jc>
	 * 	<jc>// Simply captures all unknown bean property events.</jc>
	 * 	<jk>public class</jk> MyParserListener <jk>extends</jk> ParserListener {
	 * 
	 * 		<jc>// A simple property to store our events.</jc>
	 * 		<jk>public</jk> List&lt;String&gt; <jf>events</jf> = <jk>new</jk> LinkedList&lt;&gt;();
	 * 
	 * 		<ja>@Override</ja> 
	 * 		<jk>public</jk> &lt;T&gt; <jk>void</jk> onUnknownBeanProperty(ParserSession session, ParserPipe pipe, String propertyName, Class&lt;T&gt; beanClass, T bean, <jk>int</jk> line, <jk>int</jk> col) {
	 * 			<jf>events</jf>.add(propertyName + <js>","</js> + line + <js>","</js> + col);
	 * 		}
	 * 	}
	 * 
	 * 	<jc>// Create a parser using our listener.</jc>
	 * 	ReaderParser p = JsonParser.
	 * 		.<jsm>create</jsm>()
	 * 		.listener(MyParserListener.<jk>class</jk>)
	 * 		.build();
	 * 	
	 * 	<jc>// Same, but use property.</jc>
	 * 	ReaderParser p = JsonParser.
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>PARSER_listener</jsf>, MyParserListener.<jk>class</jk>)
	 * 		.build();
	 * 
	 * 	<jc>// Create a session object.</jc>
	 * 	<jc>// Needed because listeners are created per-session.</jc>
	 * 	<jk>try</jk> (ReaderParserSession s = p.createSession()) {
	 * 		
	 * 		<jc>// Parse some JSON object.</jc>
	 * 		MyBean myBean = s.parse(<js>"{...}"</js>, MyBean.<jk>class</jk>);
	 * 
	 * 		<jc>// Get the listener.</jc>
	 * 		MyParserListener l = s.getListener(MyParserListener.<jk>class</jk>);
	 * 
	 * 		<jc>// Dump the results to the console.</jc>
	 * 		JsonSerializer.<jsf>DEFAULT_LAX</jsf>.println(l.<jf>events</jf>);
	 * 	}
	 * </p>
	 */
	public static final String PARSER_listener = PREFIX + "listener.c";

	/**
	 * Configuration property:  Strict mode.
	 * 
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Parser.strict.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session-overridable:</b>  <jk>true</jk>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link ParserBuilder#strict(boolean)}
	 * 			<li class='jm'>{@link ParserBuilder#strict()}
	 * 		</ul>
	 * </ul>
	 * 
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * If <jk>true</jk>, strict mode for the parser is enabled.
	 * 
	 * <p>
	 * Strict mode can mean different things for different parsers.
	 * 
	 * <table class='styled'>
	 * 	<tr><th>Parser class</th><th>Strict behavior</th></tr>
	 * 	<tr>
	 * 		<td>All reader-based parsers</td>
	 * 		<td>
	 * 			When enabled, throws {@link ParseException ParseExceptions} on malformed charset input.
	 * 			Otherwise, malformed input is ignored.
	 * 		</td>
	 * 	</tr>
	 * 	<tr>
	 * 		<td>{@link JsonParser}</td>
	 * 		<td>
	 * 			When enabled, throws exceptions on the following invalid JSON syntax:
	 * 			<ul>
	 * 				<li>Unquoted attributes.
	 * 				<li>Missing attribute values.
	 * 				<li>Concatenated strings.
	 * 				<li>Javascript comments.
	 * 				<li>Numbers and booleans when Strings are expected.
	 * 				<li>Numbers valid in Java but not JSON (e.g. octal notation, etc...)
	 * 			</ul>
	 * 		</td>
	 * 	</tr>
	 * </table>
	 * 
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Create a parser using strict mode.</jc>
	 * 	ReaderParser p = JsonParser.
	 * 		.<jsm>create</jsm>()
	 * 		.strict()
	 * 		.build();
	 * 	
	 * 	<jc>// Same, but use property.</jc>
	 * 	ReaderParser p = JsonParser.
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>PARSER_strict</jsf>, <jk>true</jk>)
	 * 		.build();
	 * 
	 * 	<jc>// Use it.</jc>
	 *  	<jk>try</jk> {
	 *  		String json = <js>"{unquotedAttr:'value'}"</js>;
	 * 		MyBean myBean = p.parse(json, MyBean.<jk>class</jk>);
	 *  	} <jk>catch</jk> (ParseException e) {
	 *  		<jsm>assertTrue</jsm>(e.getMessage().contains(<js>"Unquoted attribute detected."</js>);
	 *  	}
	 * </p>
	 */
	public static final String PARSER_strict = PREFIX + "strict.b";

	/**
	 * Configuration property:  Trim parsed strings.
	 * 
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Parser.trimStrings.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session-overridable:</b>  <jk>true</jk>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link ParserBuilder#trimStrings(boolean)}
	 * 			<li class='jm'>{@link ParserBuilder#trimStrings()}
	 * 		</ul>
	 * </ul>
	 * 
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * If <jk>true</jk>, string values will be trimmed of whitespace using {@link String#trim()} before being added to
	 * the POJO.
	 * 
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Create a parser with trim-strings enabled.</jc>
	 * 	ReaderParser p = JsonParser.
	 * 		.<jsm>create</jsm>()
	 * 		.trimStrings()
	 * 		.build();
	 * 	
	 * 	<jc>// Same, but use property.</jc>
	 * 	ReaderParser p = JsonParser.
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>PARSER_trimStrings</jsf>, <jk>true</jk>)
	 * 		.build();
	 * 
	 * 	<jc>// Use it.</jc>
	 *  	String json = <js>"{foo:' bar '}"</js>;
	 * 	Map&lt;String,String&gt; m = p.parse(json, HashMap.<jk>class</jk>, String.<jk>class</jk>, String.<jk>class</jk>);
	 * 	<jsm>assertEquals</jsm>(<js>"bar"</js>, m.get(<js>"foo"</js>));
	 * </p>
	 */
	public static final String PARSER_trimStrings = PREFIX + "trimStrings.b";

	static Parser DEFAULT = new Parser(PropertyStore.create().build()) {
		@Override
		public ParserSession createSession(ParserSessionArgs args) {
			throw new NoSuchMethodError();
		}
	};

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	final boolean trimStrings, strict;
	final String inputStreamCharset, fileCharset;
	final Class<? extends ParserListener> listener;

	/** General parser properties currently set on this parser. */
	private final MediaType[] consumes;

	// Hidden constructor to force subclass from InputStreamParser or ReaderParser.
	Parser(PropertyStore ps, String...consumes) {
		super(ps);

		trimStrings = getProperty(PARSER_trimStrings, boolean.class, false);
		strict = getProperty(PARSER_strict, boolean.class, false);
		inputStreamCharset = getProperty(PARSER_inputStreamCharset, String.class, "UTF-8");
		fileCharset = getProperty(PARSER_fileCharset, String.class, "DEFAULT");
		listener = getClassProperty(PARSER_listener, ParserListener.class, null);
		this.consumes = new MediaType[consumes.length];
		for (int i = 0; i < consumes.length; i++) {
			this.consumes[i] = MediaType.forString(consumes[i]);
		}
	}

	@Override /* Context */
	public ParserBuilder builder() {
		return new ParserBuilder(getPropertyStore());
	}

	/**
	 * Instantiates a new clean-slate {@link ParserBuilder} object.
	 * 
	 * <p>
	 * This is equivalent to simply calling <code><jk>new</jk> ParserBuilder()</code>.
	 * 
	 * <p>
	 * Note that this method creates a builder initialized to all default settings, whereas {@link #builder()} copies 
	 * the settings of the object called on.
	 * 
	 * @return A new {@link ParserBuilder} object.
	 */
	public static ParserBuilder create() {
		return new ParserBuilder(PropertyStore.DEFAULT);
	}


	//--------------------------------------------------------------------------------
	// Abstract methods
	//--------------------------------------------------------------------------------

	/**
	 * Returns <jk>true</jk> if this parser subclasses from {@link ReaderParser}.
	 * 
	 * @return <jk>true</jk> if this parser subclasses from {@link ReaderParser}.
	 */
	public boolean isReaderParser() {
		return true;
	}

	/**
	 * Create the session object that will be passed in to the parse method.
	 * 
	 * <p>
	 * It's up to implementers to decide what the session object looks like, although typically it's going to be a
	 * subclass of {@link ParserSession}.
	 * 
	 * @param args
	 * 	Runtime arguments.
	 * @return The new session.
	 */
	public abstract ParserSession createSession(ParserSessionArgs args);


	//--------------------------------------------------------------------------------
	// Other methods
	//--------------------------------------------------------------------------------

	/**
	 * Parses input into the specified object type.
	 * 
	 * <p>
	 * The type can be a simple type (e.g. beans, strings, numbers) or parameterized type (collections/maps).
	 * 
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode'>
	 * 	ReaderParser p = JsonParser.<jsf>DEFAULT</jsf>;
	 * 
	 * 	<jc>// Parse into a linked-list of strings.</jc>
	 * 	List l = p.parse(json, LinkedList.<jk>class</jk>, String.<jk>class</jk>);
	 * 
	 * 	<jc>// Parse into a linked-list of beans.</jc>
	 * 	List l = p.parse(json, LinkedList.<jk>class</jk>, MyBean.<jk>class</jk>);
	 * 
	 * 	<jc>// Parse into a linked-list of linked-lists of strings.</jc>
	 * 	List l = p.parse(json, LinkedList.<jk>class</jk>, LinkedList.<jk>class</jk>, String.<jk>class</jk>);
	 * 
	 * 	<jc>// Parse into a map of string keys/values.</jc>
	 * 	Map m = p.parse(json, TreeMap.<jk>class</jk>, String.<jk>class</jk>, String.<jk>class</jk>);
	 * 
	 * 	<jc>// Parse into a map containing string keys and values of lists containing beans.</jc>
	 * 	Map m = p.parse(json, TreeMap.<jk>class</jk>, String.<jk>class</jk>, List.<jk>class</jk>, MyBean.<jk>class</jk>);
	 * </p>
	 * 
	 * <p>
	 * <code>Collection</code> classes are assumed to be followed by zero or one objects indicating the element type.
	 * 
	 * <p>
	 * <code>Map</code> classes are assumed to be followed by zero or two meta objects indicating the key and value types.
	 * 
	 * <p>
	 * The array can be arbitrarily long to indicate arbitrarily complex data structures.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>Use the {@link #parse(Object, Class)} method instead if you don't need a parameterized map/collection.
	 * </ul>
	 * 
	 * @param <T> The class type of the object to create.
	 * @param input
	 * 	The input.
	 * 	<br>Character-based parsers can handle the following input class types:
	 * 	<ul>
	 * 		<li><jk>null</jk>
	 * 		<li>{@link Reader}
	 * 		<li>{@link CharSequence}
	 * 		<li>{@link InputStream} containing UTF-8 encoded text (or charset defined by
	 * 			{@link #PARSER_inputStreamCharset} property value).
	 * 		<li><code><jk>byte</jk>[]</code> containing UTF-8 encoded text (or charset defined by
	 * 			{@link #PARSER_inputStreamCharset} property value).
	 * 		<li>{@link File} containing system encoded text (or charset defined by
	 * 			{@link #PARSER_fileCharset} property value).
	 * 	</ul>
	 * 	<br>Stream-based parsers can handle the following input class types:
	 * 	<ul>
	 * 		<li><jk>null</jk>
	 * 		<li>{@link InputStream}
	 * 		<li><code><jk>byte</jk>[]</code>
	 * 		<li>{@link File}
	 * 	</ul>
	 * @param type
	 * 	The object type to create.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType},
	 * 	{@link GenericArrayType}
	 * @param args
	 * 	The type arguments of the class if it's a collection or map.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType},
	 * 	{@link GenericArrayType}
	 * 	<br>Ignored if the main type is not a map or collection.
	 * @return The parsed object.
	 * @throws ParseException
	 * 	If the input contains a syntax error or is malformed, or is not valid for the specified type.
	 * @see BeanSession#getClassMeta(Type,Type...) for argument syntax for maps and collections.
	 */
	public final <T> T parse(Object input, Type type, Type...args) throws ParseException {
		return createSession().parse(input, type, args);
	}

	/**
	 * Same as {@link #parse(Object, Type, Type...)} except optimized for a non-parameterized class.
	 * 
	 * <p>
	 * This is the preferred parse method for simple types since you don't need to cast the results.
	 * 
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode'>
	 * 	ReaderParser p = JsonParser.<jsf>DEFAULT</jsf>;
	 * 
	 * 	<jc>// Parse into a string.</jc>
	 * 	String s = p.parse(json, String.<jk>class</jk>);
	 * 
	 * 	<jc>// Parse into a bean.</jc>
	 * 	MyBean b = p.parse(json, MyBean.<jk>class</jk>);
	 * 
	 * 	<jc>// Parse into a bean array.</jc>
	 * 	MyBean[] ba = p.parse(json, MyBean[].<jk>class</jk>);
	 * 
	 * 	<jc>// Parse into a linked-list of objects.</jc>
	 * 	List l = p.parse(json, LinkedList.<jk>class</jk>);
	 * 
	 * 	<jc>// Parse into a map of object keys/values.</jc>
	 * 	Map m = p.parse(json, TreeMap.<jk>class</jk>);
	 * </p>
	 * 
	 * @param <T> The class type of the object being created.
	 * @param input
	 * 	The input.
	 * 	See {@link #parse(Object, Type, Type...)} for details.
	 * @param type The object type to create.
	 * @return The parsed object.
	 * @throws ParseException
	 * 	If the input contains a syntax error or is malformed, or is not valid for the specified type.
	 */
	public final <T> T parse(Object input, Class<T> type) throws ParseException {
		return createSession().parse(input, type);
	}

	/**
	 * Same as {@link #parse(Object, Type, Type...)} except the type has already been converted into a {@link ClassMeta}
	 * object.
	 * 
	 * <p>
	 * This is mostly an internal method used by the framework.
	 * 
	 * @param <T> The class type of the object being created.
	 * @param input
	 * 	The input.
	 * 	See {@link #parse(Object, Type, Type...)} for details.
	 * @param type The object type to create.
	 * @return The parsed object.
	 * @throws ParseException
	 * 	If the input contains a syntax error or is malformed, or is not valid for the specified type.
	 */
	public final <T> T parse(Object input, ClassMeta<T> type) throws ParseException {
		return createSession().parse(input, type);
	}

	@Override /* Context */
	public final ParserSession createSession() {
		return createSession(createDefaultSessionArgs());
	}

	@Override /* Context */
	public final ParserSessionArgs createDefaultSessionArgs() {
		return new ParserSessionArgs(ObjectMap.EMPTY_MAP, null, null, null, getPrimaryMediaType(), null);
	}

	//--------------------------------------------------------------------------------
	// Optional methods
	//--------------------------------------------------------------------------------

	/**
	 * Parses the contents of the specified reader and loads the results into the specified map.
	 * 
	 * <p>
	 * Reader must contain something that serializes to a map (such as text containing a JSON object).
	 * 
	 * <p>
	 * Used in the following locations:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The various character-based constructors in {@link ObjectMap} (e.g.
	 * 		{@link ObjectMap#ObjectMap(CharSequence,Parser)}).
	 * </ul>
	 * 
	 * @param <K> The key class type.
	 * @param <V> The value class type.
	 * @param input The input.  See {@link #parse(Object, ClassMeta)} for supported input types.
	 * @param m The map being loaded.
	 * @param keyType The class type of the keys, or <jk>null</jk> to default to <code>String.<jk>class</jk></code>.
	 * @param valueType The class type of the values, or <jk>null</jk> to default to whatever is being parsed.
	 * @return The same map that was passed in to allow this method to be chained.
	 * @throws ParseException If the input contains a syntax error or is malformed, or is not valid for the specified type.
	 * @throws UnsupportedOperationException If not implemented.
	 */
	public final <K,V> Map<K,V> parseIntoMap(Object input, Map<K,V> m, Type keyType, Type valueType) throws ParseException {
		return createSession().parseIntoMap(input, m, keyType, valueType);
	}

	/**
	 * Parses the contents of the specified reader and loads the results into the specified collection.
	 * 
	 * <p>
	 * Used in the following locations:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		The various character-based constructors in {@link ObjectList} (e.g.
	 * 		{@link ObjectList#ObjectList(CharSequence,Parser)}.
	 * </ul>
	 * 
	 * @param <E> The element class type.
	 * @param input The input.  See {@link #parse(Object, ClassMeta)} for supported input types.
	 * @param c The collection being loaded.
	 * @param elementType The class type of the elements, or <jk>null</jk> to default to whatever is being parsed.
	 * @return The same collection that was passed in to allow this method to be chained.
	 * @throws ParseException
	 * 	If the input contains a syntax error or is malformed, or is not valid for the specified type.
	 * @throws UnsupportedOperationException If not implemented.
	 */
	public final <E> Collection<E> parseIntoCollection(Object input, Collection<E> c, Type elementType) throws ParseException {
		return createSession().parseIntoCollection(input, c, elementType);
	}

	/**
	 * Parses the specified array input with each entry in the object defined by the {@code argTypes}
	 * argument.
	 * 
	 * <p>
	 * Used for converting arrays (e.g. <js>"[arg1,arg2,...]"</js>) into an {@code Object[]} that can be passed
	 * to the {@code Method.invoke(target, args)} method.
	 * 
	 * <p>
	 * Used in the following locations:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Used to parse argument strings in the {@link PojoIntrospector#invokeMethod(Method, Reader)} method.
	 * </ul>
	 * 
	 * @param input The input.  Subclasses can support different input types.
	 * @param argTypes Specifies the type of objects to create for each entry in the array.
	 * @return An array of parsed objects.
	 * @throws ParseException
	 * 	If the input contains a syntax error or is malformed, or is not valid for the specified type.
	 */
	public final Object[] parseArgs(Object input, Type[] argTypes) throws ParseException {
		if (argTypes == null || argTypes.length == 0)
			return new Object[0];
		return createSession().parseArgs(input, argTypes);
	}


	//--------------------------------------------------------------------------------
	// Other methods
	//--------------------------------------------------------------------------------

	/**
	 * Returns the media types handled based on the values passed to the <code>consumes</code> constructor parameter.
	 * 
	 * @return The list of media types.  Never <jk>null</jk>.
	 */
	public final MediaType[] getMediaTypes() {
		return consumes;
	}

	/**
	 * Returns the first media type handled based on the values passed to the <code>consumes</code> constructor parameter.
	 * 
	 * @return The media type.
	 */
	public final MediaType getPrimaryMediaType() {
		return consumes == null || consumes.length == 0 ? null : consumes[0];
	}

	@Override /* Context */
	public ObjectMap asMap() {
		return super.asMap()
			.append("Parser", new ObjectMap()
				.append("trimStrings", trimStrings)
				.append("strict", strict)
				.append("inputStreamCharset", inputStreamCharset)
				.append("fileCharset", fileCharset)
				.append("listener", listener)
			);
	}
}
