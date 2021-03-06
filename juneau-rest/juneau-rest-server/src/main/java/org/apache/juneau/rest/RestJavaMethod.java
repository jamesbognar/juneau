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

import static javax.servlet.http.HttpServletResponse.*;
import static org.apache.juneau.BeanContext.*;
import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.internal.Utils.*;
import static org.apache.juneau.rest.RestContext.*;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

import javax.servlet.http.*;

import org.apache.juneau.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.http.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.widget.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.utils.*;

/**
 * Represents a single Java servlet/resource method annotated with {@link RestMethod @RestMethod}.
 */
public class RestJavaMethod implements Comparable<RestJavaMethod>  {
	private final String httpMethod;
	private final UrlPathPattern pathPattern;
	final RestParam[] params;
	private final RestGuard[] guards;
	private final RestMatcher[] optionalMatchers;
	private final RestMatcher[] requiredMatchers;
	private final RestConverter[] converters;
	private final RestMethodProperties properties;
	private final Integer priority;
	private final RestContext context;
	final java.lang.reflect.Method method;
	final SerializerGroup serializers;
	final ParserGroup parsers;
	final EncoderGroup encoders;
	final HttpPartSerializer partSerializer;
	final HttpPartParser partParser;
	final Map<String,Object> 
		defaultRequestHeaders,
		defaultQuery,
		defaultFormData;
	final String defaultCharset;
	final long maxInput;
	final BeanContext beanContext;
	final Map<String,Widget> widgets;
	final List<MediaType> 
		supportedAcceptTypes,
		supportedContentTypes;

	RestJavaMethod(Object servlet, java.lang.reflect.Method method, RestContext context) throws RestServletException {
		Builder b = new Builder(servlet, method, context);
		this.context = context;
		this.method = method;
		this.httpMethod = b.httpMethod;
		this.pathPattern = b.pathPattern;
		this.params = b.params;
		this.guards = b.guards;
		this.optionalMatchers = b.optionalMatchers;
		this.requiredMatchers = b.requiredMatchers;
		this.converters = b.converters;
		this.serializers = b.serializers;
		this.parsers = b.parsers;
		this.encoders = b.encoders;
		this.partParser = b.partParser;
		this.partSerializer = b.partSerializer;
		this.beanContext = b.beanContext;
		this.properties = b.properties;
		this.defaultRequestHeaders = b.defaultRequestHeaders;
		this.defaultQuery = b.defaultQuery;
		this.defaultFormData = b.defaultFormData;
		this.defaultCharset = b.defaultCharset;
		this.maxInput = b.maxInput;
		this.priority = b.priority;
		this.supportedAcceptTypes = b.supportedAcceptTypes;
		this.supportedContentTypes = b.supportedContentTypes;
		this.widgets = Collections.unmodifiableMap(b.widgets);
	}

	private static final class Builder  {
		String httpMethod, defaultCharset;
		UrlPathPattern pathPattern;
		RestParam[] params;
		RestGuard[] guards;
		RestMatcher[] optionalMatchers, requiredMatchers;
		RestConverter[] converters;
		SerializerGroup serializers;
		ParserGroup parsers;
		EncoderGroup encoders;
		HttpPartParser partParser;
		HttpPartSerializer partSerializer;
		BeanContext beanContext;
		RestMethodProperties properties;
		Map<String,Object> defaultRequestHeaders, defaultQuery, defaultFormData;
		long maxInput;
		Integer priority;
		Map<String,Widget> widgets;
		List<MediaType> supportedAcceptTypes, supportedContentTypes;

		Builder(Object servlet, java.lang.reflect.Method method, RestContext context) throws RestServletException {
			String sig = method.getDeclaringClass().getName() + '.' + method.getName();

			try {

				RestMethod m = method.getAnnotation(RestMethod.class);
				if (m == null)
					throw new RestServletException("@RestMethod annotation not found on method ''{0}''", sig);
				
				VarResolver vr = context.getVarResolver();

				serializers = context.getSerializers();
				parsers = context.getParsers();
				partSerializer = context.getPartSerializer();
				partParser = context.getPartParser();
				beanContext = context.getBeanContext();
				encoders = context.getEncoders();
				properties = new RestMethodProperties(context.getProperties());
				defaultCharset = context.getDefaultCharset();
				maxInput = context.getMaxInput();

				if (! m.defaultCharset().isEmpty())
					defaultCharset = vr.resolve(m.defaultCharset());
				if (! m.maxInput().isEmpty())
					maxInput = StringUtils.parseLongWithSuffix(vr.resolve(m.maxInput()));

				HtmlDocBuilder hdb = new HtmlDocBuilder(properties);

				HtmlDoc hd = m.htmldoc();
				hdb.process(hd);

				widgets = new HashMap<>(context.getWidgets());
				for (Class<? extends Widget> wc : hd.widgets()) {
					Widget w = beanContext.newInstance(Widget.class, wc);
					widgets.put(w.getName(), w);
					hdb.script("INHERIT", "$W{"+w.getName()+".script}");
					hdb.style("INHERIT", "$W{"+w.getName()+".style}");
				}

				ASet<String> inherit = new ASet<String>().appendAll(StringUtils.split(m.inherit()));
				if (inherit.contains("*")) 
					inherit.appendAll("SERIALIZERS","PARSERS","TRANSFORMS","PROPERTIES","ENCODERS");

				SerializerGroupBuilder sgb = null;
				ParserGroupBuilder pgb = null;
				ParserBuilder uepb = null;
				BeanContextBuilder bcb = null;
				PropertyStore cps = context.getPropertyStore();

				if (m.serializers().length > 0 || m.parsers().length > 0 || m.properties().length > 0 || m.flags().length > 0
						|| m.beanFilters().length > 0 || m.pojoSwaps().length > 0 || m.bpi().length > 0
						|| m.bpx().length > 0) {
					sgb = SerializerGroup.create();
					pgb = ParserGroup.create();
					uepb = Parser.create();
					bcb = beanContext.builder();

					if (inherit.contains("SERIALIZERS") || m.serializers().length == 0)
						sgb.append(cps.getArrayProperty(REST_serializers, Object.class));

					if (inherit.contains("PARSERS") || m.parsers().length == 0)
						pgb.append(cps.getArrayProperty(REST_parsers, Object.class));
				}

				httpMethod = m.name().toUpperCase(Locale.ENGLISH);
				if (httpMethod.equals("") && method.getName().startsWith("do"))
					httpMethod = method.getName().substring(2).toUpperCase(Locale.ENGLISH);
				if (httpMethod.equals(""))
					httpMethod = "GET";
				if (httpMethod.equals("METHOD"))
					httpMethod = "*";

				priority = m.priority();

				String p = m.path();
				converters = new RestConverter[m.converters().length];
				for (int i = 0; i < converters.length; i++)
					converters[i] = beanContext.newInstance(RestConverter.class, m.converters()[i]);

				guards = new RestGuard[m.guards().length];
				for (int i = 0; i < guards.length; i++)
					guards[i] = beanContext.newInstance(RestGuard.class, m.guards()[i]);

				List<RestMatcher> optionalMatchers = new LinkedList<>(), requiredMatchers = new LinkedList<>();
				for (int i = 0; i < m.matchers().length; i++) {
					Class<? extends RestMatcher> c = m.matchers()[i];
					RestMatcher matcher = beanContext.newInstance(RestMatcher.class, c, true, servlet, method);
					if (matcher.mustMatch())
						requiredMatchers.add(matcher);
					else
						optionalMatchers.add(matcher);
				}
				if (! m.clientVersion().isEmpty())
					requiredMatchers.add(new ClientVersionMatcher(context.getClientVersionHeader(), method));

				this.requiredMatchers = requiredMatchers.toArray(new RestMatcher[requiredMatchers.size()]);
				this.optionalMatchers = optionalMatchers.toArray(new RestMatcher[optionalMatchers.size()]);

				PropertyStore ps = context.getPropertyStore();
				if (! inherit.contains("TRANSFORMS"))
					ps = ps.builder().set(BEAN_beanFilters, null).set(BEAN_pojoSwaps, null).build();
				
				if (sgb != null) {
					sgb.append(m.serializers());
				
					if (! inherit.contains("PROPERTIES"))
						sgb.beanFilters((Object[])ps.getClassArrayProperty(BEAN_beanFilters)).pojoSwaps(ps.getClassArrayProperty(BEAN_pojoSwaps));
					else
						sgb.apply(ps);
					for (Property p1 : m.properties())
						sgb.set(p1.name(), p1.value());
					for (String p1 : m.flags())
						sgb.set(p1, true);
					if (m.bpi().length > 0) {
						Map<String,String> bpiMap = new LinkedHashMap<>();
						for (String s : m.bpi()) {
							for (String s2 : split(s, ';')) {
								int i = s2.indexOf(':');
								if (i == -1)
									throw new RestServletException(
										"Invalid format for @RestMethod.bpi() on method ''{0}''.  Must be in the format \"ClassName: comma-delimited-tokens\".  \nValue: {1}", sig, s);
								bpiMap.put(s2.substring(0, i).trim(), s2.substring(i+1).trim());
							}
						}
						sgb.includeProperties(bpiMap);
					}
					if (m.bpx().length > 0) {
						Map<String,String> bpxMap = new LinkedHashMap<>();
						for (String s : m.bpx()) {
							for (String s2 : split(s, ';')) {
								int i = s2.indexOf(':');
								if (i == -1)
									throw new RestServletException(
										"Invalid format for @RestMethod.bpx() on method ''{0}''.  Must be in the format \"ClassName: comma-delimited-tokens\".  \nValue: {1}", sig, s);
								bpxMap.put(s2.substring(0, i).trim(), s2.substring(i+1).trim());
							}
						}
						sgb.excludeProperties(bpxMap);
					}
					sgb.beanFilters((Object[])m.beanFilters());
					sgb.pojoSwaps(m.pojoSwaps());
				}

				if (pgb != null) {
					pgb.append(m.parsers());
					if (! inherit.contains("PROPERTIES"))
						pgb.beanFilters((Object[])ps.getClassArrayProperty(BEAN_beanFilters)).pojoSwaps(ps.getClassArrayProperty(BEAN_pojoSwaps));
					else
						pgb.apply(ps);
					for (Property p1 : m.properties())
						pgb.set(p1.name(), p1.value());
					for (String p1 : m.flags())
						pgb.set(p1, true);
					pgb.beanFilters((Object[])m.beanFilters());
					pgb.pojoSwaps(m.pojoSwaps());
				}

				if (uepb != null) {
					uepb.apply(ps);
					for (Property p1 : m.properties())
						uepb.set(p1.name(), p1.value());
					for (String p1 : m.flags())
						uepb.set(p1, true);
					uepb.beanFilters((Object[])m.beanFilters());
					uepb.pojoSwaps(m.pojoSwaps());
				}
				
				if (bcb != null) {
					bcb.apply(ps);
					for (Property p1 : m.properties())
						bcb.set(p1.name(), p1.value());
					for (String p1 : m.flags())
						bcb.set(p1, true);
					bcb.beanFilters((Object[])m.beanFilters());
					bcb.pojoSwaps(m.pojoSwaps());
				}
				
				if (m.properties().length > 0 || m.flags().length > 0) {
					properties = new RestMethodProperties(properties);
					for (Property p1 : m.properties())
						properties.put(p1.name(), p1.value());
					for (String p1 : m.flags())
						properties.put(p1, true);
				}

				if (m.encoders().length > 0) {
					EncoderGroupBuilder g = EncoderGroup.create().append(IdentityEncoder.INSTANCE);
					if (inherit.contains("ENCODERS"))
						g.append(encoders);

					for (Class<? extends Encoder> c : m.encoders()) {
						try {
							g.append(c);
						} catch (Exception e) {
							throw new RestServletException(
								"Exception occurred while trying to instantiate Encoder on method ''{0}'': ''{1}''", sig, c.getSimpleName()).initCause(e);
						}
					}
					encoders = g.build();
				}

				defaultRequestHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
				for (String s : m.defaultRequestHeaders()) {
					String[] h = RestUtils.parseKeyValuePair(vr.resolve(s));
					if (h == null)
						throw new RestServletException(
							"Invalid default request header specified on method ''{0}'': ''{1}''.  Must be in the format: ''name[:=]value''", sig, s);
					defaultRequestHeaders.put(h[0], h[1]);
				}

				defaultQuery = new LinkedHashMap<>();
				for (String s : m.defaultQuery()) {
					String[] h = RestUtils.parseKeyValuePair(vr.resolve(s));
					if (h == null)
						throw new RestServletException(
							"Invalid default query parameter specified on method ''{0}'': ''{1}''.  Must be in the format: ''name[:=]value''", sig, s);
					defaultQuery.put(h[0], h[1]);
				}

				defaultFormData = new LinkedHashMap<>();
				for (String s : m.defaultFormData()) {
					String[] h = RestUtils.parseKeyValuePair(vr.resolve(s));
					if (h == null)
						throw new RestServletException(
							"Invalid default form data parameter specified on method ''{0}'': ''{1}''.  Must be in the format: ''name[:=]value''", sig, s);
					defaultFormData.put(h[0], h[1]);
				}

				Type[] pt = method.getGenericParameterTypes();
				Annotation[][] pa = method.getParameterAnnotations();
				for (int i = 0; i < pt.length; i++) {
					for (Annotation a : pa[i]) {
						if (a instanceof Header) {
							Header h = (Header)a;
							if (! h.def().isEmpty())
								defaultRequestHeaders.put(firstNonEmpty(h.name(), h.value()), h.def());
						} else if (a instanceof Query) {
							Query q = (Query)a;
							if (! q.def().isEmpty())
								defaultQuery.put(firstNonEmpty(q.name(), q.value()), q.def());
						} else if (a instanceof FormData) {
							FormData f = (FormData)a;
							if (! f.def().isEmpty())
								defaultFormData.put(firstNonEmpty(f.name(), f.value()), f.def());
						}
					}
				}

				pathPattern = new UrlPathPattern(p);

				if (sgb != null) 
					serializers = sgb.build();
				if (pgb != null)
					parsers = pgb.build();
				if (uepb != null && partParser instanceof Parser) {
					Parser pp = (Parser)partParser;
					partParser = (HttpPartParser)pp.builder().apply(uepb.getPropertyStore()).build();
				}
				if (bcb != null)
					beanContext = bcb.build();

				supportedAcceptTypes = 
					m.produces().length > 0 
					? Collections.unmodifiableList(new ArrayList<>(Arrays.asList(MediaType.forStrings(resolveVars(vr, m.produces()))))) 
					: serializers.getSupportedMediaTypes();
				supportedContentTypes =
					m.consumes().length > 0 
					? Collections.unmodifiableList(new ArrayList<>(Arrays.asList(MediaType.forStrings(resolveVars(vr, m.consumes()))))) 
					: parsers.getSupportedMediaTypes();
					
				params = context.findParams(method, pathPattern, false);

				// Need this to access methods in anonymous inner classes.
				method.setAccessible(true);
			} catch (RestServletException e) {
				throw e;
			} catch (Exception e) {
				throw new RestServletException("Exception occurred while initializing method ''{0}''", sig).initCause(e);
			}
		}
	}

	/**
	 * Returns <jk>true</jk> if this Java method has any guards or matchers.
	 */
	boolean hasGuardsOrMatchers() {
		return (guards.length != 0 || requiredMatchers.length != 0 || optionalMatchers.length != 0);
	}

	/**
	 * Returns the HTTP method name (e.g. <js>"GET"</js>).
	 */
	String getHttpMethod() {
		return httpMethod;
	}

	/**
	 * Returns the path pattern for this method.
	 */
	String getPathPattern() {
		return pathPattern.toString();
	}

	/**
	 * Returns <jk>true</jk> if the specified request object can call this method.
	 */
	boolean isRequestAllowed(RestRequest req) {
		for (RestGuard guard : guards) {
			req.setJavaMethod(method);
			if (! guard.isRequestAllowed(req))
				return false;
		}
		return true;
	}

	/**
	 * Workhorse method.
	 * 
	 * @param pathInfo The value of {@link HttpServletRequest#getPathInfo()} (sorta)
	 * @return The HTTP response code.
	 */
	int invoke(String pathInfo, RestRequest req, RestResponse res) throws RestException {

		String[] patternVals = pathPattern.match(pathInfo);
		if (patternVals == null)
			return SC_NOT_FOUND;

		String remainder = null;
		if (patternVals.length > pathPattern.getVars().length)
			remainder = patternVals[pathPattern.getVars().length];
		for (int i = 0; i < pathPattern.getVars().length; i++)
			req.getPathMatch().put(pathPattern.getVars()[i], patternVals[i]);
		req.getPathMatch().pattern(pathPattern.getPatternString()).remainder(remainder);

		RestRequestProperties requestProperties = new RestRequestProperties(req.getVarResolverSession(), properties);

		req.init(this, requestProperties);
		res.init(this, requestProperties);

		// Class-level guards
		for (RestGuard guard : context.getGuards())
			if (! guard.guard(req, res))
				return SC_UNAUTHORIZED;

		// If the method implements matchers, test them.
		for (RestMatcher m : requiredMatchers)
			if (! m.matches(req))
				return SC_PRECONDITION_FAILED;
		if (optionalMatchers.length > 0) {
			boolean matches = false;
			for (RestMatcher m : optionalMatchers)
				matches |= m.matches(req);
			if (! matches)
				return SC_PRECONDITION_FAILED;
		}

		context.preCall(req, res);

		Object[] args = new Object[params.length];
		for (int i = 0; i < params.length; i++) {
			try {
				args[i] = params[i].resolve(req, res);
			} catch (RestException e) {
				throw e;
			} catch (Exception e) {
				throw new RestException(SC_BAD_REQUEST,
					"Invalid data conversion.  Could not convert {0} ''{1}'' to type ''{2}'' on method ''{3}.{4}''.",
					params[i].getParamType().name(), params[i].getName(), params[i].getType(), method.getDeclaringClass().getName(), method.getName()
				).initCause(e);
			}
		}

		try {

			for (RestGuard guard : guards)
				if (! guard.guard(req, res))
					return SC_OK;

			Object output = method.invoke(context.getResource(), args);
			if (! method.getReturnType().equals(Void.TYPE))
				if (output != null || ! res.getOutputStreamCalled())
					res.setOutput(output);

			context.postCall(req, res);

			if (res.hasOutput()) {
				output = res.getOutput();
				for (RestConverter converter : converters)
					output = converter.convert(req, output);
				res.setOutput(output);
			}
		} catch (IllegalArgumentException e) {
			throw new RestException(SC_BAD_REQUEST,
				"Invalid argument type passed to the following method: ''{0}''.\n\tArgument types: {1}",
				method.toString(), getReadableClassNames(args)
			).initCause(e);
		} catch (InvocationTargetException e) {
			Throwable e2 = e.getTargetException();		// Get the throwable thrown from the doX() method.
			if (e2 instanceof RestException)
				throw (RestException)e2;
			if (e2 instanceof ParseException)
				throw new RestException(SC_BAD_REQUEST, e2);
			if (e2 instanceof InvalidDataConversionException)
				throw new RestException(SC_BAD_REQUEST, e2);
			throw new RestException(SC_INTERNAL_SERVER_ERROR, e2);
		} catch (RestException e) {
			throw e;
		} catch (Exception e) {
			throw new RestException(SC_INTERNAL_SERVER_ERROR, e);
		}
		return SC_OK;
	}

	@Override /* Object */
	public String toString() {
		return "SimpleMethod: name=" + httpMethod + ", path=" + pathPattern.getPatternString();
	}

	/*
	 * compareTo() method is used to keep SimpleMethods ordered in the RestCallRouter list.
	 * It maintains the order in which matches are made during requests.
	 */
	@Override /* Comparable */
	public int compareTo(RestJavaMethod o) {
		int c;

		c = priority.compareTo(o.priority);
		if (c != 0)
			return c;

		c = pathPattern.compareTo(o.pathPattern);
		if (c != 0)
			return c;

		c = compare(o.requiredMatchers.length, requiredMatchers.length);
		if (c != 0)
			return c;

		c = compare(o.optionalMatchers.length, optionalMatchers.length);
		if (c != 0)
			return c;

		c = compare(o.guards.length, guards.length);
		if (c != 0)
			return c;

		return 0;
	}

	@Override /* Object */
	public boolean equals(Object o) {
		if (! (o instanceof RestJavaMethod))
			return false;
		return (compareTo((RestJavaMethod)o) == 0);
	}

	@Override /* Object */
	public int hashCode() {
		return super.hashCode();
	}
	
	static String[] resolveVars(VarResolver vr, String[] in) {
		String[] out = new String[in.length];
		for (int i = 0; i < in.length; i++) 
			out[i] = vr.resolve(in[i]);
		return out;
	}
}