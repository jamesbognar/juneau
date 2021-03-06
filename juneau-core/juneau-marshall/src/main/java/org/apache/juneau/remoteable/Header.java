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
package org.apache.juneau.remoteable;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import org.apache.juneau.httppart.*;
import org.apache.juneau.urlencoding.*;

/**
 * Annotation applied to Java method arguments of interface proxies to denote that they are serialized as an HTTP
 * header value.
 * 
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<ja>@Remoteable</ja>(path=<js>"/myproxy"</js>)
 * 	<jk>public interface</jk> MyProxy {
 * 
 * 		<jc>// Explicit names specified for HTTP headers.</jc>
 * 		<jc>// pojo will be converted to UON notation (unless plain-text parts enabled).</jc>
 * 		<ja>@RemoteMethod</ja>(path=<js>"/mymethod1"</js>)
 * 		String myProxyMethod1(<ja>@Header</ja>(<js>"Foo"</js>)</ja> String foo,
 * 			<ja>@Header</ja>(<js>"Bar"</js>)</ja> MyPojo pojo);
 * 
 * 		<jc>// Multiple values pulled from a NameValuePairs object.</jc>
 * 		<jc>// Same as @Header("*").</jc>
 * 		<ja>@RemoteMethod</ja>(path=<js>"/mymethod2"</js>)
 * 		String myProxyMethod2(<ja>@Header</ja> NameValuePairs nameValuePairs);
 * 
 * 		<jc>// Multiple values pulled from a Map.</jc>
 * 		<jc>// Same as @Header("*").</jc>
 * 		<ja>@RemoteMethod</ja>(path=<js>"/mymethod3"</js>)
 * 		String myProxyMethod3(<ja>@Header</ja> Map&lt;String,Object&gt; map);
 * 
 * 		<jc>// Multiple values pulled from a bean.</jc>
 * 		<jc>// Same as @Header("*").</jc>
 * 		<ja>@RemoteMethod</ja>(path=<js>"/mymethod4"</js>)
 * 		String myProxyMethod4(<ja>@Header</ja> MyBean myBean);
 * 	}
 * </p>
 * 
 * <p>
 * The annotation can also be applied to a bean property field or getter when the argument is annotated with
 * {@link RequestBean @RequestBean}:
 * 
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<ja>@Remoteable</ja>(path=<js>"/myproxy"</js>)
 * 	<jk>public interface</jk> MyProxy {
 * 
 * 		<ja>@RemoteMethod</ja>(path=<js>"/mymethod"</js>)
 * 		String myProxyMethod(<ja>@RequestBean</ja> MyRequestBean bean);
 * 	}
 * 
 * 	<jk>public interface</jk> MyRequestBean {
 * 
 * 		<jc>// Name explicitly specified.</jc>
 * 		<ja>@Header</ja>(<js>"Foo"</js>)
 * 		String getX();
 * 
 * 		<jc>// Name inherited from bean property.</jc>
 * 		<jc>// Same as @Header("bar")</jc>
 * 		<ja>@Header</ja>
 * 		String getBar();
 * 
 * 		<jc>// Name inherited from bean property.</jc>
 * 		<jc>// Same as @Header("Baz")</jc>
 * 		<ja>@Header</ja>
 * 		<ja>@BeanProperty</ja>(<js>"Baz"</js>)
 * 		String getY();
 * 
 * 		<jc>// Multiple values pulled from NameValuePairs object.</jc>
 * 		<jc>// Same as @Header("*")</jc>
 * 		<ja>@Header</ja>
 * 		NameValuePairs getNameValuePairs();
 * 
 * 		<jc>// Multiple values pulled from Map.</jc>
 * 		<jc>// Same as @Header("*")</jc>
 * 		<ja>@Header</ja>
 * 	 	Map&lt;String,Object&gt; getMap();
 * 
 * 		<jc>// Multiple values pulled from bean.</jc>
 * 		<jc>// Same as @Header("*")</jc>
 * 		<ja>@Header</ja>
 * 	 	MyBean getBean();
 * 	}
 * </p>
 * 
 * <p>
 * The {@link #name()} and {@link #value()} elements are synonyms for specifying the header name.
 * Only one should be used.
 * <br>The following annotations are fully equivalent:
 * <p class='bcode'>
 * 	<ja>@Header</ja>(name=<js>"Foo"</js>)
 * 
 * 	<ja>@Header</ja>(<js>"Foo"</js>)
 * </p>
 * 
 * <h6 class='topic'>Additional Information</h6>
 * <ul class='doctree'>
 * 	<li class='link'>
 * 		<a class='doclink' href='../../../../overview-summary.html#Remoteable.3rdParty'>Interface proxies against 3rd-party REST interfaces</a>
 * 	<li class='jp'>
 * 		<a class='doclink' href='package-summary.html#TOC'>org.apache.juneau.remoteable</a>
 * </ul>
 */
@Documented
@Target({PARAMETER,FIELD,METHOD})
@Retention(RUNTIME)
@Inherited
public @interface Header {

	/**
	 * The HTTP header name.
	 * 
	 * <p>
	 * A blank value (the default) indicates to reuse the bean property name when used on a request bean property.
	 * 
	 * <p>
	 * The value should be either <js>"*"</js> to represent multiple name/value pairs, or a label that defines the
	 * header name.
	 * 
	 * <p>
	 * A blank value (the default) has the following behavior:
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		If the data type is <code>NameValuePairs</code>, <code>Map</code>, or a bean,
	 * 		then it's the equivalent to <js>"*"</js> which will cause the value to be serialized as name/value pairs.
	 * 
	 * 		<h6 class='figure'>Example:</h6>
	 * 		<p class='bcode'>
	 * 	<jc>// When used on a remote method parameter</jc>
	 * 	<ja>@Remoteable</ja>(path=<js>"/myproxy"</js>)
	 * 	<jk>public interface</jk> MyProxy {
	 * 
	 * 		<jc>// Equivalent to @Header("*")</jc>
	 * 		<ja>@RemoteMethod</ja>(path=<js>"/mymethod"</js>)
	 * 		String myProxyMethod1(<ja>@Header</ja> Map&lt;String,Object&gt; headers);
	 * 	}
	 * 
	 * 	<jc>// When used on a request bean method</jc>
	 * 	<jk>public interface</jk> MyRequestBean {
	 * 
	 * 		<jc>// Equivalent to @Header("*")</jc>
	 * 		<ja>@Header</ja>
	 * 		Map&lt;String,Object&gt; getFoo();
	 * 	}
	 * 		</p>
	 * 	</li>
	 * 	<li>
	 * 		If used on a request bean method, uses the bean property name.
	 * 
	 * 		<h6 class='figure'>Example:</h6>
	 * 		<p class='bcode'>
	 * 	<jk>public interface</jk> MyRequestBean {
	 * 
	 * 		<jc>// Equivalent to @Header("Foo")</jc>
	 * 		<ja>@Header</ja>
	 * 		<ja>@BeanProperty</ja>(<js>"Foo"</js>)
	 * 		String getFoo();
	 * 	}
	 * 		</p>
	 * 	</li>
	 * </ul>
	 */
	String name() default "";

	/**
	 * A synonym for {@link #name()}.
	 * 
	 * <p>
	 * Allows you to use shortened notation if you're only specifying the name.
	 */
	String value() default "";

	/**
	 * Skips this value if it's an empty string or empty collection/array.
	 * 
	 * <p>
	 * Note that <jk>null</jk> values are already ignored.
	 */
	boolean skipIfEmpty() default false;

	/**
	 * Specifies the {@link HttpPartSerializer} class used for serializing values to strings.
	 * 
	 * <p>
	 * The default value defaults to the using the part serializer defined on the {@link RequestBean @RequestBean} annotation,
	 * then on the client which by default is {@link UrlEncodingSerializer}.
	 * 
	 * <p>
	 * This annotation is provided to allow values to be custom serialized.
	 */
	Class<? extends HttpPartSerializer> serializer() default HttpPartSerializer.Null.class;
}
