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
package org.apache.juneau.rest;

import static java.util.Collections.*;
import static java.util.logging.Level.*;
import static javax.servlet.http.HttpServletResponse.*;
import static org.apache.juneau.html.HtmlDocSerializer.*;
import static org.apache.juneau.internal.IOUtils.*;
import static org.apache.juneau.serializer.Serializer.*;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.charset.*;
import java.text.*;
import java.util.*;
import java.util.logging.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.juneau.*;
import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.http.*;
import org.apache.juneau.ini.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.widget.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.utils.*;

/**
 * Represents an HTTP request for a REST resource.
 * 
 * <p>
 * Equivalent to {@link HttpServletRequest} except with some additional convenience methods.
 * 
 * <p>
 * For reference, given the URL <js>"http://localhost:9080/contextRoot/servletPath/foo?bar=baz#qux"</js>, the
 * following methods return the following values....
 * <table class='styled'>
 * 	<tr><th>Method</th><th>Value</th></tr>
 * 	<tr><td>{@code getContextPath()}</td><td>{@code /contextRoot}</td></tr>
 * 	<tr><td>{@code getPathInfo()}</td><td>{@code /foo}</td></tr>
 * 	<tr><td>{@code getPathTranslated()}</td><td>{@code path-to-deployed-war-on-filesystem/foo}</td></tr>
 * 	<tr><td>{@code getQueryString()}</td><td>{@code bar=baz}</td></tr>
 * 	<tr><td>{@code getRequestURI()}</td><td>{@code /contextRoot/servletPath/foo}</td></tr>
 * 	<tr><td>{@code getRequestURL()}</td><td>{@code http://localhost:9080/contextRoot/servletPath/foo}</td></tr>
 * 	<tr><td>{@code getServletPath()}</td><td>{@code /servletPath}</td></tr>
 * </table>
 * 
 * <p>
 * Refer to <a class="doclink" href="package-summary.html#TOC">REST Servlet API</a> for information about using this
 * class.
 */
@SuppressWarnings({ "unchecked", "unused" })
public final class RestRequest extends HttpServletRequestWrapper {

	private final RestContext context;
	private RestJavaMethod restJavaMethod;

	private final String method;
	private RequestBody body;
	private Method javaMethod;
	private RestRequestProperties properties;
	private final boolean debug;
	private BeanSession beanSession;
	private VarResolverSession varSession;
	private final RequestQuery queryParams;
	private RequestFormData formData;
	private RequestPathMatch pathParams;
	private boolean isPost;
	private UriContext uriContext;
	private String charset;
	private RequestHeaders headers;
	private ConfigFile cf;
	private Swagger swagger;

	/**
	 * Constructor.
	 */
	RestRequest(RestContext context, HttpServletRequest req) throws ServletException {
		super(req);
		this.context = context;

		try {
			isPost = req.getMethod().equalsIgnoreCase("POST");

			// If this is a POST, we want to parse the query parameters ourselves to prevent
			// the servlet code from processing the HTTP body as URL-Encoded parameters.
			queryParams = new RequestQuery();
			if (isPost) 
				RestUtils.parseQuery(getQueryString(), queryParams);
			else
				queryParams.putAll(req.getParameterMap());


			// Get the HTTP method.
			// Can be overridden through a "method" GET attribute.
			String _method = super.getMethod();

			String m = getQuery().getString("method");
			if (context.allowMethodParam(m))
				_method = m;

			method = _method;

			headers = new RequestHeaders();
			for (Enumeration<String> e = getHeaderNames(); e.hasMoreElements();) {
				String name = e.nextElement();
				headers.put(name, super.getHeaders(name));
			}

			body = new RequestBody(this);

			if (context.isAllowBodyParam()) {
				String b = getQuery().getString("body");
				if (b != null) {
					headers.put("Content-Type", UonSerializer.DEFAULT.getResponseContentType());
					body.load(b.getBytes(UTF8));
				}
			}

			if (context.isAllowHeaderParams())
				headers.setQueryParams(queryParams);

			debug = "true".equals(getQuery().getString("debug", "false")) || "true".equals(getHeaders().getString("Debug", "false"));

			this.pathParams = new RequestPathMatch();

		} catch (RestException e) {
			throw e;
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}

	/*
	 * Called from RestServlet after a match has been made but before the guard or method invocation.
	 */
	final void init(RestJavaMethod rjm, RestRequestProperties properties) {
		this.restJavaMethod = rjm;
		this.javaMethod = rjm.method;
		this.properties = properties;
		this.beanSession = rjm.beanContext.createSession();
		this.pathParams
			.parser(rjm.partParser)
			.beanSession(beanSession);
		this.queryParams
			.addDefault(rjm.defaultQuery)
			.parser(rjm.partParser)
			.beanSession(beanSession);
		this.headers
			.addDefault(rjm.defaultRequestHeaders)
			.addDefault(context.getDefaultRequestHeaders())
			.parser(rjm.partParser)
			.beanSession(beanSession);
		this.body
			.encoders(rjm.encoders)
			.parsers(rjm.parsers)
			.headers(headers)
			.beanSession(beanSession)
			.maxInput(rjm.maxInput);

		String stylesheet = getQuery().getString("stylesheet");
		if (stylesheet != null)
			getSession().setAttribute("stylesheet", stylesheet.replace(' ', '$'));  // Prevent SVL insertion.
		stylesheet = (String)getSession().getAttribute("stylesheet");
		if (stylesheet != null)
			properties.put(HTMLDOC_stylesheet, new String[]{stylesheet});

		if (debug) {
			String msg = ""
				+ "\n=== HTTP Request (incoming) ===================================================="
				+ toString()
				+ "\n=== END ========================================================================";
			context.getLogger().log(Level.WARNING, msg);
		}

		if (isPlainText())
			this.properties.put(SERIALIZER_useWhitespace, true);
	}

	/**
	 * Returns a string of the form <js>"HTTP method-name full-url"</js>
	 * 
	 * @return A description of the request.
	 */
	public String getDescription() {
		String qs = getQueryString();
		return "HTTP " + getMethod() + " " + getRequestURI() + (qs == null ? "" : "?" + qs);
	}

	/**
	 * Same as {@link #getAttribute(String)} but returns a default value if not found.
	 * 
	 * @param name The request attribute name.
	 * @param def The default value if the attribute doesn't exist.
	 * @return The request attribute value.
	 */
	public Object getAttribute(String name, Object def) {
		Object o = super.getAttribute(name);
		return (o == null ? def : o);
	}

	/**
	 * Shorthand method for calling {@link #setAttribute(String, Object)} fluently.
	 * 
	 * @param name The request attribute name.
	 * @param value The request attribute value.
	 * @return This object (for method chaining).
	 */
	public RestRequest attr(String name, Object value) {
		setAttribute(name, value);
		return this;
	}


	//--------------------------------------------------------------------------------
	// Properties
	//--------------------------------------------------------------------------------

	/**
	 * Retrieve the properties active for this request.
	 * 
	 * <p>
	 * These properties can be modified by the request.
	 * 
	 * @return The properties active for this request.
	 */
	public RestRequestProperties getProperties() {
		return this.properties;
	}

	/**
	 * Shortcut for calling <code>getProperties().append(name, value);</code> fluently.
	 * 
	 * @param name The property name.
	 * @param value The property value.
	 * @return This object (for method chaining).
	 */
	public RestRequest prop(String name, Object value) {
		this.properties.append(name, value);
		return this;
	}


	//--------------------------------------------------------------------------------
	// Headers
	//--------------------------------------------------------------------------------

	/**
	 * Returns the headers on this request.
	 * 
	 * @return The headers on this request.  Never <jk>null</jk>.
	 */
	public RequestHeaders getHeaders() {
		return headers;
	}

	@Override /* ServletRequest */
	public String getHeader(String name) {
		return getHeaders().getString(name);
	}

	@Override /* ServletRequest */
	public Enumeration<String> getHeaders(String name) {
		String[] v = headers.get(name);
		if (v == null || v.length == 0)
			return Collections.enumeration(Collections.EMPTY_LIST);
		return Collections.enumeration(Arrays.asList(v));
	}

	/**
	 * Returns the media types that are valid for <code>Accept</code> headers on the request.
	 * 
	 * @return The set of media types registered in the serializer group of this request.
	 */
	public List<MediaType> getProduces() {
		return restJavaMethod.supportedAcceptTypes;
	}

	/**
	 * Returns the media types that are valid for <code>Content-Type</code> headers on the request.
	 * 
	 * @return The set of media types registered in the parser group of this request.
	 */
	public List<MediaType> getConsumes() {
		return restJavaMethod.supportedContentTypes;
	}

	/**
	 * Sets the charset to expect on the request body.
	 */
	@Override /* ServletRequest */
	public void setCharacterEncoding(String charset) {
		this.charset = charset;
	}

	/**
	 * Returns the charset specified on the <code>Content-Type</code> header, or <js>"UTF-8"</js> if not specified.
	 */
	@Override /* ServletRequest */
	public String getCharacterEncoding() {
		if (charset == null) {
			// Determine charset
			// NOTE:  Don't use super.getCharacterEncoding() because the spec is implemented inconsistently.
			// Jetty returns the default charset instead of null if the character is not specified on the request.
			String h = getHeader("Content-Type");
			if (h != null) {
				int i = h.indexOf(";charset=");
				if (i > 0)
					charset = h.substring(i+9).trim();
			}
			if (charset == null)
				charset = restJavaMethod.defaultCharset;
			if (! Charset.isSupported(charset))
				throw new RestException(SC_UNSUPPORTED_MEDIA_TYPE, "Unsupported charset in header ''Content-Type'': ''{0}''", h);
		}
		return charset;
	}

	@Override /* ServletRequest */
	public Locale getLocale() {
		String h = headers.getString("Accept-Language");
		if (h != null) {
			MediaTypeRange[] mr = MediaTypeRange.parse(h);
			if (mr.length > 0)
				return toLocale(mr[0].getMediaType().getType());
		}
		return super.getLocale();
	}

	@Override /* ServletRequest */
	public Enumeration<Locale> getLocales() {
		String h = headers.getString("Accept-Language");
		if (h != null) {
			MediaTypeRange[] mr = MediaTypeRange.parse(h);
			if (mr.length > 0) {
				List<Locale> l = new ArrayList<Locale>(mr.length);
				for (MediaTypeRange r : mr)
					l.add(toLocale(r.getMediaType().getType()));
				return enumeration(l);
			}
		}
		return super.getLocales();
	}


	//--------------------------------------------------------------------------------
	// Query parameters
	//--------------------------------------------------------------------------------

	/**
	 * Equivalent to {@link #getParameterMap()}, but only looks for query parameters in the URL, not form posts.
	 * 
	 * <p>
	 * This method can be used to retrieve query parameters without triggering the underlying servlet API to load and
	 * parse the request body.
	 * 
	 * <p>
	 * This object is modifiable.
	 * 
	 * @return The query parameters as a modifiable map.
	 */
	public RequestQuery getQuery() {
		return queryParams;
	}

	/**
	 * Shortcut for calling <code>getQuery().getString(name)</code>.
	 * 
	 * @param name The query parameter name.
	 * @return The query parameter value, or <jk>null<jk> if not found.
	 */
	public String getQuery(String name) {
		return getQuery().getString(name);
	}


	//--------------------------------------------------------------------------------
	// Form data parameters
	//--------------------------------------------------------------------------------

	/**
	 * Retrieves the URL-encoded form data from the request if the body has already been cached locally.
	 * 
	 * @return The URL-encoded form data from the request.
	 */
	public RequestFormData getFormData() {
		try {
			if (formData == null) {
				formData = new RequestFormData();
				formData.setParser(restJavaMethod.partParser).setBeanSession(beanSession);
				if (! body.isLoaded()) {
					formData.putAll(getParameterMap());
				} else {
					Map<String,String[]> m = RestUtils.parseQuery(body.getReader());
					for (Map.Entry<String,String[]> e : m.entrySet()) {
						for (String v : e.getValue())
							formData.put(e.getKey(), v);
					}
				}
			}
			formData.addDefault(restJavaMethod.defaultFormData);
			return formData;
		} catch (Exception e) {
			throw new RestException(SC_INTERNAL_SERVER_ERROR, e);
		}
	}

	/**
	 * Shortcut for calling <code>getFormData().getString(name)</code>.
	 * 
	 * @param name The form data parameter name.
	 * @return The form data parameter value, or <jk>null<jk> if not found.
	 */
	public String getFormData(String name) {
		return getFormData().getString(name);
	}


	//--------------------------------------------------------------------------------
	// Path parameters
	//--------------------------------------------------------------------------------

	/**
	 * Retrieves the URL-encoded form data from the request if the body has already been cached locally.
	 * 
	 * @return The URL-encoded form data from the request.
	 */
	public RequestPathMatch getPathMatch() {
		return pathParams;
	}

	/**
	 * Shortcut for calling <code>getPathMatch().get(name)</code>.
	 * 
	 * @param name The path variable name.
	 * @return The path variable value, or <jk>null<jk> if not found.
	 */
	public String getPath(String name) {
		return getPathMatch().get(name);
	}

	//--------------------------------------------------------------------------------
	// Body methods
	//--------------------------------------------------------------------------------

	/**
	 * Returns the body of this HTTP request.
	 * 
	 * @return The body of this HTTP request.
	 */
	public RequestBody getBody() {
		return body;
	}

	/**
	 * Returns the HTTP body content as a {@link Reader}.
	 * 
	 * <p>
	 * If {@code allowHeaderParams} init parameter is true, then first looks for {@code &body=xxx} in the URL query
	 * string.
	 * 
	 * <p>
	 * Automatically handles GZipped input streams.
	 */
	@Override /* ServletRequest */
	public BufferedReader getReader() throws IOException {
		return getBody().getReader();
	}

	/**
	 * Returns the HTTP body content as an {@link InputStream}.
	 * 
	 * <p>
	 * Automatically handles GZipped input streams.
	 * 
	 * @return The negotiated input stream.
	 * @throws IOException If any error occurred while trying to get the input stream or wrap it in the GZIP wrapper.
	 */
	@Override /* ServletRequest */
	public ServletInputStream getInputStream() throws IOException {
		return getBody().getInputStream();
	}

	ServletInputStream getRawInputStream() throws IOException {
		return super.getInputStream();
	}


	//--------------------------------------------------------------------------------
	// URI-related methods
	//--------------------------------------------------------------------------------

	@Override /* HttpServletRequest */
	public String getContextPath() {
		String cp = context.getContextPath();
		return cp == null ? super.getContextPath() : cp;
	}

	@Override /* HttpServletRequest */
	public String getServletPath() {
		String cp = context.getContextPath();
		String sp = super.getServletPath();
		return cp == null || ! sp.startsWith(cp) ? sp : sp.substring(cp.length());
	}

	/**
	 * Returns the URI context of the request.
	 * 
	 * <p>
	 * The URI context contains all the information about the URI of the request, such as the servlet URI, context
	 * path, etc...
	 * 
	 * @return The URI context of the request.
	 */
	public UriContext getUriContext() {
		if (uriContext == null) {
			String scheme = getScheme();
			int port = getServerPort();
			StringBuilder authority = new StringBuilder(getScheme()).append("://").append(getServerName());
			if (! (port == 80 && "http".equals(scheme) || port == 443 && "https".equals(scheme)))
				authority.append(':').append(port);
			uriContext = new UriContext(authority.toString(), getContextPath(), getServletPath(), super.getPathInfo());
		}
		return uriContext;
	}

	/**
	 * Returns a URI resolver that can be used to convert URIs to absolute or root-relative form.
	 * 
	 * @param resolution The URI resolution rule.
	 * @param relativity The relative URI relativity rule.
	 * @return The URI resolver for this request.
	 */
	public UriResolver getUriResolver(UriResolution resolution, UriRelativity relativity) {
		return new UriResolver(resolution, relativity, getUriContext());
	}

	/**
	 * Shortcut for calling {@link #getUriResolver()} using {@link UriResolution#ROOT_RELATIVE} and
	 * {@link UriRelativity#RESOURCE}
	 * 
	 * @return The URI resolver for this request.
	 */
	public UriResolver getUriResolver() {
		return new UriResolver(UriResolution.ROOT_RELATIVE, UriRelativity.RESOURCE, getUriContext());
	}

	/**
	 * Returns the URI for this request.
	 * 
	 * <p>
	 * Similar to {@link #getRequestURI()} but returns the value as a {@link URI}.
	 * It also gives you the capability to override the query parameters (e.g. add new query parameters to the existing
	 * URI).
	 * 
	 * @param includeQuery If <jk>true</jk> include the query parameters on the request.
	 * @param addQueryParams Augment the request URI with the specified query parameters.
	 * @return A new URI.
	 */
	public URI getUri(boolean includeQuery, Map<String,?> addQueryParams) {
		String uri = getRequestURI();
		if (includeQuery || addQueryParams != null) {
			StringBuilder sb = new StringBuilder(uri);
			RequestQuery rq = this.queryParams.copy();
			if (addQueryParams != null)
				for (Map.Entry<String,?> e : addQueryParams.entrySet())
					rq.put(e.getKey(), e.getValue());
			if (! rq.isEmpty())
				sb.append('?').append(rq.toQueryString());
			uri = sb.toString();
		}
		try {
			return new URI(uri);
		} catch (URISyntaxException e) {
			// Shouldn't happen.
			throw new RuntimeException(e);
		}
	}

	//--------------------------------------------------------------------------------
	// Labels
	//--------------------------------------------------------------------------------

	/**
	 * Returns the info provider on the servlet.
	 * 
	 * @return The info provider on the servlet.
	 */
	public RestInfoProvider getInfoProvider() {
		return context.getInfoProvider();
	}
	
	/**
	 * Returns the localized site name.
	 * 
	 * <p>
	 * The site name is intended to be a title that can be applied to the entire site.
	 * 
	 * <p>
	 * One possible use is if you want to add the same title to the top of all pages by defining a header on a
	 * common parent class like so:
	 * <p class='bcode'>
	 * 	htmldoc=<ja>@HtmlDoc</ja>(
	 * 		header={
	 * 			<js>"&lt;h1&gt;$R{siteName}&lt;/h1&gt;"</js>,
	 * 			<js>"&lt;h2&gt;$R{servletTitle}&lt;/h2&gt;"</js>
	 * 		}
	 * 	)
	 * </p>
	 * 
	 * <p>
	 * Equivalent to calling {@link RestInfoProvider#getSiteName(RestRequest)} with this object.
	 * 
	 * @return The localized servlet label.
	 */
	public String getSiteName() {
		try {
			return context.getInfoProvider().getSiteName(this);
		} catch (RestException e) {
			throw e;
		} catch (Exception e) {
			throw new RestException(SC_INTERNAL_SERVER_ERROR, e);
		}
	}

	/**
	 * Returns the localized servlet title.
	 * 
	 * <p>
	 * Equivalent to calling {@link RestInfoProvider#getTitle(RestRequest)} with this object.
	 * 
	 * @return The localized servlet label.
	 */
	public String getServletTitle() {
		try {
			return context.getInfoProvider().getTitle(this);
		} catch (RestException e) {
			throw e;
		} catch (Exception e) {
			throw new RestException(SC_INTERNAL_SERVER_ERROR, e);
		}
	}

	/**
	 * Returns the localized servlet description.
	 * 
	 * <p>
	 * Equivalent to calling {@link RestInfoProvider#getDescription(RestRequest)} with this object.
	 * 
	 * @return The localized servlet description.
	 */
	public String getServletDescription() {
		try {
			return context.getInfoProvider().getDescription(this);
		} catch (RestException e) {
			throw e;
		} catch (Exception e) {
			throw new RestException(SC_INTERNAL_SERVER_ERROR, e);
		}
	}

	/**
	 * Returns the localized method summary.
	 * 
	 * <p>
	 * Equivalent to calling {@link RestInfoProvider#getMethodSummary(Method, RestRequest)} with this object.
	 * 
	 * @return The localized method description.
	 */
	public String getMethodSummary() {
		try {
			return context.getInfoProvider().getMethodSummary(javaMethod, this);
		} catch (RestException e) {
			throw e;
		} catch (Exception e) {
			throw new RestException(SC_INTERNAL_SERVER_ERROR, e);
		}
	}

	/**
	 * Returns the localized method description.
	 * 
	 * <p>
	 * Equivalent to calling {@link RestInfoProvider#getMethodDescription(Method, RestRequest)} with this object.
	 * 
	 * @return The localized method description.
	 */
	public String getMethodDescription() {
		try {
			return context.getInfoProvider().getMethodDescription(javaMethod, this);
		} catch (RestException e) {
			throw e;
		} catch (Exception e) {
			throw new RestException(SC_INTERNAL_SERVER_ERROR, e);
		}
	}

	//--------------------------------------------------------------------------------
	// Other methods
	//--------------------------------------------------------------------------------

	/**
	 * Returns the serializers associated with this request.
	 * 
	 * @return The serializers associated with this request.
	 */
	public SerializerGroup getSerializers() {
		return restJavaMethod.serializers;
	}

	/**
	 * Returns the parsers associated with this request.
	 * 
	 * @return The parsers associated with this request.
	 */
	public ParserGroup getParsers() {
		return restJavaMethod.parsers;
	}


	/**
	 * Returns the method of this request.
	 * 
	 * <p>
	 * If <code>allowHeaderParams</code> init parameter is <jk>true</jk>, then first looks for
	 * <code>&amp;method=xxx</code> in the URL query string.
	 */
	@Override /* ServletRequest */
	public String getMethod() {
		return method;
	}

	/**
	 * Returns the HTTP 1.1 method name of the request as an enum.
	 * 
	 * <p>
	 * Note that non-RFC2616 method names resolve as {@link HttpMethod#OTHER}.
	 * 
	 * @return The HTTP method.
	 */
	public HttpMethod getHttpMethod() {
		return HttpMethod.forString(method);
	}

	@Override /* ServletRequest */
	public int getContentLength() {
		return getBody().getContentLength();
	}

	int getRawContentLength() {
		return super.getContentLength();
	}

	/**
	 * Returns <jk>true</jk> if <code>&amp;plainText=true</code> was specified as a URL parameter.
	 * 
	 * <p>
	 * This indicates that the <code>Content-Type</code> of the output should always be set to <js>"text/plain"</js>
	 * to make it easy to render in a browser.
	 * 
	 * <p>
	 * This feature is useful for debugging.
	 * 
	 * @return <jk>true</jk> if {@code &amp;plainText=true} was specified as a URL parameter
	 */
	public boolean isPlainText() {
		return "true".equals(getQuery().getString("plainText", "false"));
	}

	/**
	 * Shortcut method for calling {@link MessageBundle#getString(Locale, String, Object...)} based on the request locale.
	 * 
	 * @param key The message key.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 * @return The localized message.
	 */
	public String getMessage(String key, Object...args) {
		return context.getMessages().getString(getLocale(), key, args);
	}

	/**
	 * Returns the resource bundle for the request locale.
	 * 
	 * @return The resource bundle.  Never <jk>null</jk>.
	 */
	public MessageBundle getResourceBundle() {
		return context.getMessages().getBundle(getLocale());
	}

	/**
	 * Returns the servlet handling the request.
	 * 
	 * <p>
	 * Can be used to access servlet-init parameters or annotations during requests, such as in calls to
	 * {@link RestGuard#guard(RestRequest, RestResponse)}..
	 * 
	 * @return The servlet handling the request.
	 */
	public RestContext getContext() {
		return context;
	}

	/**
	 * Returns the java method handling the request.
	 * 
	 * <p>
	 * Can be used to access the method name or method annotations during requests, such as in calls to
	 * {@link RestGuard#guard(RestRequest, RestResponse)}.
	 * 
	 * <h5 class='section'>Notes:</h5>
	 * <ul>
	 * 	<li>This returns null when evaluating servlet-level guards since the method has not been resolved at that
	 * 		point of execution.
	 * </ul>
	 * 
	 * @return The Java method handling the request, or <code>null</code> if the method has not yet been resolved.
	 */
	public Method getJavaMethod() {
		return javaMethod;
	}

	/**
	 * Returns the {@link BeanSession} associated with this request.
	 * 
	 * @return The request bean session.
	 */
	public BeanSession getBeanSession() {
		return beanSession;
	}

	/**
	 * Returns the variable resolver session for this request using session objects created by
	 * {@link RestCallHandler#getSessionObjects(RestRequest)}.
	 * 
	 * <p>
	 * See {@link RestContext#getVarResolver()} for the list of supported variables.
	 * 
	 * @return The variable resolver for this request.
	 */
	public VarResolverSession getVarResolverSession() {
		if (varSession == null)
			varSession = context.getVarResolver().createSession(context.getCallHandler().getSessionObjects(this));
		return varSession;
	}

	/**
	 * Shortcut for calling <code>getVarResolverSession().resolve(input)</code>.
	 * 
	 * @param input The input string to resolve variables in.
	 * @return The string with variables resolved, or <jk>null</jk> if input is null.
	 */
	public String resolveVars(String input) {
		return getVarResolverSession().resolve(input);
	}

	/**
	 * Shortcut for calling {@link #resolveVars(String[])} on all elements in the array.
	 * 
	 * @param input The input strings to resolve variables in.
	 * @return A copy of the array with variables resolved.
	 */
	public String[] resolveVars(String[] input) {
		return getVarResolverSession().resolve(input);
	}

	/**
	 * Returns an instance of a {@link ReaderResource} that represents the contents of a resource text file from the
	 * classpath.
	 * 
	 * @param name The name of the resource (i.e. the value normally passed to {@link Class#getResourceAsStream(String)}.
	 * @param resolveVars
	 * 	If <jk>true</jk>, any {@link org.apache.juneau.rest.annotation.Parameter @Parameter} variables will be
	 * 	resolved by the variable resolver returned by {@link #getVarResolverSession()}.
	 * 	<br>See {@link RestContext#getVarResolver()} for the list of supported variables.
	 * @param mediaType The value to set as the <js>"Content-Type"</js> header for this object.
	 * @return A new reader resource, or <jk>null</jk> if resource could not be found.
	 * @throws IOException
	 */
	public ReaderResource getClasspathReaderResource(String name, boolean resolveVars, MediaType mediaType) throws IOException {
		String s = context.getClasspathResourceAsString(name, getLocale());
		if (s == null)
			return null;
		ReaderResource.Builder b = new ReaderResource.Builder().mediaType(mediaType).contents(s);
		if (resolveVars)
			b.varResolver(getVarResolverSession());
		return b.build();
	}

	/**
	 * Same as {@link #getClasspathReaderResource(String, boolean, MediaType)} except uses the resource mime-type map
	 * constructed using {@link RestContextBuilder#mimeTypes(String...)} to determine the media type.
	 * 
	 * @param name The name of the resource (i.e. the value normally passed to {@link Class#getResourceAsStream(String)}.
	 * @param resolveVars
	 * 	If <jk>true</jk>, any {@link org.apache.juneau.rest.annotation.Parameter @Parameter} variables will be
	 * 	resolved by the variable resolver returned by {@link #getVarResolverSession()}.
	 * 	<br>See {@link RestContext#getVarResolver()} for the list of supported variables.
	 * @return A new reader resource, or <jk>null</jk> if resource could not be found.
	 * @throws IOException
	 */
	public ReaderResource getClasspathReaderResource(String name, boolean resolveVars) throws IOException {
		return getClasspathReaderResource(name, resolveVars, MediaType.forString(context.getMediaTypeForName(name)));
	}

	/**
	 * Same as {@link #getClasspathReaderResource(String, boolean)} with <code>resolveVars == <jk>false</jk></code>
	 * 
	 * @param name The name of the resource (i.e. the value normally passed to {@link Class#getResourceAsStream(String)}.
	 * @return A new reader resource, or <jk>null</jk> if resource could not be found.
	 * @throws IOException
	 */
	public ReaderResource getClasspathReaderResource(String name) throws IOException {
		return getClasspathReaderResource(name, false, MediaType.forString(context.getMediaTypeForName(name)));
	}

	/**
	 * Returns the config file associated with the servlet.
	 * 
	 * @return
	 * 	The config file associated with the servlet, or <jk>null</jk> if servlet does not have a config file
	 * 	associated with it.
	 */
	public ConfigFile getConfigFile() {
		if (cf == null)
			cf = context.getConfigFile().getResolving(getVarResolverSession());
		return cf;
	}

	/**
	 * Returns the localized swagger associated with the servlet.
	 * 
	 * @return
	 * 	The swagger associated with the servlet.
	 * 	Never <jk>null</jk>.
	 */
	public Swagger getSwagger() {
		try {
			if (swagger == null)
				swagger = context.getInfoProvider().getSwagger(this);
			return swagger;
		} catch (RestException e) {
			throw e;
		} catch (Exception e) {
			throw new RestException(SC_INTERNAL_SERVER_ERROR, e);
		}
	}

	/**
	 * Returns the widgets used for resolving <js>"$W{...}"</js> string variables.
	 * 
	 * @return
	 * 	The widgets used for resolving <js>"$W{...}"</js> string variables.
	 * 	Never <jk>null</jk>.
	 */
	public Map<String,Widget> getWidgets() {
		return restJavaMethod.widgets;
	}

	@Override /* Object */
	public String toString() {
		StringBuilder sb = new StringBuilder("\n").append(getDescription()).append("\n");
		sb.append("---Headers---\n");
		for (Enumeration<String> e = getHeaderNames(); e.hasMoreElements();) {
			String h = e.nextElement();
			sb.append("\t").append(h).append(": ").append(getHeader(h)).append("\n");
		}
		sb.append("---Default Servlet Headers---\n");
		for (Map.Entry<String,Object> e : context.getDefaultRequestHeaders().entrySet()) {
			sb.append("\t").append(e.getKey()).append(": ").append(e.getValue()).append("\n");
		}
		if (javaMethod == null) {
			sb.append("***init() not called yet!***\n");
		} else if (method.equals("PUT") || method.equals("POST")) {
			try {
				sb.append("---Body UTF-8---\n");
				sb.append(body.asString()).append("\n");
				sb.append("---Body Hex---\n");
				sb.append(body.asSpacedHex()).append("\n");
			} catch (Exception e1) {
				sb.append(e1.getLocalizedMessage());
				context.getLogger().log(WARNING, e1, "Error occurred while trying to read debug input.");
			}
		}
		return sb.toString();
	}

	/**
	 * Shortcut for calling <code>getContext().getLogger()</code>.
	 * 
	 * @return The logger associated with the resource context.
	 */
	public RestLogger getLogger() {
		return context.getLogger();
	}

	//--------------------------------------------------------------------------------
	// Utility methods
	//--------------------------------------------------------------------------------

	/*
	 * Converts an Accept-Language value entry to a Locale.
	 */
	private static Locale toLocale(String lang) {
		String country = "";
		int i = lang.indexOf('-');
		if (i > -1) {
			country = lang.substring(i+1).trim();
			lang = lang.substring(0,i).trim();
		}
		return new Locale(lang, country);
	}


	void setJavaMethod(Method method) {
		this.javaMethod = method;
	}
}