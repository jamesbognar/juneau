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
package org.apache.juneau.rest.annotation;

import org.apache.juneau.*;
import org.apache.juneau.html.*;
import org.apache.juneau.rest.*;

/**
 * Contains all the configurable annotations for the {@link HtmlDocSerializer}.
 * <p>
 * Used with {@link RestResource#htmldoc()} and {@link RestMethod#htmldoc()} to customize the HTML view of
 * 	serialized POJOs.
 * <p>
 * All annotations specified here have no effect on any serializers other than {@link HtmlDocSerializer} and is
 * 	provided as a shorthand method of for specifying configuration properties.
 * <p>
 * For example, the following two methods for defining the HTML document title are considered equivalent:
 * <p class='bcode'>
 * 	<ja>@RestResource</ja>(
 * 		properties={
 * 			<ja>@Property</ja>(name=<jsf>HTMLDOC_title</jsf>, value=<js>"My Resource Page"</js>)
 * 		}
 * 	)
 *
 * 	<ja>@RestResource</ja>(
 * 		htmldoc=<ja>@HtmlDoc</ja>(
 * 			title=<js>"My Resource Page"</js>
 * 		)
 * 	)
 * </p>
 * <p>
 * The purpose of these annotation is to populate the HTML document view which by default consists of the following
 * 	structure:
 * <p class='bcode'>
 * 	<xt>&lt;html&gt;
 * 		&lt;head&gt;
 * 			&lt;style <xa>type</xa>=<xs>'text/css'</xs>&gt;
 * 				<xv>CSS styles and links to stylesheets</xv>
 * 			&lt/style&gt;
 * 		&lt;/head&gt;
 * 		&lt;body&gt;
 * 			&lt;header&gt;
 * 				<xv>Page title and description</xv>
 * 			&lt;/header&gt;
 * 			&lt;nav&gt;
 * 				<xv>Page links</xv>
 * 			&lt;/nav&gt;
 * 			&lt;aside&gt;
 * 				<xv>Side-bar page links</xv>
 * 			&lt;/aside&gt;
 * 			&lt;article&gt;
 * 				<xv>Contents of serialized object</xv>
 * 			&lt;/article&gt;
 * 			&lt;footer&gt;
 * 				<xv>Footer message</xv>
 * 			&lt;/footer&gt;
 * 		&lt;/body&gt;
 * 	&lt;/html&gt;</xt>
 * </p>
 */
public @interface HtmlDoc {

	/**
	 * Sets the HTML page title.
	 * <p>
	 * The format of this value is plain text.
	 * <p>
	 * It gets wrapped in a <code><xt>&lt;h3&gt; <xa>class</xa>=<xs>'title'</xs>&gt;</xt></code> element and then added
	 * 	to the <code><xt>&lt;header&gt;</code> section on the page.
	 * <p>
	 * If not specified, the page title is pulled from one of the following locations:
	 * <ol>
	 * 	<li><code>{servletClass}.{methodName}.pageTitle</code> resource bundle value.
	 * 	<li><code>{servletClass}.pageTitle</code> resource bundle value.
	 * 	<li><code><ja>@RestResource</ja>(title)</code> annotation.
	 * 	<li><code>{servletClass}.title</code> resource bundle value.
	 * 	<li><code>info/title</code> entry in swagger file.
	 * <ol>
	 * <p>
	 * This field can contain variables (e.g. <js>"$L{my.localized.variable}"</js>).
	 * <p>
	 * A value of <js>"NONE"</js> can be used to force no value.
	 * <p>
	 * <ul class='doctree'>
	 * 	<li class='info'>
	 * 		In most cases, you'll simply want to use the <code>@RestResource(title)</code> annotation to specify the
	 * 		page title.
	 * 		However, this annotation is provided in cases where you want the page title to be different that the one
	 * 		shown in the swagger document.
	 * </ul>
	 * <p>
	 * The programmatic equivalent to this annotation are the {@link RestConfig#setHtmlTitle(String)}/{@link RestResponse#setHtmlTitle(Object)} methods.
	 */
	String title() default "";

	/**
	 * Sets the HTML page description.
	 * <p>
	 * The format of this value is plain text.
	 * <p>
	 * It gets wrapped in a <code><xt>&lt;h5&gt; <xa>class</xa>=<xs>'description'</xs>&gt;</xt></code> element and then
	 * 	added to the <code><xt>&lt;header&gt;</code> section on the page.
	 * <p>
	 * If not specified, the page title is pulled from one of the following locations:
	 * <ol>
	 * 	<li><code>{servletClass}.{methodName}.pageText</code> resource bundle value.
	 * 	<li><code>{servletClass}.pageText</code> resource bundle value.
	 * 	<li><code><ja>@RestMethod</ja>(summary)</code> annotation.
	 * 	<li><code>{servletClass}.{methodName}.summary</code> resource bundle value.
	 * 	<li><code>summary</code> entry in swagger file for method.
	 * 	<li><code>{servletClass}.description</code> resource bundle value.
	 * 	<li><code>info/description</code> entry in swagger file.
	 * <ol>
	 * <p>
	 * This field can contain variables (e.g. <js>"$L{my.localized.variable}"</js>).
	 * <p>
	 * A value of <js>"NONE"</js> can be used to force no value.
	 * <p>
	 * <ul class='doctree'>
	 * 	<li class='info'>
	 * 		In most cases, you'll simply want to use the <code>@RestResource(description)</code> or
	 * 		<code>@RestMethod(summary)</code> annotations to specify the page text.
	 * 		However, this annotation is provided in cases where you want the text to be different that the values shown
	 * 		in the swagger document.
	 * </ul>
	 * <p>
	 * The programmatic equivalent to this annotation are the {@link RestConfig#setHtmlDescription(String)}/{@link RestResponse#setHtmlDescription(Object)} methods.
	 */
	String description() default "";

	/**
	 * Sets the HTML header section contents.
	 * <p>
	 * The format of this value is HTML.
	 * <p>
	 * The page header normally contains the title and description, but this value can be used to override the contents
	 * 	to be whatever you want.
	 * <p>
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<ja>@RestResource</ja>(
	 * 		htmldoc=<ja>@HtmlDoc</ja>(
	 * 			header=<js>"&lt;p&gtThis is my REST interface&lt;/p&gt;"</js>
	 * 		)
	 * 	)
	 * </p>
	 * <p>
	 * When a value is specified, the {@link #title()} and {@link #description()} values will be ignored.
	 * <p>
	 * A value of <js>"NONE"</js> can be used to force no header.
	 * <p>
	 * This field can contain variables (e.g. <js>"$L{my.localized.variable}"</js>).
	 * <p>
	 * The programmatic equivalent to this annotation are the {@link RestConfig#setHtmlHeader(String)}/{@link RestResponse#setHtmlHeader(Object)} methods.
	 */
	String header() default "";

	/**
	 * Sets the links in the HTML nav section.
	 * <p>
	 * The format of this value is a lax-JSON map of key/value pairs where the keys are the link text and the values are
	 * 	relative (to the servlet) or absolute URLs.
	 * <p>
	 * The page links are positioned immediately under the title and text.
	 * <p>
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<ja>@RestResource</ja>(
	 * 		htmldoc=<ja>@HtmlDoc</ja>(
	 * 			links=<js>"{up:'request:/..',options:'servlet:/?method=OPTIONS'}"</js>
	 * 		)
	 * 	)
	 * </p>
	 * <p>
	 * This field can contain variables (e.g. <js>"$L{my.localized.variable}"</js>).
	 * <p>
	 * A value of <js>"NONE"</js> can be used to force no value.
	 * <p>
	 * This field can also use URIs of any support type in {@link UriResolver}.
	 * <p>
	 * The programmatic equivalent to this annotation are the {@link RestConfig#setHtmlLinks(String)}/{@link RestResponse#setHtmlLinks(Object)} methods.
	 */
	String links() default "";

	/**
	 * Sets the HTML nav section contents.
	 * <p>
	 * The format of this value is HTML.
	 * <p>
	 * The nav section of the page contains the links.
	 * <p>
	 * The format of this value is HTML.
	 * <p>
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<ja>@RestResource</ja>(
	 * 		htmldoc=<ja>@HtmlDoc</ja>(
	 * 			nav=<js>"&lt;p&gt;Custom nav content&lt;/p&gt;"</js>
	 * 		)
	 * 	)
	 * </p>
	 * <p>
	 * When a value is specified, the {@link #links()} value will be ignored.
	 * <p>
	 * This field can contain variables (e.g. <js>"$L{my.localized.variable}"</js>).
	 * <p>
	 * A value of <js>"NONE"</js> can be used to force no value.
	 * <p>
	 * The programmatic equivalent to this annotation are the {@link RestConfig#setHtmlNav(String)}/{@link RestResponse#setHtmlNav(Object)} methods.
	 */
	String nav() default "";

	/**
	 * Sets the HTML aside section contents.
	 * <p>
	 * The format of this value is HTML.
	 * <p>
	 * The aside section typically floats on the right side of the page.
	 * <p>
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<ja>@RestResource</ja>(
	 * 		htmldoc=<ja>@HtmlDoc</ja>(
	 * 			aside=<js>"&lt;p&gt;Custom aside content&lt;/p&gt;"</js>
	 * 		)
	 * 	)
	 * </p>
	 * <p>
	 * This field can contain variables (e.g. <js>"$L{my.localized.variable}"</js>).
	 * <p>
	 * A value of <js>"NONE"</js> can be used to force no value.
	 * <p>
	 * The programmatic equivalent to this annotation are the {@link RestConfig#setHtmlAside(String)}/{@link RestResponse#setHtmlAside(Object)} methods.
	 */
	String aside() default "";

	/**
	 * Sets the HTML footer section contents.
	 * <p>
	 * The format of this value is HTML.
	 * <p>
	 * The footer section typically floats on the bottom of the page.
	 * <p>
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<ja>@RestResource</ja>(
	 * 		htmldoc=<ja>@HtmlDoc</ja>(
	 * 			footer=<js>"&lt;p&gt;Custom footer content&lt;/p&gt;"</js>
	 * 		)
	 * 	)
	 * </p>
	 * <p>
	 * This field can contain variables (e.g. <js>"$L{my.localized.variable}"</js>).
	 * <p>
	 * A value of <js>"NONE"</js> can be used to force no value.
	 * <p>
	 * The programmatic equivalent to this annotation are the {@link RestConfig#setHtmlFooter(String)}/{@link RestResponse#setHtmlFooter(Object)} methods.
	 */
	String footer() default "";

	/**
	 * Sets the HTML CSS style section contents.
	 * <p>
	 * The format of this value is CSS.
	 * <p>
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<ja>@RestResource</ja>(
	 * 		htmldoc=<ja>@HtmlDoc</ja>(
	 * 			css=<js>".red{color:red;}\n.blue{color:blue}"</js>
	 * 		)
	 * 	)
	 * </p>
	 * <p>
	 * This field can contain variables (e.g. <js>"$L{my.localized.variable}"</js>).
	 * <p>
	 * A value of <js>"NONE"</js> can be used to force no value.
	 * <p>
	 * The programmatic equivalent to this annotation are the {@link RestConfig#setHtmlCss(String)}/{@link RestResponse#setHtmlCss(Object)} methods.
	 */
	String css() default "";

	/**
	 * Sets the CSS URL in the HTML CSS style section.
	 * <p>
	 * The format of this value is a URL.
	 * <p>
	 * Specifies the URL to the stylesheet to add as a link in the style tag in the header.
	 * <p>
	 * The format of this value is CSS.
	 * <p>
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<ja>@RestResource</ja>(
	 * 		htmldoc=<ja>@HtmlDoc</ja>(
	 * 			cssUrl=<js>"http://someOtherHost/stealTheir.css"</js>
	 * 		)
	 * 	)
	 * </p>
	 * <p>
	 * This field can contain variables (e.g. <js>"$L{my.localized.variable}"</js>) and can use URL protocols defined
	 * 	by {@link UriResolver}.
	 * <p>
	 * The programmatic equivalent to this annotation are the {@link RestConfig#setHtmlCssUrl(String)}/{@link RestResponse#setHtmlCssUrl(Object)} methods.
	 */
	String cssUrl() default "servlet:/style.css";

	/**
	 * Shorthand method for forcing the rendered HTML content to be no-wrap.
	 */
	boolean nowrap() default false;

	/**
	 * Specifies the text to display when serializing an empty array or collection.
	 */
	String noResultsMessage() default "no results";

	/**
	 * Specifies the template class to use for rendering the HTML page.
	 * <p>
	 * By default, uses {@link HtmlDocTemplateBasic} to render the contents, although you can provide
	 * 	 your own custom renderer or subclasses from the basic class to have full control over how the page is
	 * 	rendered.
	 * <p>
	 * The programmatic equivalent to this annotation are the {@link RestConfig#setHtmlTemplate(Class)}/{@link RestResponse#setHtmlTemplate(Class)} methods.
	 */
	Class<? extends HtmlDocTemplate> template() default HtmlDocTemplate.class;
}