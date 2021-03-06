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
package org.apache.juneau.yaml.proto;

import static org.apache.juneau.yaml.proto.YamlSerializer.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.serializer.*;

/**
 * Builder class for building instances of JSON serializers.
 */
public class YamlSerializerBuilder extends SerializerBuilder {

	/**
	 * Constructor, default settings.
	 */
	public YamlSerializerBuilder() {
		super();
	}

	/**
	 * Constructor.
	 * 
	 * @param ps The initial configuration settings for this builder.
	 */
	public YamlSerializerBuilder(PropertyStore ps) {
		super(ps);
	}

	@Override /* ContextBuilder */
	public YamlSerializer build() {
		return build(YamlSerializer.class);
	}


	//--------------------------------------------------------------------------------
	// Properties
	//--------------------------------------------------------------------------------

	/**
	 * Configuration property:  Prefix solidus <js>'/'</js> characters with escapes.
	 * 
	 * <p>
	 * If <jk>true</jk>, solidus (e.g. slash) characters should be escaped.
	 * The JSON specification allows for either format.
	 * However, if you're embedding JSON in an HTML script tag, this setting prevents confusion when trying to
	 * serialize <xt>&lt;\/script&gt;</xt>.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link YamlSerializer#YAML_escapeSolidus}
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public YamlSerializerBuilder escapeSolidus(boolean value) {
		return set(YAML_escapeSolidus, value);
	}

	/**
	 * Configuration property:  Simple JSON mode.
	 * 
	 * <p>
	 * If <jk>true</jk>, JSON attribute names will only be quoted when necessary.
	 * Otherwise, they are always quoted.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link YamlSerializer#YAML_simpleMode}
	 * </ul>
	 * 
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public YamlSerializerBuilder simple(boolean value) {
		return set(YAML_simpleMode, value);
	}

	/**
	 * Shortcut for calling <code>setSimpleMode(<jk>true</jk>).sq()</code>.
	 * 
	 * <h5 class='section'>See Also:</h5>
	 * <ul>
	 * 	<li class='jf'>{@link YamlSerializer#YAML_simpleMode}
	 * </ul>
	 * 
	 * @return This object (for method chaining).
	 */
	public YamlSerializerBuilder simple() {
		return simple(true).sq();
	}

	@Override /* SerializerBuilder */
	public YamlSerializerBuilder abridged(boolean value) {
		super.abridged(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public YamlSerializerBuilder abridged() {
		super.abridged();
		return this;
	}

	@Override /* SerializerBuilder */
	public YamlSerializerBuilder addBeanTypeProperties(boolean value) {
		super.addBeanTypeProperties(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public YamlSerializerBuilder detectRecursions(boolean value) {
		super.detectRecursions(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public YamlSerializerBuilder detectRecursions() {
		super.detectRecursions();
		return this;
	}

	@Override /* SerializerBuilder */
	public YamlSerializerBuilder ignoreRecursions(boolean value) {
		super.ignoreRecursions(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public YamlSerializerBuilder ignoreRecursions() {
		super.ignoreRecursions();
		return this;
	}
	@Override /* SerializerBuilder */
	public YamlSerializerBuilder initialDepth(int value) {
		super.initialDepth(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public YamlSerializerBuilder listener(Class<? extends SerializerListener> value) {
		super.listener(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public YamlSerializerBuilder maxDepth(int value) {
		super.maxDepth(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public YamlSerializerBuilder maxIndent(int value) {
		super.maxIndent(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public YamlSerializerBuilder quoteChar(char value) {
		super.quoteChar(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public YamlSerializerBuilder sortCollections(boolean value) {
		super.sortCollections(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public YamlSerializerBuilder sortCollections() {
		super.sortCollections();
		return this;
	}

	@Override /* SerializerBuilder */
	public YamlSerializerBuilder sortMaps(boolean value) {
		super.sortMaps(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public YamlSerializerBuilder sortMaps() {
		super.sortMaps();
		return this;
	}

	@Override /* SerializerBuilder */
	public YamlSerializerBuilder sq() {
		super.sq();
		return this;
	}

	@Override /* SerializerBuilder */
	public YamlSerializerBuilder trimEmptyCollections(boolean value) {
		super.trimEmptyCollections(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public YamlSerializerBuilder trimEmptyCollections() {
		super.trimEmptyCollections();
		return this;
	}

	@Override /* SerializerBuilder */
	public YamlSerializerBuilder trimEmptyMaps(boolean value) {
		super.trimEmptyMaps(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public YamlSerializerBuilder trimEmptyMaps() {
		super.trimEmptyMaps();
		return this;
	}

	@Override /* SerializerBuilder */
	public YamlSerializerBuilder trimNullProperties(boolean value) {
		super.trimNullProperties(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public YamlSerializerBuilder trimStrings(boolean value) {
		super.trimStrings(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public YamlSerializerBuilder trimStrings() {
		super.trimStrings();
		return this;
	}

	@Override /* SerializerBuilder */
	public YamlSerializerBuilder uriContext(UriContext value) {
		super.uriContext(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public YamlSerializerBuilder uriRelativity(UriRelativity value) {
		super.uriRelativity(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public YamlSerializerBuilder uriResolution(UriResolution value) {
		super.uriResolution(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public YamlSerializerBuilder useWhitespace(boolean value) {
		super.useWhitespace(value);
		return this;
	}

	@Override /* SerializerBuilder */
	public YamlSerializerBuilder useWhitespace() {
		super.useWhitespace();
		return this;
	}

	@Override /* SerializerBuilder */
	public YamlSerializerBuilder ws() {
		super.ws();
		return this;
	}

	@Override /* BeanContextBuilder */
	public YamlSerializerBuilder beansRequireDefaultConstructor(boolean value) {
		super.beansRequireDefaultConstructor(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public YamlSerializerBuilder beansRequireDefaultConstructor() {
		super.beansRequireDefaultConstructor();
		return this;
	}

	@Override /* BeanContextBuilder */
	public YamlSerializerBuilder beansRequireSerializable(boolean value) {
		super.beansRequireSerializable(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public YamlSerializerBuilder beansRequireSerializable() {
		super.beansRequireSerializable();
		return this;
	}

	@Override /* BeanContextBuilder */
	public YamlSerializerBuilder beansRequireSettersForGetters(boolean value) {
		super.beansRequireSettersForGetters(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public YamlSerializerBuilder beansRequireSettersForGetters() {
		super.beansRequireSettersForGetters();
		return this;
	}

	@Override /* BeanContextBuilder */
	public YamlSerializerBuilder beansRequireSomeProperties(boolean value) {
		super.beansRequireSomeProperties(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public YamlSerializerBuilder beanMapPutReturnsOldValue(boolean value) {
		super.beanMapPutReturnsOldValue(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public YamlSerializerBuilder beanMapPutReturnsOldValue() {
		super.beanMapPutReturnsOldValue();
		return this;
	}

	@Override /* BeanContextBuilder */
	public YamlSerializerBuilder beanConstructorVisibility(Visibility value) {
		super.beanConstructorVisibility(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public YamlSerializerBuilder beanClassVisibility(Visibility value) {
		super.beanClassVisibility(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public YamlSerializerBuilder beanFieldVisibility(Visibility value) {
		super.beanFieldVisibility(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public YamlSerializerBuilder beanMethodVisibility(Visibility value) {
		super.beanMethodVisibility(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public YamlSerializerBuilder useJavaBeanIntrospector(boolean value) {
		super.useJavaBeanIntrospector(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public YamlSerializerBuilder useJavaBeanIntrospector() {
		super.useJavaBeanIntrospector();
		return this;
	}

	@Override /* BeanContextBuilder */
	public YamlSerializerBuilder useInterfaceProxies(boolean value) {
		super.useInterfaceProxies(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public YamlSerializerBuilder ignoreUnknownBeanProperties(boolean value) {
		super.ignoreUnknownBeanProperties(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public YamlSerializerBuilder ignoreUnknownBeanProperties() {
		super.ignoreUnknownBeanProperties();
		return this;
	}

	@Override /* BeanContextBuilder */
	public YamlSerializerBuilder ignoreUnknownNullBeanProperties(boolean value) {
		super.ignoreUnknownNullBeanProperties(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public YamlSerializerBuilder ignorePropertiesWithoutSetters(boolean value) {
		super.ignorePropertiesWithoutSetters(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public YamlSerializerBuilder ignoreInvocationExceptionsOnGetters(boolean value) {
		super.ignoreInvocationExceptionsOnGetters(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public YamlSerializerBuilder ignoreInvocationExceptionsOnGetters() {
		super.ignoreInvocationExceptionsOnGetters();
		return this;
	}

	@Override /* BeanContextBuilder */
	public YamlSerializerBuilder ignoreInvocationExceptionsOnSetters(boolean value) {
		super.ignoreInvocationExceptionsOnSetters(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public YamlSerializerBuilder ignoreInvocationExceptionsOnSetters() {
		super.ignoreInvocationExceptionsOnSetters();
		return this;
	}

	@Override /* BeanContextBuilder */
	public YamlSerializerBuilder sortProperties(boolean value) {
		super.sortProperties(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public YamlSerializerBuilder sortProperties() {
		super.sortProperties();
		return this;
	}

	@Override /* BeanContextBuilder */
	public YamlSerializerBuilder notBeanPackages(Object...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public YamlSerializerBuilder notBeanPackages(String...values) {
		super.notBeanPackages(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public YamlSerializerBuilder notBeanPackages(boolean append, Object...values) {
		super.notBeanPackages(append, values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public YamlSerializerBuilder notBeanPackagesRemove(Object...values) {
		super.notBeanPackagesRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public YamlSerializerBuilder notBeanClasses(Object...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public YamlSerializerBuilder notBeanClasses(Class<?>...values) {
		super.notBeanClasses(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public YamlSerializerBuilder notBeanClasses(boolean append, Object...values) {
		super.notBeanClasses(append, values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public YamlSerializerBuilder notBeanClassesRemove(Object...values) {
		super.notBeanClassesRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public YamlSerializerBuilder beanFilters(Object...values) {
		super.beanFilters(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public YamlSerializerBuilder beanFilters(Class<?>...values) {
		super.beanFilters(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public YamlSerializerBuilder beanFilters(boolean append, Object...values) {
		super.beanFilters(append, values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public YamlSerializerBuilder beanFiltersRemove(Object...values) {
		super.beanFiltersRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public YamlSerializerBuilder pojoSwaps(Object...values) {
		super.pojoSwaps(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public YamlSerializerBuilder pojoSwaps(Class<?>...values) {
		super.pojoSwaps(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public YamlSerializerBuilder pojoSwaps(boolean append, Object...values) {
		super.pojoSwaps(append, values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public YamlSerializerBuilder pojoSwapsRemove(Object...values) {
		super.pojoSwapsRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public YamlSerializerBuilder implClasses(Map<String,Class<?>> values) {
		super.implClasses(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public <T> YamlSerializerBuilder implClass(Class<T> interfaceClass, Class<? extends T> implClass) {
		super.implClass(interfaceClass, implClass);
		return this;
	}

	@Override /* BeanContextBuilder */
	public YamlSerializerBuilder beanDictionary(Object...values) {
		super.beanDictionary(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public YamlSerializerBuilder beanDictionary(Class<?>...values) {
		super.beanDictionary(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public YamlSerializerBuilder beanDictionary(boolean append, Object...values) {
		super.beanDictionary(append, values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public YamlSerializerBuilder beanDictionaryRemove(Object...values) {
		super.beanDictionaryRemove(values);
		return this;
	}

	@Override /* BeanContextBuilder */
	public YamlSerializerBuilder beanTypePropertyName(String value) {
		super.beanTypePropertyName(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public YamlSerializerBuilder locale(Locale value) {
		super.locale(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public YamlSerializerBuilder timeZone(TimeZone value) {
		super.timeZone(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public YamlSerializerBuilder mediaType(MediaType value) {
		super.mediaType(value);
		return this;
	}

	@Override /* BeanContextBuilder */
	public YamlSerializerBuilder debug() {
		super.debug();
		return this;
	}

	@Override /* ContextBuilder */
	public YamlSerializerBuilder set(String name, Object value) {
		super.set(name, value);
		return this;
	}

	@Override /* ContextBuilder */
	public YamlSerializerBuilder set(boolean append, String name, Object value) {
		super.set(append, name, value);
		return this;
	}

	@Override /* ContextBuilder */
	public YamlSerializerBuilder set(Map<String,Object> properties) {
		super.set(properties);
		return this;
	}

	@Override /* ContextBuilder */
	public YamlSerializerBuilder add(Map<String,Object> properties) {
		super.add(properties);
		return this;
	}

	@Override /* ContextBuilder */
	public YamlSerializerBuilder addTo(String name, Object value) {
		super.addTo(name, value);
		return this;
	}

	@Override /* ContextBuilder */
	public YamlSerializerBuilder addTo(String name, String key, Object value) {
		super.addTo(name, key, value);
		return this;
	}

	@Override /* ContextBuilder */
	public YamlSerializerBuilder removeFrom(String name, Object value) {
		super.removeFrom(name, value);
		return this;
	}

	@Override /* ContextBuilder */
	public YamlSerializerBuilder apply(PropertyStore copyFrom) {
		super.apply(copyFrom);
		return this;
	}
}