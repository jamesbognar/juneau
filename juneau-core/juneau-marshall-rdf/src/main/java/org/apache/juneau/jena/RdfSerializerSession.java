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
package org.apache.juneau.jena;

import static org.apache.juneau.jena.Constants.*;
import static org.apache.juneau.jena.RdfCommon.*;
import static org.apache.juneau.jena.RdfSerializer.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;
import org.apache.juneau.xml.*;

import com.hp.hpl.jena.rdf.model.*;

/**
 * Session object that lives for the duration of a single use of {@link RdfSerializer}.
 * 
 * <p>
 * This class is NOT thread safe.  
 * It is typically discarded after one-time use although it can be reused within the same thread.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class RdfSerializerSession extends WriterSerializerSession {

	private final String rdfLanguage;
	private final Namespace juneauNs, juneauBpNs;
	private final boolean
		addLiteralTypes,
		addRootProperty,
		useXmlNamespaces,
		looseCollections,
		autoDetectNamespaces,
		addBeanTypeProperties;
	private final Property pRoot, pValue;
	private final Model model;
	private final RDFWriter writer;
	private final RdfCollectionFormat collectionFormat;
	private final Namespace[] namespaces;

	/**
	 * Create a new session using properties specified in the context.
	 * 
	 * @param ctx
	 * 	The context creating this session object.
	 * 	The context contains all the configuration settings for this object.
	 * @param args
	 * 	Runtime arguments.
	 * 	These specify session-level information such as locale and URI context.
	 * 	It also include session-level properties that override the properties defined on the bean and
	 * 	serializer contexts.
	 */
	protected RdfSerializerSession(RdfSerializer ctx, SerializerSessionArgs args) {
		super(ctx, args);
				
		rdfLanguage = getProperty(RDF_language, String.class, ctx.rdfLanguage);
		juneauNs = getInstanceProperty(RDF_juneauNs, Namespace.class, ctx.juneauNs);
		juneauBpNs = getInstanceProperty(RDF_juneauBpNs, Namespace.class, ctx.juneauBpNs);
		addLiteralTypes = getProperty(RDF_addLiteralTypes, boolean.class, ctx.addLiteralTypes);
		addRootProperty = getProperty(RDF_addRootProperty, boolean.class, ctx.addRootProperty);
		collectionFormat = getProperty(RDF_collectionFormat, RdfCollectionFormat.class, ctx.collectionFormat);
		looseCollections = getProperty(RDF_looseCollections, boolean.class, ctx.looseCollections);
		useXmlNamespaces = getProperty(RDF_useXmlNamespaces, boolean.class, ctx.useXmlNamespaces);
		autoDetectNamespaces = getProperty(RDF_autoDetectNamespaces, boolean.class, ctx.autoDetectNamespaces);
		namespaces = getInstanceArrayProperty(RDF_namespaces, Namespace.class, ctx.namespaces);
		addBeanTypeProperties = getProperty(RDF_addBeanTypeProperties, boolean.class, ctx.addBeanTypeProperties);
		model = ModelFactory.createDefaultModel();
		addModelPrefix(juneauNs);
		addModelPrefix(juneauBpNs);
		for (Namespace ns : this.namespaces)
			addModelPrefix(ns);
		pRoot = model.createProperty(juneauNs.getUri(), RDF_juneauNs_ROOT);
		pValue = model.createProperty(juneauNs.getUri(), RDF_juneauNs_VALUE);
		writer = model.getWriter(rdfLanguage);

		// Only apply properties with this prefix!
		String propPrefix = RdfCommon.LANG_PROP_MAP.get(rdfLanguage);
		if (propPrefix == null)
			throw new FormattedRuntimeException("Unknown RDF language encountered: ''{0}''", rdfLanguage);

		// RDF/XML specific properties.
		if (propPrefix.equals("rdfXml.")) {
			writer.setProperty("tab", isUseWhitespace() ? 2 : 0);
			writer.setProperty("attributeQuoteChar", Character.toString(getQuoteChar()));
		}
		
		for (Map.Entry<String,Object> e : ctx.jenaSettings.entrySet())
			if (e.getKey().startsWith(propPrefix, 5))
				writer.setProperty(e.getKey().substring(5 + propPrefix.length()), e.getValue());
		
		for (String k : getPropertyKeys()) 
			if (k.startsWith("RdfCommon.jena.") && k.startsWith(propPrefix, 15))
				writer.setProperty(k.substring(15 + propPrefix.length()), getProperty(k));
	}

	@Override /* Session */
	public ObjectMap asMap() {
		return super.asMap()
			.append("RdfSerializerSession", new ObjectMap()
				.append("addBeanTypeProperties", addBeanTypeProperties)
				.append("addLiteralTypes", addLiteralTypes)
				.append("addRootProperty", addRootProperty)
				.append("autoDetectNamespaces", autoDetectNamespaces)
				.append("collectionFormat", collectionFormat)
				.append("juneauNs", juneauNs)
				.append("juneauBpNs", juneauBpNs)
				.append("looseCollections", looseCollections)
				.append("namespaces", namespaces)
				.append("rdfLanguage", rdfLanguage)
				.append("useXmlNamespaces", useXmlNamespaces)
			);
	}

	/*
	 * Adds the specified namespace as a model prefix.
	 */
	private void addModelPrefix(Namespace ns) {
		model.setNsPrefix(ns.getName(), ns.getUri());
	}

	/**
	 * Returns the {@link Serializer#SERIALIZER_addBeanTypeProperties} setting value for this session.
	 * 
	 * @return The {@link Serializer#SERIALIZER_addBeanTypeProperties} setting value for this session.
	 */
	@Override /* SerializerSession */
	public final boolean isAddBeanTypeProperties() {
		return addBeanTypeProperties;
	}

	/*
	 * XML-encodes the specified string using the {@link XmlUtils#escapeText(Object)} method.
	 */
	private String encodeTextInvalidChars(Object o) {
		if (o == null)
			return null;
		String s = toString(o);
		return XmlUtils.escapeText(s);
	}

	/*
	 * XML-encoded the specified element name using the {@link XmlUtils#encodeElementName(Object)} method.
	 */
	private String encodeElementName(Object o) {
		return XmlUtils.encodeElementName(toString(o));
	}
	
	@Override /* Serializer */
	protected void doSerialize(SerializerPipe out, Object o) throws Exception {

		Resource r = null;

		ClassMeta<?> cm = getClassMetaForObject(o);
		if (looseCollections && cm != null && cm.isCollectionOrArray()) {
			Collection c = sort(cm.isCollection() ? (Collection)o : toList(cm.getInnerClass(), o));
			for (Object o2 : c)
				serializeAnything(o2, false, object(), "root", null, null);
		} else {
			RDFNode n = serializeAnything(o, false, getExpectedRootType(o), "root", null, null);
			if (n.isLiteral()) {
				r = model.createResource();
				r.addProperty(pValue, n);
			} else {
				r = n.asResource();
			}

			if (addRootProperty)
				r.addProperty(pRoot, "true");
		}

		writer.write(model, out.getWriter(), "http://unknown/");
	}

	private RDFNode serializeAnything(Object o, boolean isURI, ClassMeta<?> eType, 
			String attrName, BeanPropertyMeta bpm, Resource parentResource) throws Exception {
		Model m = model;

		ClassMeta<?> aType = null;       // The actual type
		ClassMeta<?> wType = null;       // The wrapped type
		ClassMeta<?> sType = object();   // The serialized type

		aType = push(attrName, o, eType);

		if (eType == null)
			eType = object();

		// Handle recursion
		if (aType == null) {
			o = null;
			aType = object();
		}

		if (o != null) {

			if (aType.isDelegate()) {
				wType = aType;
				aType = ((Delegate)o).getClassMeta();
			}

			sType = aType;

			// Swap if necessary
			PojoSwap swap = aType.getPojoSwap(this);
			if (swap != null) {
				o = swap.swap(this, o);
				sType = swap.getSwapClassMeta(this);

				// If the getSwapClass() method returns Object, we need to figure out
				// the actual type now.
				if (sType.isObject())
					sType = getClassMetaForObject(o);
			}
		} else {
			sType = eType.getSerializedClassMeta(this);
		}

		String typeName = getBeanTypeName(eType, aType, bpm);

		RDFNode n = null;

		if (o == null || sType.isChar() && ((Character)o).charValue() == 0) {
			if (bpm != null) {
				if (! isTrimNulls()) {
					n = m.createResource(RDF_NIL);
				}
			} else {
				n = m.createResource(RDF_NIL);
			}

		} else if (sType.isUri() || isURI) {
			// Note that RDF URIs must be absolute to be valid!
			String uri = getUri(o, null);
			if (StringUtils.isAbsoluteUri(uri))
				n = m.createResource(uri);
			else
				n = m.createLiteral(encodeTextInvalidChars(uri));

		} else if (sType.isCharSequence() || sType.isChar()) {
			n = m.createLiteral(encodeTextInvalidChars(o));

		} else if (sType.isNumber() || sType.isBoolean()) {
			if (! addLiteralTypes)
				n = m.createLiteral(o.toString());
			else
				n = m.createTypedLiteral(o);

		} else if (sType.isMap() || (wType != null && wType.isMap())) {
			if (o instanceof BeanMap) {
				BeanMap bm = (BeanMap)o;
				Object uri = null;
				RdfBeanMeta rbm = (RdfBeanMeta)bm.getMeta().getExtendedMeta(RdfBeanMeta.class);
				if (rbm.hasBeanUri())
					uri = rbm.getBeanUriProperty().get(bm, null);
				String uri2 = getUri(uri, null);
				n = m.createResource(uri2);
				serializeBeanMap(bm, (Resource)n, typeName);
			} else {
				Map m2 = (Map)o;
				n = m.createResource();
				serializeMap(m2, (Resource)n, sType);
			}

		} else if (sType.isBean()) {
			BeanMap bm = toBeanMap(o);
			Object uri = null;
			RdfBeanMeta rbm = (RdfBeanMeta)bm.getMeta().getExtendedMeta(RdfBeanMeta.class);
			if (rbm.hasBeanUri())
				uri = rbm.getBeanUriProperty().get(bm, null);
			String uri2 = getUri(uri, null);
			n = m.createResource(uri2);
			serializeBeanMap(bm, (Resource)n, typeName);

		} else if (sType.isCollectionOrArray() || (wType != null && wType.isCollection())) {
			Collection c = sort(sType.isCollection() ? (Collection)o : toList(sType.getInnerClass(), o));
			RdfCollectionFormat f = collectionFormat;
			RdfClassMeta rcm = sType.getExtendedMeta(RdfClassMeta.class);
			if (rcm.getCollectionFormat() != RdfCollectionFormat.DEFAULT)
				f = rcm.getCollectionFormat();
			if (bpm != null && bpm.getExtendedMeta(RdfBeanPropertyMeta.class).getCollectionFormat() != RdfCollectionFormat.DEFAULT)
				f = bpm.getExtendedMeta(RdfBeanPropertyMeta.class).getCollectionFormat();
			switch (f) {
				case BAG: n = serializeToContainer(c, eType, m.createBag()); break;
				case LIST: n = serializeToList(c, eType); break;
				case MULTI_VALUED: serializeToMultiProperties(c, eType, bpm, attrName, parentResource); break;
				default: n = serializeToContainer(c, eType, m.createSeq());
			}
		
		} else if (sType.isReader() || sType.isInputStream()) {
			n = m.createLiteral(encodeTextInvalidChars(IOUtils.read(o)));
		
		} else {
			n = m.createLiteral(encodeTextInvalidChars(toString(o)));
		}

		pop();

		return n;
	}

	private String getUri(Object uri, Object uri2) {
		String s = null;
		if (uri != null)
			s = uri.toString();
		if ((s == null || s.isEmpty()) && uri2 != null)
			s = uri2.toString();
		if (s == null)
			return null;
		return getUriResolver().resolve(s);
	}

	private void serializeMap(Map m, Resource r, ClassMeta<?> type) throws Exception {

		m = sort(m);

		ClassMeta<?> keyType = type.getKeyType(), valueType = type.getValueType();

		ArrayList<Map.Entry<Object,Object>> l = new ArrayList<>(m.entrySet());
		Collections.reverse(l);
		for (Map.Entry<Object,Object> me : l) {
			Object value = me.getValue();

			Object key = generalize(me.getKey(), keyType);

			Namespace ns = juneauBpNs;
			Property p = model.createProperty(ns.getUri(), encodeElementName(toString(key)));
			RDFNode n = serializeAnything(value, false, valueType, toString(key), null, r);
			if (n != null)
				r.addProperty(p, n);
		}
	}

	private void serializeBeanMap(BeanMap<?> m, Resource r, String typeName) throws Exception {
		List<BeanPropertyValue> l = m.getValues(isTrimNulls(), typeName != null ? createBeanTypeNameProperty(m, typeName) : null);
		Collections.reverse(l);
		for (BeanPropertyValue bpv : l) {
			BeanPropertyMeta pMeta = bpv.getMeta();
			ClassMeta<?> cMeta = pMeta.getClassMeta();

			if (pMeta.getExtendedMeta(RdfBeanPropertyMeta.class).isBeanUri())
				continue;

			String key = bpv.getName();
			Object value = bpv.getValue();
			Throwable t = bpv.getThrown();
			if (t != null)
				onBeanGetterException(pMeta, t);

			if (canIgnoreValue(cMeta, key, value))
				continue;

			BeanPropertyMeta bpm = bpv.getMeta();
			Namespace ns = bpm.getExtendedMeta(RdfBeanPropertyMeta.class).getNamespace();
			if (ns == null && useXmlNamespaces)
				ns = bpm.getExtendedMeta(XmlBeanPropertyMeta.class).getNamespace();
			if (ns == null)
				ns = juneauBpNs;
			else if (autoDetectNamespaces)
				addModelPrefix(ns);

			Property p = model.createProperty(ns.getUri(), encodeElementName(key));
			RDFNode n = serializeAnything(value, pMeta.isUri(), cMeta, key, pMeta, r);
			if (n != null)
				r.addProperty(p, n);
		}
	}


	private Container serializeToContainer(Collection c, ClassMeta<?> type, Container list) throws Exception {

		ClassMeta<?> elementType = type.getElementType();
		for (Object e : c) {
			RDFNode n = serializeAnything(e, false, elementType, null, null, null);
			list = list.add(n);
		}
		return list;
	}

	private RDFList serializeToList(Collection c, ClassMeta<?> type) throws Exception {
		ClassMeta<?> elementType = type.getElementType();
		List<RDFNode> l = new ArrayList<>(c.size());
		for (Object e : c) {
			l.add(serializeAnything(e, false, elementType, null, null, null));
		}
		return model.createList(l.iterator());
	}

	private void serializeToMultiProperties(Collection c, ClassMeta<?> sType, 
			BeanPropertyMeta bpm, String attrName, Resource parentResource) throws Exception {
		ClassMeta<?> elementType = sType.getElementType();
		for (Object e : c) {
			Namespace ns = null;
			if (bpm != null) {
				ns = bpm.getExtendedMeta(RdfBeanPropertyMeta.class).getNamespace();
				if (ns == null && useXmlNamespaces)
					ns = bpm.getExtendedMeta(XmlBeanPropertyMeta.class).getNamespace();
			}
			if (ns == null)
				ns = juneauBpNs;
			else if (autoDetectNamespaces)
				addModelPrefix(ns);
			RDFNode n2 = serializeAnything(e, false, elementType, null, null, null);
			Property p = model.createProperty(ns.getUri(), encodeElementName(attrName));
			parentResource.addProperty(p, n2);
		}
	}
}
