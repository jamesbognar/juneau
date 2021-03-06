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
package org.apache.juneau.dto.swagger;

import static org.apache.juneau.internal.BeanPropertyUtils.*;
import org.apache.juneau.annotation.*;

/**
 * The object provides metadata about the API. The metadata can be used by the clients if needed, and can be presented
 * in the Swagger-UI for convenience.
 * 
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	<jc>// Construct using SwaggerBuilder.</jc>
 * 	Info x = <jsm>info</jsm>(<js>"Swagger Sample App"</js>, <js>"1.0.1"</js>)
 * 		.description(<js>"This is a sample server Petstore server."</js>)
 * 		.termsOfService(<js>"http://swagger.io/terms/"</js>)
 * 		.contact(
 * 			<jsm>contact</jsm>(<js>"API Support"</js>, <js>"http://www.swagger.io/support"</js>, <js>"support@swagger.io"</js>)
 * 		)
 * 		.license(
 * 			<jsm>license</jsm>(<js>"Apache 2.0"</js>, <js>"http://www.apache.org/licenses/LICENSE-2.0.html"</js>)
 * 		);
 * 
 * 	<jc>// Serialize using JsonSerializer.</jc>
 * 	String json = JsonSerializer.<jsf>DEFAULT</jsf>.toString(x);
 * 
 * 	<jc>// Or just use toString() which does the same as above.</jc>
 * 	String json = x.toString();
 * </p>
 * <p class='bcode'>
 * 	<jc>// Output</jc>
 * 	{
 * 		<js>"title"</js>: <js>"Swagger Sample App"</js>,
 * 		<js>"description"</js>: <js>"This is a sample server Petstore server."</js>,
 * 		<js>"termsOfService"</js>: <js>"http://swagger.io/terms/"</js>,
 * 		<js>"contact"</js>: {
 * 			<js>"name"</js>: <js>"API Support"</js>,
 * 			<js>"url"</js>: <js>"http://www.swagger.io/support"</js>,
 * 			<js>"email"</js>: <js>"support@swagger.io"</js>
 * 		},
 * 		<js>"license"</js>: {
 * 			<js>"name"</js>: <js>"Apache 2.0"</js>,
 * 			<js>"url"</js>: <js>"http://www.apache.org/licenses/LICENSE-2.0.html"</js>
 * 		},
 * 		<js>"version"</js>: <js>"1.0.1"</js>
 * 	}
 * </p>
 * 
 * <h6 class='topic'>Additional Information</h6>
 * <ul class='doctree'>
 * 	<li class='link'><a class='doclink' href='../../../../../overview-summary.html#juneau-dto.Swagger'>Overview > juneau-dto > Swagger</a>
 * </ul>
 */
@Bean(properties="title,description,termsOfService,contact,license,version,*")
public class Info extends SwaggerElement {

	private String 
		title,
		description,
		termsOfService,
		version;
	private Contact contact;
	private License license;

	/**
	 * Bean property getter:  <property>title</property>.
	 * 
	 * <p>
	 * The title of the application.
	 * 
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Bean property setter:  <property>title</property>.
	 * 
	 * <p>
	 * The title of the application.
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>Property value is required.
	 * @return This object (for method chaining).
	 */
	public Info setTitle(String value) {
		title = value;
		return this;
	}

	/**
	 * Same as {@link #setTitle(String)}.
	 * 
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-String values will be converted to String using <code>toString()</code>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public Info title(Object value) {
		return setTitle(toStringVal(value));
	}

	/**
	 * Bean property getter:  <property>description</property>.
	 * 
	 * <p>
	 * A short description of the application. 
	 * 
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Bean property setter:  <property>description</property>.
	 * 
	 * <p>
	 * A short description of the application. 
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br><a class="doclink" href="https://help.github.com/articles/github-flavored-markdown">GFM syntax</a> can be used for rich text representation.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public Info setDescription(String value) {
		description = value;
		return this;
	}

	/**
	 * Same as {@link #setDescription(String)}.
	 * 
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-String values will be converted to String using <code>toString()</code>.
	 * 	<br><a class="doclink" href="https://help.github.com/articles/github-flavored-markdown">GFM syntax</a> can be used for rich text representation.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public Info description(Object value) {
		return setDescription(toStringVal(value));
	}

	/**
	 * Bean property getter:  <property>termsOfService</property>.
	 * 
	 * <p>
	 * The Terms of Service for the API.
	 * 
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getTermsOfService() {
		return termsOfService;
	}

	/**
	 * Bean property setter:  <property>termsOfService</property>.
	 * 
	 * <p>
	 * The Terms of Service for the API.
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public Info setTermsOfService(String value) {
		termsOfService = value;
		return this;
	}

	/**
	 * Same as {@link #setTermsOfService(String)}.
	 * 
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-String values will be converted to String using <code>toString()</code>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public Info termsOfService(Object value) {
		return setTermsOfService(toStringVal(value));
	}

	/**
	 * Bean property getter:  <property>contact</property>.
	 * 
	 * <p>
	 * The contact information for the exposed API.
	 * 
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public Contact getContact() {
		return contact;
	}

	/**
	 * Bean property setter:  <property>contact</property>.
	 * 
	 * <p>
	 * The contact information for the exposed API.
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public Info setContact(Contact value) {
		contact = value;
		return this;
	}

	/**
	 * Same as {@link #setContact(Contact)}.
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>Valid types:
	 * 	<ul>
	 * 		<li>{@link Contact}
	 * 		<li><code>String</code> - JSON object representation of {@link Contact}
	 * 			<h6 class='figure'>Example:</h6>
	 * 			<p class='bcode'>
	 * 	contact(<js>"{name:'name',url:'url',...}"</js>);
	 * 			</p>
	 * 	</ul>
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public Info contact(Object value) {
		return setContact(toType(value, Contact.class));
	}

	/**
	 * Bean property getter:  <property>license</property>.
	 * 
	 * <p>
	 * The license information for the exposed API.
	 * 
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public License getLicense() {
		return license;
	}

	/**
	 * Bean property setter:  <property>license</property>.
	 * 
	 * <p>
	 * The license information for the exposed API.
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public Info setLicense(License value) {
		license = value;
		return this;
	}

	/**
	 * Same as {@link #setLicense(License)}.
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>Valid types:
	 * 	<ul>
	 * 		<li>{@link License}
	 * 		<li><code>String</code> - JSON object representation of {@link License}
	 * 			<h6 class='figure'>Example:</h6>
	 * 			<p class='bcode'>
	 * 	license(<js>"{name:'name',url:'url',...}"</js>);
	 * 			</p>
	 * 	</ul>
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public Info license(Object value) {
		return setLicense(toType(value, License.class));
	}

	/**
	 * Bean property getter:  <property>version</property>.
	 * 
	 * <p>
	 * Provides the version of the application API (not to be confused with the specification version).
	 * 
	 * @return The property value, or <jk>null</jk> if it is not set.
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Bean property setter:  <property>version</property>.
	 * 
	 * <p>
	 * Provides the version of the application API (not to be confused with the specification version).
	 * 
	 * @param value 
	 * 	The new value for this property.
	 * 	<br>Property value is required.
	 * @return This object (for method chaining).
	 */
	public Info setVersion(String value) {
		version = value;
		return this;
	}

	/**
	 * Same as {@link #setVersion(String)}.
	 * 
	 * @param value
	 * 	The new value for this property.
	 * 	<br>Non-String values will be converted to String using <code>toString()</code>.
	 * 	<br>Can be <jk>null</jk> to unset the property.
	 * @return This object (for method chaining).
	 */
	public Info version(Object value) {
		return setVersion(toStringVal(value));
	}

	@Override /* SwaggerElement */
	public <T> T get(String property, Class<T> type) {
		if (property == null)
			return null;
		switch (property) {
			case "title": return toType(getTitle(), type);
			case "description": return toType(getDescription(), type);
			case "termsOfService": return toType(getTermsOfService(), type);
			case "contact": return toType(getContact(), type);
			case "license": return toType(getLicense(), type);
			case "version": return toType(getVersion(), type);
			default: return super.get(property, type);
		}
	}

	@Override /* SwaggerElement */
	public Info set(String property, Object value) {
		if (property == null)
			return this;
		switch (property) {
			case "title": return title(value);
			case "description": return description(value);
			case "termsOfService": return termsOfService(value);
			case "contact": return contact(value);
			case "license": return license(value);
			case "version": return version(value);
			default: 
				super.set(property, value);
				return this;
		}
	}
}
