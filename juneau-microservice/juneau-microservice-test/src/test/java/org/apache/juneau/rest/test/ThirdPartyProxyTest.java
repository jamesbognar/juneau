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
package org.apache.juneau.rest.test;

import static org.apache.juneau.rest.test.TestUtils.*;
import static org.apache.juneau.rest.test.pojos.Constants.*;
import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.html.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.jena.*;
import org.apache.juneau.json.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.remoteable.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.test.ThirdPartyProxyTest.ThirdPartyProxy.*;
import org.apache.juneau.rest.test.pojos.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.utils.*;
import org.apache.juneau.xml.*;
import org.junit.*;
import org.junit.runner.*;
import org.junit.runners.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(Parameterized.class)
public class ThirdPartyProxyTest extends RestTestcase {

	@Parameterized.Parameters
	public static Collection<Object[]> getParameters() {
		return Arrays.asList(new Object[][] {
			{ /* 0 */ "Json", JsonSerializer.DEFAULT, JsonParser.DEFAULT },
			{ /* 1 */ "Xml", XmlSerializer.DEFAULT, XmlParser.DEFAULT },
			{ /* 2 */ "Mixed", JsonSerializer.DEFAULT, XmlParser.DEFAULT },
			{ /* 3 */ "Html", HtmlSerializer.DEFAULT, HtmlParser.DEFAULT },
			{ /* 4 */ "MessagePack", MsgPackSerializer.DEFAULT, MsgPackParser.DEFAULT },
			{ /* 5 */ "UrlEncoding", UrlEncodingSerializer.DEFAULT, UrlEncodingParser.DEFAULT },
			{ /* 6 */ "Uon", UonSerializer.DEFAULT, UonParser.DEFAULT },
			{ /* 7 */ "RdfXml", RdfSerializer.DEFAULT_XMLABBREV, RdfParser.DEFAULT_XML },
		});
	}

	private ThirdPartyProxy proxy;

	public ThirdPartyProxyTest(String label, Serializer serializer, Parser parser) {
		proxy = getCached(label, ThirdPartyProxy.class);
		if (proxy == null) {
			this.proxy = getClient(label, serializer, parser).builder().partSerializer(UonPartSerializer.class).build().getRemoteableProxy(ThirdPartyProxy.class, null, serializer, parser);
			cache(label, proxy);
		}
	}

	//--------------------------------------------------------------------------------
	// Temporary exhaustive test.
	//--------------------------------------------------------------------------------

	@Test
	@Ignore
	public void a00_lotsOfSetInt3dArray() {
		final AtomicLong time = new AtomicLong(System.currentTimeMillis());
		final AtomicInteger iteration = new AtomicInteger(0);
      TimerTask timerTask = new TimerTask() {
			@Override
			public void run() {
				if (System.currentTimeMillis() - time.get() > 10000) {
					try {
						System.err.println("Failed at iteration " + iteration.get());
						TestMicroservice.jettyDump(null, null);
						System.exit(2);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
      };
      // running timer task as daemon thread
      Timer timer = new Timer(true);
      timer.scheduleAtFixedRate(timerTask, 0, 10 * 1000);
		for (int i = 0; i < 100000; i++) {
			iteration.set(i);
			String s = proxy.setInt3dArray(new int[][][]{{{i},null},null}, i);
			if (i % 1000 == 0)
				System.err.println("response="+s);
			time.set(System.currentTimeMillis());
		}
      timer.cancel();
	}



	//--------------------------------------------------------------------------------
	// Header tests
	//--------------------------------------------------------------------------------

	@Test
	public void a01_primitiveHeaders() throws Exception {
		String r = proxy.primitiveHeaders(
			"foo",
			null,
			123,
			123,
			null,
			true,
			1.0f,
			1.0f
		);
		assertEquals("OK", r);
	}

	@Test
	public void a02_primitiveCollectionHeaders() throws Exception {
		String r = proxy.primitiveCollectionHeaders(
			new int[][][]{{{1,2},null},null},
			new Integer[][][]{{{1,null},null},null},
			new String[][][]{{{"foo",null},null},null},
			new AList<Integer>().append(1).append(null),
			new AList<List<List<Integer>>>()
				.append(
					new AList<List<Integer>>()
					.append(new AList<Integer>().append(1).append(null))
					.append(null)
				)
				.append(null)
			,
			new AList<Integer[][][]>().append(new Integer[][][]{{{1,null},null},null}).append(null),
			new AList<int[][][]>().append(new int[][][]{{{1,2},null},null}).append(null),
			Arrays.asList("foo","bar",null)
		);
		assertEquals("OK", r);
	}

	@Test
	public void a03_beanHeaders() throws Exception {
		String r = proxy.beanHeaders(
			new ABean().init(),
			null,
			new ABean[][][]{{{new ABean().init(),null},null},null},
			new AList<ABean>().append(new ABean().init()).append(null),
			new AList<ABean[][][]>().append(new ABean[][][]{{{new ABean().init(),null},null},null}).append(null),
			new AMap<String,ABean>().append("foo",new ABean().init()),
			new AMap<String,List<ABean>>().append("foo",Arrays.asList(new ABean().init())),
			new AMap<String,List<ABean[][][]>>().append("foo",new AList<ABean[][][]>().append(new ABean[][][]{{{new ABean().init(),null},null},null}).append(null)),
			new AMap<Integer,List<ABean>>().append(1,Arrays.asList(new ABean().init()))
		);
		assertEquals("OK", r);
	}


	@Test
	public void a04_typedBeanHeaders() throws Exception {
		String r = proxy.typedBeanHeaders(
			new TypedBeanImpl().init(),
			null,
			new TypedBean[][][]{{{new TypedBeanImpl().init(),null},null},null},
			new AList<TypedBean>().append(new TypedBeanImpl().init()).append(null),
			new AList<TypedBean[][][]>().append(new TypedBean[][][]{{{new TypedBeanImpl().init(),null},null},null}).append(null),
			new AMap<String,TypedBean>().append("foo",new TypedBeanImpl().init()),
			new AMap<String,List<TypedBean>>().append("foo",Arrays.asList((TypedBean)new TypedBeanImpl().init())),
			new AMap<String,List<TypedBean[][][]>>().append("foo",new AList<TypedBean[][][]>().append(new TypedBean[][][]{{{new TypedBeanImpl().init(),null},null},null}).append(null)),
			new AMap<Integer,List<TypedBean>>().append(1,Arrays.asList((TypedBean)new TypedBeanImpl().init()))
		);
		assertEquals("OK", r);
	}

	@Test
	public void a05_swappedPojoHeaders() throws Exception {
		String r = proxy.swappedPojoHeaders(
			new SwappedPojo(),
			new SwappedPojo[][][]{{{new SwappedPojo(),null},null},null},
			new AMap<SwappedPojo,SwappedPojo>().append(new SwappedPojo(), new SwappedPojo()),
			new AMap<SwappedPojo,SwappedPojo[][][]>().append(new SwappedPojo(), new SwappedPojo[][][]{{{new SwappedPojo(),null},null},null})
		);
		assertEquals("OK", r);
	}

	@Test
	public void a06_implicitSwappedPojoHeaders() throws Exception {
		String r = proxy.implicitSwappedPojoHeaders(
			new ImplicitSwappedPojo(),
			new ImplicitSwappedPojo[][][]{{{new ImplicitSwappedPojo(),null},null},null},
			new AMap<ImplicitSwappedPojo,ImplicitSwappedPojo>().append(new ImplicitSwappedPojo(), new ImplicitSwappedPojo()),
			new AMap<ImplicitSwappedPojo,ImplicitSwappedPojo[][][]>().append(new ImplicitSwappedPojo(), new ImplicitSwappedPojo[][][]{{{new ImplicitSwappedPojo(),null},null},null})
		);
		assertEquals("OK", r);
	}

	@Test
	public void a07_enumHeaders() throws Exception {
		String r = proxy.enumHeaders(
			TestEnum.TWO,
			null,
			new TestEnum[][][]{{{TestEnum.TWO,null},null},null},
			new AList<TestEnum>().append(TestEnum.TWO).append(null),
			new AList<List<List<TestEnum>>>()
				.append(
					new AList<List<TestEnum>>()
						.append(
							new AList<TestEnum>().append(TestEnum.TWO).append(null)
						)
					.append(null)
				).append(null),
			new AList<TestEnum[][][]>().append(new TestEnum[][][]{{{TestEnum.TWO,null},null},null}).append(null),
			new AMap<TestEnum,TestEnum>().append(TestEnum.ONE,TestEnum.TWO),
			new AMap<TestEnum,TestEnum[][][]>().append(TestEnum.ONE, new TestEnum[][][]{{{TestEnum.TWO,null},null},null}),
			new AMap<TestEnum,List<TestEnum[][][]>>().append(TestEnum.ONE, new AList<TestEnum[][][]>().append(new TestEnum[][][]{{{TestEnum.TWO,null},null},null}).append(null))
		);
		assertEquals("OK", r);
	}

	@Test
	public void a08_mapHeader() throws Exception {
		String r = proxy.mapHeader(
			new AMap<String,Object>().append("a", "foo").append("b", "").append("c", null)
		);
		assertEquals("OK", r);
	}

	@Test
	public void a09_beanHeader() throws Exception {
		String r = proxy.beanHeader(
			new NeBean().init()
		);
		assertEquals("OK", r);
	}

	@Test
	public void a10_nameValuePairsHeader() throws Exception {
		String r = proxy.nameValuePairsHeader(
			new NameValuePairs().append("a", "foo").append("b", "").append("c", null)
		);
		assertEquals("OK", r);
	}

	@Test
	public void a11_headerIfNE1() throws Exception {
		String r = proxy.headerIfNE1(
			"foo"
		);
		assertEquals("OK", r);
	}

	@Test
	public void a12_headerIfNE2() throws Exception {
		String r = proxy.headerIfNE2(
			null
		);
		assertEquals("OK", r);
	}

	@Test
	public void a13_headerIfNEMap() throws Exception {
		String r = proxy.headerIfNEMap(
			new AMap<String,Object>().append("a", "foo").append("b", "").append("c", null)
		);
		assertEquals("OK", r);
	}

	@Test
	public void a14_headerIfNEBean() throws Exception {
		String r = proxy.headerIfNEBean(
			new NeBean().init()
		);
		assertEquals("OK", r);
	}

	@Test
	public void a15_headerIfNEnameValuePairs() throws Exception {
		String r = proxy.headerIfNEnameValuePairs(
			new NameValuePairs().append("a", "foo").append("b", "").append("c", null)
		);
		assertEquals("OK", r);
	}


	//--------------------------------------------------------------------------------
	// Query tests
	//--------------------------------------------------------------------------------

	@Test
	public void b01_primitiveQueries() throws Exception {
		String r = proxy.primitiveQueries(
			"foo",
			null,
			123,
			123,
			null,
			true,
			1.0f,
			1.0f
		);
		assertEquals("OK", r);
	}

	@Test
	public void b02_primitiveCollectionQueries() throws Exception {
		String r = proxy.primitiveCollectionQueries(
			new int[][][]{{{1,2},null},null},
			new Integer[][][]{{{1,null},null},null},
			new String[][][]{{{"foo",null},null},null},
			new AList<Integer>().append(1).append(null),
			new AList<List<List<Integer>>>()
				.append(
					new AList<List<Integer>>()
					.append(new AList<Integer>().append(1).append(null))
					.append(null)
				)
				.append(null)
			,
			new AList<Integer[][][]>().append(new Integer[][][]{{{1,null},null},null}).append(null),
			new AList<int[][][]>().append(new int[][][]{{{1,2},null},null}).append(null),
			Arrays.asList("foo","bar",null)
		);
		assertEquals("OK", r);
	}

	@Test
	public void b03_beanQueries() throws Exception {
		String r = proxy.beanQueries(
			new ABean().init(),
			null,
			new ABean[][][]{{{new ABean().init(),null},null},null},
			new AList<ABean>().append(new ABean().init()).append(null),
			new AList<ABean[][][]>().append(new ABean[][][]{{{new ABean().init(),null},null},null}).append(null),
			new AMap<String,ABean>().append("foo",new ABean().init()),
			new AMap<String,List<ABean>>().append("foo",Arrays.asList(new ABean().init())),
			new AMap<String,List<ABean[][][]>>().append("foo",new AList<ABean[][][]>().append(new ABean[][][]{{{new ABean().init(),null},null},null}).append(null)),
			new AMap<Integer,List<ABean>>().append(1,Arrays.asList(new ABean().init()))
		);
		assertEquals("OK", r);
	}


	@Test
	public void b04_typedBeanQueries() throws Exception {
		String r = proxy.typedBeanQueries(
			new TypedBeanImpl().init(),
			null,
			new TypedBean[][][]{{{new TypedBeanImpl().init(),null},null},null},
			new AList<TypedBean>().append(new TypedBeanImpl().init()).append(null),
			new AList<TypedBean[][][]>().append(new TypedBean[][][]{{{new TypedBeanImpl().init(),null},null},null}).append(null),
			new AMap<String,TypedBean>().append("foo",new TypedBeanImpl().init()),
			new AMap<String,List<TypedBean>>().append("foo",Arrays.asList((TypedBean)new TypedBeanImpl().init())),
			new AMap<String,List<TypedBean[][][]>>().append("foo",new AList<TypedBean[][][]>().append(new TypedBean[][][]{{{new TypedBeanImpl().init(),null},null},null}).append(null)),
			new AMap<Integer,List<TypedBean>>().append(1,Arrays.asList((TypedBean)new TypedBeanImpl().init()))
		);
		assertEquals("OK", r);
	}

	@Test
	public void b05_swappedPojoQueries() throws Exception {
		String r = proxy.swappedPojoQueries(
			new SwappedPojo(),
			new SwappedPojo[][][]{{{new SwappedPojo(),null},null},null},
			new AMap<SwappedPojo,SwappedPojo>().append(new SwappedPojo(), new SwappedPojo()),
			new AMap<SwappedPojo,SwappedPojo[][][]>().append(new SwappedPojo(), new SwappedPojo[][][]{{{new SwappedPojo(),null},null},null})
		);
		assertEquals("OK", r);
	}

	@Test
	public void b06_implicitSwappedPojoQueries() throws Exception {
		String r = proxy.implicitSwappedPojoQueries(
			new ImplicitSwappedPojo(),
			new ImplicitSwappedPojo[][][]{{{new ImplicitSwappedPojo(),null},null},null},
			new AMap<ImplicitSwappedPojo,ImplicitSwappedPojo>().append(new ImplicitSwappedPojo(), new ImplicitSwappedPojo()),
			new AMap<ImplicitSwappedPojo,ImplicitSwappedPojo[][][]>().append(new ImplicitSwappedPojo(), new ImplicitSwappedPojo[][][]{{{new ImplicitSwappedPojo(),null},null},null})
		);
		assertEquals("OK", r);
	}

	@Test
	public void b07_enumQueries() throws Exception {
		String r = proxy.enumQueries(
			TestEnum.TWO,
			null,
			new TestEnum[][][]{{{TestEnum.TWO,null},null},null},
			new AList<TestEnum>().append(TestEnum.TWO).append(null),
			new AList<List<List<TestEnum>>>()
				.append(
					new AList<List<TestEnum>>()
						.append(
							new AList<TestEnum>().append(TestEnum.TWO).append(null)
						)
					.append(null)
				).append(null),
			new AList<TestEnum[][][]>().append(new TestEnum[][][]{{{TestEnum.TWO,null},null},null}).append(null),
			new AMap<TestEnum,TestEnum>().append(TestEnum.ONE,TestEnum.TWO),
			new AMap<TestEnum,TestEnum[][][]>().append(TestEnum.ONE, new TestEnum[][][]{{{TestEnum.TWO,null},null},null}),
			new AMap<TestEnum,List<TestEnum[][][]>>().append(TestEnum.ONE, new AList<TestEnum[][][]>().append(new TestEnum[][][]{{{TestEnum.TWO,null},null},null}).append(null))
		);
		assertEquals("OK", r);
	}

	@Test
	public void b08_stringQuery1() throws Exception {
		String r = proxy.stringQuery1("a=1&b=foo");
		assertEquals("OK", r);
	}

	@Test
	public void b09_stringQuery2() throws Exception {
		String r = proxy.stringQuery2("a=1&b=foo");
		assertEquals("OK", r);
	}

	@Test
	public void b10_mapQuery() throws Exception {
		String r = proxy.mapQuery(
			new AMap<String,Object>().append("a", 1).append("b", "foo")
		);
		assertEquals("OK", r);
	}

	@Test
	public void b11_beanQuery() throws Exception {
		String r = proxy.beanQuery(
			new NeBean().init()
		);
		assertEquals("OK", r);
	}

	@Test
	public void b12_nameValuePairsQuery() throws Exception {
		String r = proxy.nameValuePairsQuery(
			new NameValuePairs().append("a", "foo").append("b", "").append("c", null)
		);
		assertEquals("OK", r);
	}

	@Test
	public void b13_queryIfNE1() throws Exception {
		String r = proxy.queryIfNE1(
			"foo"
		);
		assertEquals("OK", r);
	}

	@Test
	public void b14_queryIfNE2() throws Exception {
		String r = proxy.queryIfNE2(
			null
		);
		assertEquals("OK", r);
	}

	@Test
	public void b15_queryIfNEMap() throws Exception {
		String r = proxy.queryIfNEMap(
			new AMap<String,Object>().append("a", "foo").append("b", "").append("c", null)
		);
		assertEquals("OK", r);
	}

	@Test
	public void b16_queryIfNEBean() throws Exception {
		String r = proxy.queryIfNEBean(
			new NeBean().init()
		);
		assertEquals("OK", r);
	}

	@Test
	public void b17_queryIfNEnameValuePairs() throws Exception {
		String r = proxy.queryIfNEnameValuePairs(
			new NameValuePairs().append("a", "foo").append("b", "").append("c", null)
		);
		assertEquals("OK", r);
	}

	//--------------------------------------------------------------------------------
	// FormData tests
	//--------------------------------------------------------------------------------

	@Test
	public void c01_primitiveFormData() throws Exception {
		String r = proxy.primitiveFormData(
			"foo",
			null,
			123,
			123,
			null,
			true,
			1.0f,
			1.0f
		);
		assertEquals("OK", r);
	}

	@Test
	public void c02_primitiveCollectionFormData() throws Exception {
		String r = proxy.primitiveCollectionFormData(
			new int[][][]{{{1,2},null},null},
			new Integer[][][]{{{1,null},null},null},
			new String[][][]{{{"foo",null},null},null},
			new AList<Integer>().append(1).append(null),
			new AList<List<List<Integer>>>()
				.append(
					new AList<List<Integer>>()
					.append(new AList<Integer>().append(1).append(null))
					.append(null)
				)
				.append(null)
			,
			new AList<Integer[][][]>().append(new Integer[][][]{{{1,null},null},null}).append(null),
			new AList<int[][][]>().append(new int[][][]{{{1,2},null},null}).append(null),
			Arrays.asList("foo","bar",null)
		);
		assertEquals("OK", r);
	}

	@Test
	public void c03_beanFormData() throws Exception {
		String r = proxy.beanFormData(
			new ABean().init(),
			null,
			new ABean[][][]{{{new ABean().init(),null},null},null},
			new AList<ABean>().append(new ABean().init()).append(null),
			new AList<ABean[][][]>().append(new ABean[][][]{{{new ABean().init(),null},null},null}).append(null),
			new AMap<String,ABean>().append("foo",new ABean().init()),
			new AMap<String,List<ABean>>().append("foo",Arrays.asList(new ABean().init())),
			new AMap<String,List<ABean[][][]>>().append("foo",new AList<ABean[][][]>().append(new ABean[][][]{{{new ABean().init(),null},null},null}).append(null)),
			new AMap<Integer,List<ABean>>().append(1,Arrays.asList(new ABean().init()))
		);
		assertEquals("OK", r);
	}


	@Test
	public void c04_typedBeanFormData() throws Exception {
		String r = proxy.typedBeanFormData(
			new TypedBeanImpl().init(),
			null,
			new TypedBean[][][]{{{new TypedBeanImpl().init(),null},null},null},
			new AList<TypedBean>().append(new TypedBeanImpl().init()).append(null),
			new AList<TypedBean[][][]>().append(new TypedBean[][][]{{{new TypedBeanImpl().init(),null},null},null}).append(null),
			new AMap<String,TypedBean>().append("foo",new TypedBeanImpl().init()),
			new AMap<String,List<TypedBean>>().append("foo",Arrays.asList((TypedBean)new TypedBeanImpl().init())),
			new AMap<String,List<TypedBean[][][]>>().append("foo",new AList<TypedBean[][][]>().append(new TypedBean[][][]{{{new TypedBeanImpl().init(),null},null},null}).append(null)),
			new AMap<Integer,List<TypedBean>>().append(1,Arrays.asList((TypedBean)new TypedBeanImpl().init()))
		);
		assertEquals("OK", r);
	}

	@Test
	public void c05_swappedPojoFormData() throws Exception {
		String r = proxy.swappedPojoFormData(
			new SwappedPojo(),
			new SwappedPojo[][][]{{{new SwappedPojo(),null},null},null},
			new AMap<SwappedPojo,SwappedPojo>().append(new SwappedPojo(), new SwappedPojo()),
			new AMap<SwappedPojo,SwappedPojo[][][]>().append(new SwappedPojo(), new SwappedPojo[][][]{{{new SwappedPojo(),null},null},null})
		);
		assertEquals("OK", r);
	}

	@Test
	public void c06_implicitSwappedPojoFormData() throws Exception {
		String r = proxy.implicitSwappedPojoFormData(
			new ImplicitSwappedPojo(),
			new ImplicitSwappedPojo[][][]{{{new ImplicitSwappedPojo(),null},null},null},
			new AMap<ImplicitSwappedPojo,ImplicitSwappedPojo>().append(new ImplicitSwappedPojo(), new ImplicitSwappedPojo()),
			new AMap<ImplicitSwappedPojo,ImplicitSwappedPojo[][][]>().append(new ImplicitSwappedPojo(), new ImplicitSwappedPojo[][][]{{{new ImplicitSwappedPojo(),null},null},null})
		);
		assertEquals("OK", r);
	}

	@Test
	public void c07_enumFormData() throws Exception {
		String r = proxy.enumFormData(
			TestEnum.TWO,
			null,
			new TestEnum[][][]{{{TestEnum.TWO,null},null},null},
			new AList<TestEnum>().append(TestEnum.TWO).append(null),
			new AList<List<List<TestEnum>>>()
				.append(
					new AList<List<TestEnum>>()
						.append(
							new AList<TestEnum>().append(TestEnum.TWO).append(null)
						)
					.append(null)
				).append(null),
			new AList<TestEnum[][][]>().append(new TestEnum[][][]{{{TestEnum.TWO,null},null},null}).append(null),
			new AMap<TestEnum,TestEnum>().append(TestEnum.ONE,TestEnum.TWO),
			new AMap<TestEnum,TestEnum[][][]>().append(TestEnum.ONE, new TestEnum[][][]{{{TestEnum.TWO,null},null},null}),
			new AMap<TestEnum,List<TestEnum[][][]>>().append(TestEnum.ONE, new AList<TestEnum[][][]>().append(new TestEnum[][][]{{{TestEnum.TWO,null},null},null}).append(null))
		);
		assertEquals("OK", r);
	}

	@Test
	public void c08_mapFormData() throws Exception {
		String r = proxy.mapFormData(
			new AMap<String,Object>().append("a", "foo").append("b", "").append("c", null)
		);
		assertEquals("OK", r);
	}

	@Test
	public void c09_beanFormData() throws Exception {
		String r = proxy.beanFormData(
			new NeBean().init()
		);
		assertEquals("OK", r);
	}

	@Test
	public void c10_nameValuePairsFormData() throws Exception {
		String r = proxy.nameValuePairsFormData(
			new NameValuePairs().append("a", "foo").append("b", "").append("c", null)
		);
		assertEquals("OK", r);
	}

	@Test
	public void c11_formDataIfNE1() throws Exception {
		String r = proxy.formDataIfNE1(
			"foo"
		);
		assertEquals("OK", r);
	}

	@Test
	public void c12_formDataIfNE2() throws Exception {
		String r = proxy.formDataIfNE2(
			null
		);
		assertEquals("OK", r);
	}

	@Test
	public void c13_formDataIfNEMap() throws Exception {
		String r = proxy.formDataIfNEMap(
			new AMap<String,Object>().append("a", "foo").append("b", "").append("c", null)
		);
		assertEquals("OK", r);
	}

	@Test
	public void c14_formDataIfNEBean() throws Exception {
		String r = proxy.formDataIfNEBean(
			new NeBean().init()
		);
		assertEquals("OK", r);
	}

	@Test
	public void c15_formDataIfNENameValuePairs() throws Exception {
		String r = proxy.formDataIfNENameValuePairs(
			new NameValuePairs().append("a", "foo").append("b", "").append("c", null)
		);
		assertEquals("OK", r);
	}

	//--------------------------------------------------------------------------------
	// Test return types.
	//--------------------------------------------------------------------------------

	// Various primitives
	@Test
	public void da01_returnVoid() {
		proxy.returnVoid();
	}

	@Test
	public void da02_returnInteger() {
		assertEquals((Integer)1, proxy.returnInteger());
	}

	@Test
	public void da03_returnInt() {
		assertEquals(1, proxy.returnInt());
	}

	@Test
	public void da04_returnBoolean() {
		assertEquals(true, proxy.returnBoolean());
	}

	@Test
	public void da05_returnFloat() {
		assertTrue(1f == proxy.returnFloat());
	}

	@Test
	public void da06_returnFloatObject() {
		assertTrue(1f == proxy.returnFloatObject());
	}

	@Test
	public void da07_returnString() {
		assertEquals("foobar", proxy.returnString());
	}

	@Test
	public void da08_returnNullString() {
		assertNull(proxy.returnNullString());
	}

	@Test
	public void da09_returnInt3dArray() {
		assertObjectEquals("[[[1,2],null],null]", proxy.returnInt3dArray());
	}

	@Test
	public void da10_returnInteger3dArray() {
		assertObjectEquals("[[[1,null],null],null]", proxy.returnInteger3dArray());
	}

	@Test
	public void da11_returnString3dArray() {
		assertObjectEquals("[[['foo','bar',null],null],null]", proxy.returnString3dArray());
	}

	@Test
	public void da12_returnIntegerList() {
		List<Integer> x = proxy.returnIntegerList();
		assertObjectEquals("[1,null]", x);
		assertClass(Integer.class, x.get(0));
	}

	@Test
	public void da13_returnInteger3dList() {
		List<List<List<Integer>>> x = proxy.returnInteger3dList();
		assertObjectEquals("[[[1,null],null],null]", x);
		assertClass(Integer.class, x.get(0).get(0).get(0));
	}

	@Test
	public void da14_returnInteger1d3dList() {
		List<Integer[][][]> x = proxy.returnInteger1d3dList();
		assertObjectEquals("[[[[1,null],null],null],null]", x);
		assertClass(Integer.class, x.get(0)[0][0][0]);
	}

	@Test
	public void da15_returnInt1d3dList() {
		List<int[][][]> x = proxy.returnInt1d3dList();
		assertObjectEquals("[[[[1,2],null],null],null]", x);
		assertClass(int[][][].class, x.get(0));
	}

	@Test
	public void da16_returnStringList() {
		assertObjectEquals("['foo','bar',null]", proxy.returnStringList());
	}

	// Beans

	@Test
	public void db01_returnBean() {
		ABean x = proxy.returnBean();
		assertObjectEquals("{a:1,b:'foo'}", x);
		assertClass(ABean.class, x);
	}

	@Test
	public void db02_returnBean3dArray() {
		ABean[][][] x = proxy.returnBean3dArray();
		assertObjectEquals("[[[{a:1,b:'foo'},null],null],null]", x);
		assertClass(ABean.class, x[0][0][0]);
	}

	@Test
	public void db03_returnBeanList() {
		List<ABean> x = proxy.returnBeanList();
		assertObjectEquals("[{a:1,b:'foo'}]", x);
		assertClass(ABean.class, x.get(0));
	}

	@Test
	public void db04_returnBean1d3dList() {
		List<ABean[][][]> x = proxy.returnBean1d3dList();
		assertObjectEquals("[[[[{a:1,b:'foo'},null],null],null],null]", x);
		assertClass(ABean.class, x.get(0)[0][0][0]);
	}

	@Test
	public void db05_returnBeanMap() {
		Map<String,ABean> x = proxy.returnBeanMap();
		assertObjectEquals("{foo:{a:1,b:'foo'}}", x);
		assertClass(ABean.class, x.get("foo"));
	}

	@Test
	public void db06_returnBeanListMap() {
		Map<String,List<ABean>> x = proxy.returnBeanListMap();
		assertObjectEquals("{foo:[{a:1,b:'foo'}]}", x);
		assertClass(ABean.class, x.get("foo").get(0));
	}

	@Test
	public void db07_returnBean1d3dListMap() {
		Map<String,List<ABean[][][]>> x = proxy.returnBean1d3dListMap();
		assertObjectEquals("{foo:[[[[{a:1,b:'foo'},null],null],null],null]}", x);
		assertClass(ABean.class, x.get("foo").get(0)[0][0][0]);
	}

	@Test
	public void db08_returnBeanListMapIntegerKeys() {
		// Note: JsonSerializer serializes key as string.
		Map<Integer,List<ABean>> x = proxy.returnBeanListMapIntegerKeys();
		assertObjectEquals("{'1':[{a:1,b:'foo'}]}", x);
		assertClass(Integer.class, x.keySet().iterator().next());
	}

	// Typed beans

	@Test
	public void dc01_returnTypedBean() {
		TypedBean x = proxy.returnTypedBean();
		assertObjectEquals("{_type:'TypedBeanImpl',a:1,b:'foo'}", x);
		assertClass(TypedBeanImpl.class, x);
	}

	@Test
	public void dc02_returnTypedBean3dArray() {
		TypedBean[][][] x = proxy.returnTypedBean3dArray();
		assertObjectEquals("[[[{_type:'TypedBeanImpl',a:1,b:'foo'},null],null],null]", x);
		assertClass(TypedBeanImpl.class, x[0][0][0]);
	}

	@Test
	public void dc03_returnTypedBeanList() {
		List<TypedBean> x = proxy.returnTypedBeanList();
		assertObjectEquals("[{_type:'TypedBeanImpl',a:1,b:'foo'}]", x);
		assertClass(TypedBeanImpl.class, x.get(0));
	}

	@Test
	public void dc04_returnTypedBean1d3dList() {
		List<TypedBean[][][]> x = proxy.returnTypedBean1d3dList();
		assertObjectEquals("[[[[{_type:'TypedBeanImpl',a:1,b:'foo'},null],null],null],null]", x);
		assertClass(TypedBeanImpl.class, x.get(0)[0][0][0]);
	}

	@Test
	public void dc05_returnTypedBeanMap() {
		Map<String,TypedBean> x = proxy.returnTypedBeanMap();
		assertObjectEquals("{foo:{_type:'TypedBeanImpl',a:1,b:'foo'}}", x);
		assertClass(TypedBeanImpl.class, x.get("foo"));
	}

	@Test
	public void dc06_returnTypedBeanListMap() {
		Map<String,List<TypedBean>> x = proxy.returnTypedBeanListMap();
		assertObjectEquals("{foo:[{_type:'TypedBeanImpl',a:1,b:'foo'}]}", x);
		assertClass(TypedBeanImpl.class, x.get("foo").get(0));
	}

	@Test
	public void dc07_returnTypedBean1d3dListMap() {
		Map<String,List<TypedBean[][][]>> x = proxy.returnTypedBean1d3dListMap();
		assertObjectEquals("{foo:[[[[{_type:'TypedBeanImpl',a:1,b:'foo'},null],null],null],null]}", x);
		assertClass(TypedBeanImpl.class, x.get("foo").get(0)[0][0][0]);
	}

	@Test
	public void dc08_returnTypedBeanListMapIntegerKeys() {
		// Note: JsonSerializer serializes key as string.
		Map<Integer,List<TypedBean>> x = proxy.returnTypedBeanListMapIntegerKeys();
		assertObjectEquals("{'1':[{_type:'TypedBeanImpl',a:1,b:'foo'}]}", x);
		assertClass(TypedBeanImpl.class, x.get(1).get(0));
	}

	// Swapped POJOs

	@Test
	public void dd01_returnSwappedPojo() {
		SwappedPojo x = proxy.returnSwappedPojo();
		assertObjectEquals("'"+SWAP+"'", x);
		assertTrue(x.wasUnswapped);
	}

	@Test
	public void dd02_returnSwappedPojo3dArray() {
		SwappedPojo[][][] x = proxy.returnSwappedPojo3dArray();
		assertObjectEquals("[[['"+SWAP+"',null],null],null]", x);
		assertTrue(x[0][0][0].wasUnswapped);
	}

	@Test
	public void dd03_returnSwappedPojoMap() {
		Map<SwappedPojo,SwappedPojo> x = proxy.returnSwappedPojoMap();
		assertObjectEquals("{'"+SWAP+"':'"+SWAP+"'}", x);
		Map.Entry<SwappedPojo,SwappedPojo> e = x.entrySet().iterator().next();
		assertTrue(e.getKey().wasUnswapped);
		assertTrue(e.getValue().wasUnswapped);
	}

	@Test
	public void dd04_returnSwappedPojo3dMap() {
		Map<SwappedPojo,SwappedPojo[][][]> x = proxy.returnSwappedPojo3dMap();
		assertObjectEquals("{'"+SWAP+"':[[['"+SWAP+"',null],null],null]}", x);
		Map.Entry<SwappedPojo,SwappedPojo[][][]> e = x.entrySet().iterator().next();
		assertTrue(e.getKey().wasUnswapped);
		assertTrue(e.getValue()[0][0][0].wasUnswapped);
	}

	// Implicit swapped POJOs

	@Test
	public void de01_returnImplicitSwappedPojo() {
		ImplicitSwappedPojo x = proxy.returnImplicitSwappedPojo();
		assertObjectEquals("'"+SWAP+"'", x);
		assertTrue(x.wasUnswapped);
	}

	@Test
	public void de02_returnImplicitSwappedPojo3dArray() {
		ImplicitSwappedPojo[][][] x = proxy.returnImplicitSwappedPojo3dArray();
		assertObjectEquals("[[['"+SWAP+"',null],null],null]", x);
		assertTrue(x[0][0][0].wasUnswapped);
	}

	@Test
	public void de03_returnImplicitSwappedPojoMap() {
		Map<ImplicitSwappedPojo,ImplicitSwappedPojo> x = proxy.returnImplicitSwappedPojoMap();
		assertObjectEquals("{'"+SWAP+"':'"+SWAP+"'}", x);
		Map.Entry<ImplicitSwappedPojo,ImplicitSwappedPojo> e = x.entrySet().iterator().next();
		assertTrue(e.getKey().wasUnswapped);
		assertTrue(e.getValue().wasUnswapped);
	}

	@Test
	public void de04_returnImplicitSwappedPojo3dMap() {
		Map<ImplicitSwappedPojo,ImplicitSwappedPojo[][][]> x = proxy.returnImplicitSwappedPojo3dMap();
		assertObjectEquals("{'"+SWAP+"':[[['"+SWAP+"',null],null],null]}", x);
		Map.Entry<ImplicitSwappedPojo,ImplicitSwappedPojo[][][]> e = x.entrySet().iterator().next();
		assertTrue(e.getKey().wasUnswapped);
		assertTrue(e.getValue()[0][0][0].wasUnswapped);
	}

	// Enums

	@Test
	public void df01_returnEnum() {
		TestEnum x = proxy.returnEnum();
		assertObjectEquals("'TWO'", x);
	}

	@Test
	public void df02_returnEnum3d() {
		TestEnum[][][] x = proxy.returnEnum3d();
		assertObjectEquals("[[['TWO',null],null],null]", x);
		assertClass(TestEnum.class, x[0][0][0]);
	}

	@Test
	public void df03_returnEnumList() {
		List<TestEnum> x = proxy.returnEnumList();
		assertObjectEquals("['TWO',null]", x);
		assertClass(TestEnum.class, x.get(0));
	}

	@Test
	public void df04_returnEnum3dList() {
		List<List<List<TestEnum>>> x = proxy.returnEnum3dList();
		assertObjectEquals("[[['TWO',null],null,null]]", x);
		assertClass(TestEnum.class, x.get(0).get(0).get(0));
	}

	@Test
	public void df05_returnEnum1d3dList() {
		List<TestEnum[][][]> x = proxy.returnEnum1d3dList();
		assertObjectEquals("[[[['TWO',null],null],null],null]", x);
		assertClass(TestEnum[][][].class, x.get(0));
	}

	@Test
	public void df06_returnEnumMap() {
		Map<TestEnum,TestEnum> x = proxy.returnEnumMap();
		assertObjectEquals("{ONE:'TWO'}", x);
		Map.Entry<TestEnum,TestEnum> e = x.entrySet().iterator().next();
		assertClass(TestEnum.class, e.getKey());
		assertClass(TestEnum.class, e.getValue());
	}

	@Test
	public void df07_returnEnum3dArrayMap() {
		Map<TestEnum,TestEnum[][][]> x = proxy.returnEnum3dArrayMap();
		assertObjectEquals("{ONE:[[['TWO',null],null],null]}", x);
		Map.Entry<TestEnum,TestEnum[][][]> e = x.entrySet().iterator().next();
		assertClass(TestEnum.class, e.getKey());
		assertClass(TestEnum[][][].class, e.getValue());
	}

	@Test
	public void df08_returnEnum1d3dListMap() {
		Map<TestEnum,List<TestEnum[][][]>> x = proxy.returnEnum1d3dListMap();
		assertObjectEquals("{ONE:[[[['TWO',null],null],null],null]}", x);
		assertClass(TestEnum[][][].class, x.get(TestEnum.ONE).get(0));
	}

	//--------------------------------------------------------------------------------
	// Test Body
	//--------------------------------------------------------------------------------

	// Various primitives

	@Test
	public void ea01_setInt() {
		proxy.setInt(1);
	}

	@Test
	public void ea02_setWrongInt() {
		try {
			proxy.setInt(2);
			fail("Exception expected");
		} catch (AssertionError e) { // AssertionError thrown on server side.
			assertEquals("expected:<1> but was:<2>", e.getMessage());
		}
	}

	@Test
	public void ea03_setInteger() {
		proxy.setInteger(1);
	}

	@Test
	public void ea04_setBoolean() {
		proxy.setBoolean(true);
	}

	@Test
	public void ea05_setFloat() {
		proxy.setFloat(1f);
	}

	@Test
	public void ea06_setFloatObject() {
		proxy.setFloatObject(1f);
	}

	@Test
	public void ea07_setString() {
		proxy.setString("foo");
	}

	@Test
	public void ea08_setNullString() {
		proxy.setNullString(null);
	}

	@Test
	public void ea09_setNullStringBad() {
		try {
			proxy.setNullString("foo");
			fail("Exception expected");
		} catch (AssertionError e) { // AssertionError thrown on server side.
			assertEquals("expected null, but was:<foo>", e.getLocalizedMessage());
		}
	}

	@Test
	public void ea10_setInt3dArray() {
		proxy.setInt3dArray(new int[][][]{{{1},null},null}, 1);
	}

	@Test
	public void ea11_setInteger3dArray() {
		proxy.setInteger3dArray(new Integer[][][]{{{1,null},null},null});
	}

	@Test
	public void ea12_setString3dArray() {
		proxy.setString3dArray(new String[][][]{{{"foo",null},null},null});
	}

	@Test
	public void ea13_setIntegerList() {
		proxy.setIntegerList(new AList<Integer>().append(1).append(null));
	}

	@Test
	public void ea14_setInteger3dList() {
		proxy.setInteger3dList(
			new AList<List<List<Integer>>>()
			.append(
				new AList<List<Integer>>()
				.append(new AList<Integer>().append(1).append(null))
				.append(null)
			)
			.append(null)
		);
	}

	@Test
	public void ea15_setInteger1d3dList() {
		proxy.setInteger1d3dList(
			new AList<Integer[][][]>().append(new Integer[][][]{{{1,null},null},null}).append(null)
		);
	}

	@Test
	public void ea16_setInt1d3dList() {
		proxy.setInt1d3dList(
			new AList<int[][][]>().append(new int[][][]{{{1,2},null},null}).append(null)
		);
	}

	@Test
	public void ea17_setStringList() {
		proxy.setStringList(Arrays.asList("foo","bar",null));
	}

	// Beans
	@Test
	public void eb01_setBean() {
		proxy.setBean(new ABean().init());
	}

	@Test
	public void eb02_setBean3dArray() {
		proxy.setBean3dArray(new ABean[][][]{{{new ABean().init(),null},null},null});
	}

	@Test
	public void eb03_setBeanList() {
		proxy.setBeanList(Arrays.asList(new ABean().init()));
	}

	@Test
	public void eb04_setBean1d3dList() {
		proxy.setBean1d3dList(new AList<ABean[][][]>().append(new ABean[][][]{{{new ABean().init(),null},null},null}).append(null));
	}

	@Test
	public void eb05_setBeanMap() {
		proxy.setBeanMap(new AMap<String,ABean>().append("foo",new ABean().init()));
	}

	@Test
	public void eb06_setBeanListMap() {
		proxy.setBeanListMap(new AMap<String,List<ABean>>().append("foo",Arrays.asList(new ABean().init())));
	}

	@Test
	public void eb07_setBean1d3dListMap() {
		proxy.setBean1d3dListMap(new AMap<String,List<ABean[][][]>>().append("foo",new AList<ABean[][][]>().append(new ABean[][][]{{{new ABean().init(),null},null},null}).append(null)));
	}

	@Test
	public void eb08_setBeanListMapIntegerKeys() {
		proxy.setBeanListMapIntegerKeys(new AMap<Integer,List<ABean>>().append(1,Arrays.asList(new ABean().init())));
	}

	// Typed beans

	@Test
	public void ec01_setTypedBean() {
		proxy.setTypedBean(new TypedBeanImpl().init());
	}

	@Test
	public void ec02_setTypedBean3dArray() {
		proxy.setTypedBean3dArray(new TypedBean[][][]{{{new TypedBeanImpl().init(),null},null},null});
	}

	@Test
	public void ec03_setTypedBeanList() {
		proxy.setTypedBeanList(Arrays.asList((TypedBean)new TypedBeanImpl().init()));
	}

	@Test
	public void ec04_setTypedBean1d3dList() {
		proxy.setTypedBean1d3dList(new AList<TypedBean[][][]>().append(new TypedBean[][][]{{{new TypedBeanImpl().init(),null},null},null}).append(null));
	}

	@Test
	public void ec05_setTypedBeanMap() {
		proxy.setTypedBeanMap(new AMap<String,TypedBean>().append("foo",new TypedBeanImpl().init()));
	}

	@Test
	public void ec06_setTypedBeanListMap() {
		proxy.setTypedBeanListMap(new AMap<String,List<TypedBean>>().append("foo",Arrays.asList((TypedBean)new TypedBeanImpl().init())));
	}

	@Test
	public void ec07_setTypedBean1d3dListMap() {
		proxy.setTypedBean1d3dListMap(new AMap<String,List<TypedBean[][][]>>().append("foo",new AList<TypedBean[][][]>().append(new TypedBean[][][]{{{new TypedBeanImpl().init(),null},null},null}).append(null)));
	}

	@Test
	public void ec08_setTypedBeanListMapIntegerKeys() {
		proxy.setTypedBeanListMapIntegerKeys(new AMap<Integer,List<TypedBean>>().append(1,Arrays.asList((TypedBean)new TypedBeanImpl().init())));
	}

	// Swapped POJOs

	@Test
	public void ed01_setSwappedPojo() {
		proxy.setSwappedPojo(new SwappedPojo());
	}

	@Test
	public void ed02_setSwappedPojo3dArray() {
		proxy.setSwappedPojo3dArray(new SwappedPojo[][][]{{{new SwappedPojo(),null},null},null});
	}

	@Test
	public void ed03_setSwappedPojoMap() {
		proxy.setSwappedPojoMap(new AMap<SwappedPojo,SwappedPojo>().append(new SwappedPojo(), new SwappedPojo()));
	}

	@Test
	public void ed04_setSwappedPojo3dMap() {
		proxy.setSwappedPojo3dMap(new AMap<SwappedPojo,SwappedPojo[][][]>().append(new SwappedPojo(), new SwappedPojo[][][]{{{new SwappedPojo(),null},null},null}));
	}

	// Implicit swapped POJOs
	@Test
	public void ee01_setImplicitSwappedPojo() {
		proxy.setImplicitSwappedPojo(new ImplicitSwappedPojo());
	}

	@Test
	public void ee02_setImplicitSwappedPojo3dArray() {
		proxy.setImplicitSwappedPojo3dArray(new ImplicitSwappedPojo[][][]{{{new ImplicitSwappedPojo(),null},null},null});
	}

	@Test
	public void ee03_setImplicitSwappedPojoMap() {
		proxy.setImplicitSwappedPojoMap(new AMap<ImplicitSwappedPojo,ImplicitSwappedPojo>().append(new ImplicitSwappedPojo(), new ImplicitSwappedPojo()));
	}

	@Test
	public void ee04_setImplicitSwappedPojo3dMap() {
		proxy.setImplicitSwappedPojo3dMap(new AMap<ImplicitSwappedPojo,ImplicitSwappedPojo[][][]>().append(new ImplicitSwappedPojo(), new ImplicitSwappedPojo[][][]{{{new ImplicitSwappedPojo(),null},null},null}));
	}

	// Enums

	@Test
	public void ef01_setEnum() {
		proxy.setEnum(TestEnum.TWO);
	}

	@Test
	public void ef02_setEnum3d() {
		proxy.setEnum3d(new TestEnum[][][]{{{TestEnum.TWO,null},null},null});
	}

	@Test
	public void ef03_setEnumList() {
		proxy.setEnumList(new AList<TestEnum>().append(TestEnum.TWO).append(null));
	}

	@Test
	public void ef04_setEnum3dList() {
		proxy.setEnum3dList(
			new AList<List<List<TestEnum>>>()
			.append(
				new AList<List<TestEnum>>()
				.append(
					new AList<TestEnum>().append(TestEnum.TWO).append(null)
				)
				.append(null)
			.append(null)
			)
		);
	}

	@Test
	public void ef05_setEnum1d3dList() {
		proxy.setEnum1d3dList(new AList<TestEnum[][][]>().append(new TestEnum[][][]{{{TestEnum.TWO,null},null},null}).append(null));
	}

	@Test
	public void ef06_setEnumMap() {
		proxy.setEnumMap(new AMap<TestEnum,TestEnum>().append(TestEnum.ONE,TestEnum.TWO));
	}

	@Test
	public void ef07_setEnum3dArrayMap() {
		proxy.setEnum3dArrayMap(new AMap<TestEnum,TestEnum[][][]>().append(TestEnum.ONE, new TestEnum[][][]{{{TestEnum.TWO,null},null},null}));
	}

	@Test
	public void ef08_setEnum1d3dListMap() {
		proxy.setEnum1d3dListMap(new AMap<TestEnum,List<TestEnum[][][]>>().append(TestEnum.ONE, new AList<TestEnum[][][]>().append(new TestEnum[][][]{{{TestEnum.TWO,null},null},null}).append(null)));
	}

	//--------------------------------------------------------------------------------
	// Path variables
	//--------------------------------------------------------------------------------

	@Test
	public void f01_pathVars1() {
		String r = proxy.pathVars1(1, "foo");
		assertEquals("OK", r);
	}

	@Test
	public void f02_pathVars2() {
		String r = proxy.pathVars2(
			new AMap<String,Object>().append("a", 1).append("b", "foo")
		);
		assertEquals("OK", r);
	}

	@Test
	public void f03_pathVars3() {
		String r = proxy.pathVars3(
			new ABean().init()
		);
		assertEquals("OK", r);
	}

	//--------------------------------------------------------------------------------
	// RequestBean tests - Path
	//--------------------------------------------------------------------------------

	@Test
	public void ga01_reqBeanPath1() throws Exception {
		String r = proxy.reqBeanPath1(
			new ReqBeanPath1() {
				@Override
				public int getA() {
					return 1;
				}
				@Override
				public String getB() {
					return "foo";
				}
			}
		);
		assertEquals("OK", r);
	}

	@Test
	public void ga01_reqBeanPath1a() throws Exception {
		String r = proxy.reqBeanPath1(
			new ReqBeanPath1Impl()
		);
		assertEquals("OK", r);
	}

	@Test
	public void ga02_reqBeanPath2() throws Exception {
		String r = proxy.reqBeanPath2(
			new ReqBeanPath2()
		);
		assertEquals("OK", r);
	}

	@Test
	public void ga03_reqBeanPath3() throws Exception {
		String r = proxy.reqBeanPath3(
			new ReqBeanPath3() {
				@Override
				public int getX() {
					return 1;
				}
				@Override
				public String getY() {
					return "foo";
				}
			}
		);
		assertEquals("OK", r);
	}

	@Test
	public void ga04_reqBeanPath4() throws Exception {
		String r = proxy.reqBeanPath4(
			new ReqBeanPath4() {
				@Override
				public int getX() {
					return 1;
				}
				@Override
				public String getY() {
					return "foo";
				}
			}
		);
		assertEquals("OK", r);
	}

	@Test
	public void ga05_reqBeanPath5() throws Exception {
		String r = proxy.reqBeanPath5(
			new ReqBeanPath5() {
				@Override
				public int getX() {
					return 1;
				}
				@Override
				public String getY() {
					return "foo";
				}
			}
		);
		assertEquals("OK", r);
	}

	@Test
	public void ga06_reqBeanPath6() throws Exception {
		String r = proxy.reqBeanPath6(
			new ReqBeanPath6() {
				@Override
				public Map<String,Object> getX() {
					return new AMap<String,Object>().append("a",1).append("b","foo");
				}

			}
		);
		assertEquals("OK", r);
	}

	@Test
	public void ga07_reqBeanPath7() throws Exception {
		String r = proxy.reqBeanPath7(
			new ReqBeanPath7() {
				@Override
				public ABean getX() {
					return new ABean().init();
				}
			}
		);
		assertEquals("OK", r);
	}

	//--------------------------------------------------------------------------------
	// RequestBean tests - Query
	//--------------------------------------------------------------------------------

	@Test
	public void gb01_reqBeanQuery1() throws Exception {
		String r = proxy.reqBeanQuery1(
			new ReqBeanQuery1() {
				@Override
				public int getA() {
					return 1;
				}
				@Override
				public String getB() {
					return "foo";
				}
			}
		);
		assertEquals("OK", r);
	}

	@Test
	public void gb01_reqBeanQuery1a() throws Exception {
		String r = proxy.reqBeanQuery1(
			new ReqBeanQuery1Impl()
		);
		assertEquals("OK", r);
	}

	@Test
	public void gb02_reqBeanQuery2() throws Exception {
		String r = proxy.reqBeanQuery2(
			new ReqBeanQuery2()
		);
		assertEquals("OK", r);
	}

	@Test
	public void gb03_reqBeanQuery3() throws Exception {
		String r = proxy.reqBeanQuery3(
			new ReqBeanQuery3() {
				@Override
				public int getX() {
					return 1;
				}
				@Override
				public String getY() {
					return "foo";
				}
			}
		);
		assertEquals("OK", r);
	}

	@Test
	public void gb04_reqBeanQuery4() throws Exception {
		String r = proxy.reqBeanQuery4(
			new ReqBeanQuery4() {
				@Override
				public int getX() {
					return 1;
				}
				@Override
				public String getY() {
					return "foo";
				}
			}
		);
		assertEquals("OK", r);
	}

	@Test
	public void gb05_reqBeanQuery5() throws Exception {
		String r = proxy.reqBeanQuery5(
			new ReqBeanQuery5() {
				@Override
				public int getX() {
					return 1;
				}
				@Override
				public String getY() {
					return "foo";
				}
			}
		);
		assertEquals("OK", r);
	}

	@Test
	public void gb06_reqBeanQuery6() throws Exception {
		String r = proxy.reqBeanQuery6(
			new ReqBeanQuery6() {
				@Override
				public Map<String,Object> getX() {
					return new AMap<String,Object>().append("a",1).append("b","foo");
				}

			}
		);
		assertEquals("OK", r);
	}

	@Test
	public void gb07_reqBeanQuery7() throws Exception {
		String r = proxy.reqBeanQuery7(
			new ReqBeanQuery7() {
				@Override
				public ABean getX() {
					return new ABean().init();
				}
			}
		);
		assertEquals("OK", r);
	}

	//--------------------------------------------------------------------------------
	// RequestBean tests - QueryIfNE
	//--------------------------------------------------------------------------------

	@Test
	public void gc01_reqBeanQueryIfNE1() throws Exception {
		String r = proxy.reqBeanQueryIfNE1(
			new ReqBeanQueryIfNE1() {
				@Override
				public String getA() {
					return "foo";
				}
				@Override
				public String getB() {
					return "";
				}
				@Override
				public String getC() {
					return null;
				}
			}
		);
		assertEquals("OK", r);
	}

	@Test
	public void gc01_reqBeanQueryIfNE1a() throws Exception {
		String r = proxy.reqBeanQueryIfNE1(
			new ReqBeanQueryIfNE1Impl()
		);
		assertEquals("OK", r);
	}

	@Test
	public void gc02_reqBeanQueryIfNE2() throws Exception {
		String r = proxy.reqBeanQueryIfNE2(
			new ReqBeanQueryIfNE2()
		);
		assertEquals("OK", r);
	}

	@Test
	public void gc03_reqBeanQueryIfNE3() throws Exception {
		String r = proxy.reqBeanQueryIfNE3(
			new ReqBeanQueryIfNE3() {
				@Override
				public String getX() {
					return "foo";
				}
				@Override
				public String getY() {
					return "";
				}
				@Override
				public String getZ() {
					return null;
				}
			}
		);
		assertEquals("OK", r);
	}

	@Test
	public void gc04_reqBeanQueryIfNE4() throws Exception {
		String r = proxy.reqBeanQueryIfNE4(
			new ReqBeanQueryIfNE4() {
				@Override
				public String getX() {
					return "foo";
				}
				@Override
				public String getY() {
					return "";
				}
				@Override
				public String getZ() {
					return null;
				}
			}
		);
		assertEquals("OK", r);
	}

	@Test
	public void gc05_reqBeanQueryIfNE5() throws Exception {
		String r = proxy.reqBeanQueryIfNE5(
			new ReqBeanQueryIfNE5() {
				@Override
				public String getX() {
					return "foo";
				}
				@Override
				public String getY() {
					return "";
				}
				@Override
				public String getZ() {
					return null;
				}
			}
		);
		assertEquals("OK", r);
	}

	@Test
	public void gc06_reqBeanQueryIfNE6() throws Exception {
		String r = proxy.reqBeanQueryIfNE6(
			new ReqBeanQueryIfNE6() {
				@Override
				public Map<String,Object> getX() {
					return new AMap<String,Object>().append("a","foo").append("b","").append("c", null);
				}

			}
		);
		assertEquals("OK", r);
	}

	@Test
	public void gc07_reqBeanQueryIfNE7() throws Exception {
		String r = proxy.reqBeanQueryIfNE7(
			new ReqBeanQueryIfNE7() {
				@Override
				public NeBean getX() {
					return new NeBean().init();
				}
			}
		);
		assertEquals("OK", r);
	}

	//--------------------------------------------------------------------------------
	// RequestBean tests - FormData
	//--------------------------------------------------------------------------------

	@Test
	public void gd01_reqBeanFormData1() throws Exception {
		String r = proxy.reqBeanFormData1(
			new ReqBeanFormData1() {
				@Override
				public int getA() {
					return 1;
				}
				@Override
				public String getB() {
					return "foo";
				}
			}
		);
		assertEquals("OK", r);
	}

	@Test
	public void gd01_reqBeanFormData1a() throws Exception {
		String r = proxy.reqBeanFormData1(
			new ReqBeanFormData1Impl()
		);
		assertEquals("OK", r);
	}

	@Test
	public void gd02_reqBeanFormData2() throws Exception {
		String r = proxy.reqBeanFormData2(
			new ReqBeanFormData2()
		);
		assertEquals("OK", r);
	}

	@Test
	public void gd03_reqBeanFormData3() throws Exception {
		String r = proxy.reqBeanFormData3(
			new ReqBeanFormData3() {
				@Override
				public int getX() {
					return 1;
				}
				@Override
				public String getY() {
					return "foo";
				}
			}
		);
		assertEquals("OK", r);
	}

	@Test
	public void gd04_reqBeanFormData4() throws Exception {
		String r = proxy.reqBeanFormData4(
			new ReqBeanFormData4() {
				@Override
				public int getX() {
					return 1;
				}
				@Override
				public String getY() {
					return "foo";
				}
			}
		);
		assertEquals("OK", r);
	}

	@Test
	public void gd05_reqBeanFormData5() throws Exception {
		String r = proxy.reqBeanFormData5(
			new ReqBeanFormData5() {
				@Override
				public int getX() {
					return 1;
				}
				@Override
				public String getY() {
					return "foo";
				}
			}
		);
		assertEquals("OK", r);
	}

	@Test
	public void gd06_reqBeanFormData6() throws Exception {
		String r = proxy.reqBeanFormData6(
			new ReqBeanFormData6() {
				@Override
				public Map<String,Object> getX() {
					return new AMap<String,Object>().append("a",1).append("b","foo");
				}

			}
		);
		assertEquals("OK", r);
	}

	@Test
	public void gd07_reqBeanFormData7() throws Exception {
		String r = proxy.reqBeanFormData7(
			new ReqBeanFormData7() {
				@Override
				public ABean getX() {
					return new ABean().init();
				}
			}
		);
		assertEquals("OK", r);
	}

	//--------------------------------------------------------------------------------
	// RequestBean tests - FormDataIfNE
	//--------------------------------------------------------------------------------

	@Test
	public void ge01_reqBeanFormDataIfNE1() throws Exception {
		String r = proxy.reqBeanFormDataIfNE1(
			new ReqBeanFormDataIfNE1() {
				@Override
				public String getA() {
					return "foo";
				}
				@Override
				public String getB() {
					return "";
				}
				@Override
				public String getC() {
					return null;
				}
			}
		);
		assertEquals("OK", r);
	}

	@Test
	public void ge01_reqBeanFormDataIfNE1a() throws Exception {
		String r = proxy.reqBeanFormDataIfNE1(
			new ReqBeanFormDataIfNE1Impl()
		);
		assertEquals("OK", r);
	}

	@Test
	public void ge02_reqBeanFormDataIfNE2() throws Exception {
		String r = proxy.reqBeanFormDataIfNE2(
			new ReqBeanFormDataIfNE2()
		);
		assertEquals("OK", r);
	}

	@Test
	public void ge03_reqBeanFormDataIfNE3() throws Exception {
		String r = proxy.reqBeanFormDataIfNE3(
			new ReqBeanFormDataIfNE3() {
				@Override
				public String getX() {
					return "foo";
				}
				@Override
				public String getY() {
					return "";
				}
				@Override
				public String getZ() {
					return null;
				}
			}
		);
		assertEquals("OK", r);
	}

	@Test
	public void ge04_reqBeanFormDataIfNE4() throws Exception {
		String r = proxy.reqBeanFormDataIfNE4(
			new ReqBeanFormDataIfNE4() {
				@Override
				public String getX() {
					return "foo";
				}
				@Override
				public String getY() {
					return "";
				}
				@Override
				public String getZ() {
					return null;
				}
			}
		);
		assertEquals("OK", r);
	}

	@Test
	public void ge05_reqBeanFormDataIfNE5() throws Exception {
		String r = proxy.reqBeanFormDataIfNE5(
			new ReqBeanFormDataIfNE5() {
				@Override
				public String getX() {
					return "foo";
				}
				@Override
				public String getY() {
					return "";
				}
				@Override
				public String getZ() {
					return null;
				}
			}
		);
		assertEquals("OK", r);
	}

	@Test
	public void ge06_reqBeanFormDataIfNE6() throws Exception {
		String r = proxy.reqBeanFormDataIfNE6(
			new ReqBeanFormDataIfNE6() {
				@Override
				public Map<String,Object> getX() {
					return new AMap<String,Object>().append("a","foo").append("b","").append("c", null);
				}

			}
		);
		assertEquals("OK", r);
	}

	@Test
	public void ge07_reqBeanFormDataIfNE7() throws Exception {
		String r = proxy.reqBeanFormDataIfNE7(
			new ReqBeanFormDataIfNE7() {
				@Override
				public NeBean getX() {
					return new NeBean().init();
				}
			}
		);
		assertEquals("OK", r);
	}

	//--------------------------------------------------------------------------------
	// RequestBean tests - Header
	//--------------------------------------------------------------------------------

	@Test
	public void gf01_reqBeanHeader1() throws Exception {
		String r = proxy.reqBeanHeader1(
			new ReqBeanHeader1() {
				@Override
				public int getA() {
					return 1;
				}
				@Override
				public String getB() {
					return "foo";
				}
			}
		);
		assertEquals("OK", r);
	}

	@Test
	public void gf01_reqBeanHeader1a() throws Exception {
		String r = proxy.reqBeanHeader1(
			new ReqBeanHeader1Impl()
		);
		assertEquals("OK", r);
	}

	@Test
	public void gf02_reqBeanHeader2() throws Exception {
		String r = proxy.reqBeanHeader2(
			new ReqBeanHeader2()
		);
		assertEquals("OK", r);
	}

	@Test
	public void gf03_reqBeanHeader3() throws Exception {
		String r = proxy.reqBeanHeader3(
			new ReqBeanHeader3() {
				@Override
				public int getX() {
					return 1;
				}
				@Override
				public String getY() {
					return "foo";
				}
			}
		);
		assertEquals("OK", r);
	}

	@Test
	public void gf04_reqBeanHeader4() throws Exception {
		String r = proxy.reqBeanHeader4(
			new ReqBeanHeader4() {
				@Override
				public int getX() {
					return 1;
				}
				@Override
				public String getY() {
					return "foo";
				}
			}
		);
		assertEquals("OK", r);
	}

	@Test
	public void gf05_reqBeanHeader5() throws Exception {
		String r = proxy.reqBeanHeader5(
			new ReqBeanHeader5() {
				@Override
				public int getX() {
					return 1;
				}
				@Override
				public String getY() {
					return "foo";
				}
			}
		);
		assertEquals("OK", r);
	}

	@Test
	public void gf06_reqBeanHeader6() throws Exception {
		String r = proxy.reqBeanHeader6(
			new ReqBeanHeader6() {
				@Override
				public Map<String,Object> getX() {
					return new AMap<String,Object>().append("a",1).append("b","foo");
				}

			}
		);
		assertEquals("OK", r);
	}

	@Test
	public void gf07_reqBeanHeader7() throws Exception {
		String r = proxy.reqBeanHeader7(
			new ReqBeanHeader7() {
				@Override
				public ABean getX() {
					return new ABean().init();
				}
			}
		);
		assertEquals("OK", r);
	}

	//--------------------------------------------------------------------------------
	// RequestBean tests - HeaderIfNE
	//--------------------------------------------------------------------------------

	@Test
	public void gg01_reqBeanHeaderIfNE1() throws Exception {
		String r = proxy.reqBeanHeaderIfNE1(
			new ReqBeanHeaderIfNE1() {
				@Override
				public String getA() {
					return "foo";
				}
				@Override
				public String getB() {
					return "";
				}
				@Override
				public String getC() {
					return null;
				}
			}
		);
		assertEquals("OK", r);
	}

	@Test
	public void gg01_reqBeanHeaderIfNE1a() throws Exception {
		String r = proxy.reqBeanHeaderIfNE1(
			new ReqBeanHeaderIfNE1Impl()
		);
		assertEquals("OK", r);
	}

	@Test
	public void gg02_reqBeanHeaderIfNE2() throws Exception {
		String r = proxy.reqBeanHeaderIfNE2(
			new ReqBeanHeaderIfNE2()
		);
		assertEquals("OK", r);
	}

	@Test
	public void gg03_reqBeanHeaderIfNE3() throws Exception {
		String r = proxy.reqBeanHeaderIfNE3(
			new ReqBeanHeaderIfNE3() {
				@Override
				public String getX() {
					return "foo";
				}
				@Override
				public String getY() {
					return "";
				}
				@Override
				public String getZ() {
					return null;
				}
			}
		);
		assertEquals("OK", r);
	}

	@Test
	public void gg04_reqBeanHeaderIfNE4() throws Exception {
		String r = proxy.reqBeanHeaderIfNE4(
			new ReqBeanHeaderIfNE4() {
				@Override
				public String getX() {
					return "foo";
				}
				@Override
				public String getY() {
					return "";
				}
				@Override
				public String getZ() {
					return null;
				}
			}
		);
		assertEquals("OK", r);
	}

	@Test
	public void gg05_reqBeanHeaderIfNE5() throws Exception {
		String r = proxy.reqBeanHeaderIfNE5(
			new ReqBeanHeaderIfNE5() {
				@Override
				public String getX() {
					return "foo";
				}
				@Override
				public String getY() {
					return "";
				}
				@Override
				public String getZ() {
					return null;
				}
			}
		);
		assertEquals("OK", r);
	}

	@Test
	public void gg06_reqBeanHeaderIfNE6() throws Exception {
		String r = proxy.reqBeanHeaderIfNE6(
			new ReqBeanHeaderIfNE6() {
				@Override
				public Map<String,Object> getX() {
					return new AMap<String,Object>().append("a","foo").append("b","").append("c", null);
				}

			}
		);
		assertEquals("OK", r);
	}

	@Test
	public void gg07_reqBeanHeaderIfNE7() throws Exception {
		String r = proxy.reqBeanHeaderIfNE7(
			new ReqBeanHeaderIfNE7() {
				@Override
				public NeBean getX() {
					return new NeBean().init();
				}
			}
		);
		assertEquals("OK", r);
	}

	//--------------------------------------------------------------------------------
	// PartFormatters
	//--------------------------------------------------------------------------------
	@Test
	public void h01() throws Exception {
		String r = proxy.partFormatters("1", "2", "3", "", "4", "5", "", "6", "7", "");
		assertEquals("OK", r);
	}

	//--------------------------------------------------------------------------------
	// @RemoteableMethod(returns=HTTP_STATUS)
	//--------------------------------------------------------------------------------
	@Test
	public void i01a() throws Exception {
		int r = proxy.httpStatusReturnInt200();
		assertEquals(200, r);
	}

	@Test
	public void i01b() throws Exception {
		Integer r = proxy.httpStatusReturnInteger200();
		assertEquals(200, r.intValue());
	}

	@Test
	public void i01c() throws Exception {
		int r = proxy.httpStatusReturnInt404();
		assertEquals(404, r);
	}

	@Test
	public void i01d() throws Exception {
		Integer r = proxy.httpStatusReturnInteger404();
		assertEquals(404, r.intValue());
	}

	@Test
	public void i02a() throws Exception {
		boolean r = proxy.httpStatusReturnBool200();
		assertEquals(true, r);
	}

	@Test
	public void i02b() throws Exception {
		Boolean r = proxy.httpStatusReturnBoolean200();
		assertEquals(true, r);
	}

	@Test
	public void i02c() throws Exception {
		boolean r = proxy.httpStatusReturnBool404();
		assertEquals(false, r);
	}

	public void i02d() throws Exception {
		Boolean r = proxy.httpStatusReturnBoolean404();
		assertEquals(false, r);
	}

	//--------------------------------------------------------------------------------
	// Proxy class
	//--------------------------------------------------------------------------------

	@Remoteable(path="/testThirdPartyProxy")
	public static interface ThirdPartyProxy {

		//--------------------------------------------------------------------------------
		// Header tests
		//--------------------------------------------------------------------------------

		@RemoteMethod(httpMethod="GET", path="/primitiveHeaders")
		String primitiveHeaders(
			@Header("a") String a,
			@Header("an") String an,
			@Header("b") int b,
			@Header("c") Integer c,
			@Header("cn") Integer cn,
			@Header("d") Boolean d,
			@Header("e") float e,
			@Header("f") Float f
		);

		@RemoteMethod(httpMethod="GET", path="/primitiveCollectionHeaders")
		String primitiveCollectionHeaders(
			@Header("a") int[][][] a,
			@Header("b") Integer[][][] b,
			@Header("c") String[][][] c,
			@Header("d") List<Integer> d,
			@Header("e") List<List<List<Integer>>> e,
			@Header("f") List<Integer[][][]> f,
			@Header("g") List<int[][][]> g,
			@Header("h") List<String> h
		);

		@RemoteMethod(httpMethod="GET", path="/beanHeaders")
		String beanHeaders(
			@Header("a") ABean a,
			@Header("an") ABean an,
			@Header("b") ABean[][][] b,
			@Header("c") List<ABean> c,
			@Header("d") List<ABean[][][]> d,
			@Header("e") Map<String,ABean> e,
			@Header("f") Map<String,List<ABean>> f,
			@Header("g") Map<String,List<ABean[][][]>> g,
			@Header("h") Map<Integer,List<ABean>> h
		);

		@RemoteMethod(httpMethod="GET", path="/typedBeanHeaders")
		String typedBeanHeaders(
			@Header("a") TypedBean a,
			@Header("an") TypedBean an,
			@Header("b") TypedBean[][][] b,
			@Header("c") List<TypedBean> c,
			@Header("d") List<TypedBean[][][]> d,
			@Header("e") Map<String,TypedBean> e,
			@Header("f") Map<String,List<TypedBean>> f,
			@Header("g") Map<String,List<TypedBean[][][]>> g,
			@Header("h") Map<Integer,List<TypedBean>> h
		);

		@RemoteMethod(httpMethod="GET", path="/swappedPojoHeaders")
		String swappedPojoHeaders(
			@Header("a") SwappedPojo a,
			@Header("b") SwappedPojo[][][] b,
			@Header("c") Map<SwappedPojo,SwappedPojo> c,
			@Header("d") Map<SwappedPojo,SwappedPojo[][][]> d
		);

		@RemoteMethod(httpMethod="GET", path="/implicitSwappedPojoHeaders")
		String implicitSwappedPojoHeaders(
			@Header("a") ImplicitSwappedPojo a,
			@Header("b") ImplicitSwappedPojo[][][] b,
			@Header("c") Map<ImplicitSwappedPojo,ImplicitSwappedPojo> c,
			@Header("d") Map<ImplicitSwappedPojo,ImplicitSwappedPojo[][][]> d
		);

		@RemoteMethod(httpMethod="GET", path="/enumHeaders")
		String enumHeaders(
			@Header("a") TestEnum a,
			@Header("an") TestEnum an,
			@Header("b") TestEnum[][][] b,
			@Header("c") List<TestEnum> c,
			@Header("d") List<List<List<TestEnum>>> d,
			@Header("e") List<TestEnum[][][]> e,
			@Header("f") Map<TestEnum,TestEnum> f,
			@Header("g") Map<TestEnum,TestEnum[][][]> g,
			@Header("h") Map<TestEnum,List<TestEnum[][][]>> h
		);

		@RemoteMethod(httpMethod="GET", path="/mapHeader")
		String mapHeader(
			@Header("*") Map<String,Object> a
		);

		@RemoteMethod(httpMethod="GET", path="/beanHeader")
		String beanHeader(
			@Header("*") NeBean a
		);

		@RemoteMethod(httpMethod="GET", path="/nameValuePairsHeader")
		String nameValuePairsHeader(
			@Header("*") NameValuePairs a
		);

		@RemoteMethod(httpMethod="GET", path="/headerIfNE1")
		String headerIfNE1(
			@HeaderIfNE("a") String a
		);

		@RemoteMethod(httpMethod="GET", path="/headerIfNE2")
		String headerIfNE2(
			@HeaderIfNE("a") String a
		);

		@RemoteMethod(httpMethod="GET", path="/headerIfNEMap")
		String headerIfNEMap(
			@HeaderIfNE("*") Map<String,Object> a
		);

		@RemoteMethod(httpMethod="GET", path="/headerIfNEBean")
		String headerIfNEBean(
			@HeaderIfNE("*") NeBean a
		);

		@RemoteMethod(httpMethod="GET", path="/headerIfNEnameValuePairs")
		String headerIfNEnameValuePairs(
			@HeaderIfNE("*") NameValuePairs a
		);


		//--------------------------------------------------------------------------------
		// Query tests
		//--------------------------------------------------------------------------------

		@RemoteMethod(httpMethod="GET", path="/primitiveQueries")
		String primitiveQueries(
			@Query("a") String a,
			@Query("an") String an,
			@Query("b") int b,
			@Query("c") Integer c,
			@Query("cn") Integer cn,
			@Query("d") Boolean d,
			@Query("e") float e,
			@Query("f") Float f
		);

		@RemoteMethod(httpMethod="GET", path="/primitiveCollectionQueries")
		String primitiveCollectionQueries(
			@Query("a") int[][][] a,
			@Query("b") Integer[][][] b,
			@Query("c") String[][][] c,
			@Query("d") List<Integer> d,
			@Query("e") List<List<List<Integer>>> e,
			@Query("f") List<Integer[][][]> f,
			@Query("g") List<int[][][]> g,
			@Query("h") List<String> h
		);

		@RemoteMethod(httpMethod="GET", path="/beanQueries")
		String beanQueries(
			@Query("a") ABean a,
			@Query("an") ABean an,
			@Query("b") ABean[][][] b,
			@Query("c") List<ABean> c,
			@Query("d") List<ABean[][][]> d,
			@Query("e") Map<String,ABean> e,
			@Query("f") Map<String,List<ABean>> f,
			@Query("g") Map<String,List<ABean[][][]>> g,
			@Query("h") Map<Integer,List<ABean>> h
		);

		@RemoteMethod(httpMethod="GET", path="/typedBeanQueries")
		String typedBeanQueries(
			@Query("a") TypedBean a,
			@Query("an") TypedBean an,
			@Query("b") TypedBean[][][] b,
			@Query("c") List<TypedBean> c,
			@Query("d") List<TypedBean[][][]> d,
			@Query("e") Map<String,TypedBean> e,
			@Query("f") Map<String,List<TypedBean>> f,
			@Query("g") Map<String,List<TypedBean[][][]>> g,
			@Query("h") Map<Integer,List<TypedBean>> h
		);

		@RemoteMethod(httpMethod="GET", path="/swappedPojoQueries")
		String swappedPojoQueries(
			@Query("a") SwappedPojo a,
			@Query("b") SwappedPojo[][][] b,
			@Query("c") Map<SwappedPojo,SwappedPojo> c,
			@Query("d") Map<SwappedPojo,SwappedPojo[][][]> d
		);

		@RemoteMethod(httpMethod="GET", path="/implicitSwappedPojoQueries")
		String implicitSwappedPojoQueries(
			@Query("a") ImplicitSwappedPojo a,
			@Query("b") ImplicitSwappedPojo[][][] b,
			@Query("c") Map<ImplicitSwappedPojo,ImplicitSwappedPojo> c,
			@Query("d") Map<ImplicitSwappedPojo,ImplicitSwappedPojo[][][]> d
		);

		@RemoteMethod(httpMethod="GET", path="/enumQueries")
		String enumQueries(
			@Query("a") TestEnum a,
			@Query("an") TestEnum an,
			@Query("b") TestEnum[][][] b,
			@Query("c") List<TestEnum> c,
			@Query("d") List<List<List<TestEnum>>> d,
			@Query("e") List<TestEnum[][][]> e,
			@Query("f") Map<TestEnum,TestEnum> f,
			@Query("g") Map<TestEnum,TestEnum[][][]> g,
			@Query("h") Map<TestEnum,List<TestEnum[][][]>> h
		);

		@RemoteMethod(httpMethod="GET", path="/stringQuery1")
		String stringQuery1(
			@Query String a
		);

		@RemoteMethod(httpMethod="GET", path="/stringQuery2")
		String stringQuery2(
			@Query("*") String a
		);

		@RemoteMethod(httpMethod="GET", path="/mapQuery")
		String mapQuery(
			@Query("*") Map<String,Object> a
		);

		@RemoteMethod(httpMethod="GET", path="/beanQuery")
		String beanQuery(
			@Query("*") NeBean a
		);

		@RemoteMethod(httpMethod="GET", path="/nameValuePairsQuery")
		String nameValuePairsQuery(
			@Query("*") NameValuePairs a
		);

		@RemoteMethod(httpMethod="GET", path="/queryIfNE1")
		String queryIfNE1(
			@QueryIfNE("a") String a
		);

		@RemoteMethod(httpMethod="GET", path="/queryIfNE2")
		String queryIfNE2(
			@QueryIfNE("a") String a
		);

		@RemoteMethod(httpMethod="GET", path="/queryIfNEMap")
		String queryIfNEMap(
			@QueryIfNE("*") Map<String,Object> a
		);

		@RemoteMethod(httpMethod="GET", path="/queryIfNEBean")
		String queryIfNEBean(
			@QueryIfNE("*") NeBean a
		);

		@RemoteMethod(httpMethod="GET", path="/queryIfNEnameValuePairs")
		String queryIfNEnameValuePairs(
			@QueryIfNE("*") NameValuePairs a
		);


		//--------------------------------------------------------------------------------
		// FormData tests
		//--------------------------------------------------------------------------------

		@RemoteMethod(httpMethod="POST", path="/primitiveFormData")
		String primitiveFormData(
			@FormData("a") String a,
			@FormData("an") String an,
			@FormData("b") int b,
			@FormData("c") Integer c,
			@FormData("cn") Integer cn,
			@FormData("d") Boolean d,
			@FormData("e") float e,
			@FormData("f") Float f
		);

		@RemoteMethod(httpMethod="POST", path="/primitiveCollectionFormData")
		String primitiveCollectionFormData(
			@FormData("a") int[][][] a,
			@FormData("b") Integer[][][] b,
			@FormData("c") String[][][] c,
			@FormData("d") List<Integer> d,
			@FormData("e") List<List<List<Integer>>> e,
			@FormData("f") List<Integer[][][]> f,
			@FormData("g") List<int[][][]> g,
			@FormData("h") List<String> h
		);

		@RemoteMethod(httpMethod="POST", path="/beanFormData")
		String beanFormData(
			@FormData("a") ABean a,
			@FormData("an") ABean an,
			@FormData("b") ABean[][][] b,
			@FormData("c") List<ABean> c,
			@FormData("d") List<ABean[][][]> d,
			@FormData("e") Map<String,ABean> e,
			@FormData("f") Map<String,List<ABean>> f,
			@FormData("g") Map<String,List<ABean[][][]>> g,
			@FormData("h") Map<Integer,List<ABean>> h
		);

		@RemoteMethod(httpMethod="POST", path="/typedBeanFormData")
		String typedBeanFormData(
			@FormData("a") TypedBean a,
			@FormData("an") TypedBean an,
			@FormData("b") TypedBean[][][] b,
			@FormData("c") List<TypedBean> c,
			@FormData("d") List<TypedBean[][][]> d,
			@FormData("e") Map<String,TypedBean> e,
			@FormData("f") Map<String,List<TypedBean>> f,
			@FormData("g") Map<String,List<TypedBean[][][]>> g,
			@FormData("h") Map<Integer,List<TypedBean>> h
		);

		@RemoteMethod(httpMethod="POST", path="/swappedPojoFormData")
		String swappedPojoFormData(
			@FormData("a") SwappedPojo a,
			@FormData("b") SwappedPojo[][][] b,
			@FormData("c") Map<SwappedPojo,SwappedPojo> c,
			@FormData("d") Map<SwappedPojo,SwappedPojo[][][]> d
		);

		@RemoteMethod(httpMethod="POST", path="/implicitSwappedPojoFormData")
		String implicitSwappedPojoFormData(
			@FormData("a") ImplicitSwappedPojo a,
			@FormData("b") ImplicitSwappedPojo[][][] b,
			@FormData("c") Map<ImplicitSwappedPojo,ImplicitSwappedPojo> c,
			@FormData("d") Map<ImplicitSwappedPojo,ImplicitSwappedPojo[][][]> d
		);

		@RemoteMethod(httpMethod="POST", path="/enumFormData")
		String enumFormData(
			@FormData("a") TestEnum a,
			@FormData("an") TestEnum an,
			@FormData("b") TestEnum[][][] b,
			@FormData("c") List<TestEnum> c,
			@FormData("d") List<List<List<TestEnum>>> d,
			@FormData("e") List<TestEnum[][][]> e,
			@FormData("f") Map<TestEnum,TestEnum> f,
			@FormData("g") Map<TestEnum,TestEnum[][][]> g,
			@FormData("h") Map<TestEnum,List<TestEnum[][][]>> h
		);

		@RemoteMethod(httpMethod="POST", path="/mapFormData")
		String mapFormData(
			@FormData("*") Map<String,Object> a
		);

		@RemoteMethod(httpMethod="POST", path="/beanFormData2")
		String beanFormData(
			@FormData("*") NeBean a
		);

		@RemoteMethod(httpMethod="POST", path="/nameValuePairsFormData")
		String nameValuePairsFormData(
			@FormData("*") NameValuePairs a
		);

		@RemoteMethod(httpMethod="POST", path="/formDataIfNE1")
		String formDataIfNE1(
			@FormDataIfNE("a") String a
		);

		@RemoteMethod(httpMethod="POST", path="/formDataIfNE2")
		String formDataIfNE2(
			@FormDataIfNE("a") String a
		);

		@RemoteMethod(httpMethod="POST", path="/formDataIfNEMap")
		String formDataIfNEMap(
			@FormDataIfNE("*") Map<String,Object> a
		);

		@RemoteMethod(httpMethod="POST", path="/formDataIfNEBean")
		String formDataIfNEBean(
			@FormDataIfNE("*") NeBean a
		);

		@RemoteMethod(httpMethod="POST", path="/formDataIfNENameValuePairs")
		String formDataIfNENameValuePairs(
			@FormDataIfNE("*") NameValuePairs a
		);

		//--------------------------------------------------------------------------------
		// Path tests
		//--------------------------------------------------------------------------------

		@RemoteMethod(httpMethod="POST", path="/pathVars1/{a}/{b}")
		String pathVars1(
			@Path("a") int a,
			@Path("b") String b
		);

		@RemoteMethod(httpMethod="POST", path="/pathVars2/{a}/{b}")
		String pathVars2(
			@Path Map<String,Object> a
		);

		@RemoteMethod(httpMethod="POST", path="/pathVars3/{a}/{b}")
		String pathVars3(
			@Path ABean a
		);

		//--------------------------------------------------------------------------------
		// RequestBean tests - Path
		//--------------------------------------------------------------------------------

		@RemoteMethod(httpMethod="POST", path="/reqBeanPath/{a}/{b}")
		String reqBeanPath1(
			@RequestBean ReqBeanPath1 rb
		);

		public static interface ReqBeanPath1 {
			@Path
			int getA();

			@Path
			String getB();
		}

		public static class ReqBeanPath1Impl implements ReqBeanPath1 {
			@Override
			public int getA() {
				return 1;
			}
			@Override
			public String getB() {
				return "foo";
			}
		}


		@RemoteMethod(httpMethod="POST", path="/reqBeanPath/{a}/{b}")
		String reqBeanPath2(
			@RequestBean ReqBeanPath2 rb
		);

		public static class ReqBeanPath2 {
			@Path
			public int getA() {
				return 1;
			};

			@Path
			public String getB() {
				return "foo";
			}
		}

		@RemoteMethod(httpMethod="POST", path="/reqBeanPath/{a}/{b}")
		String reqBeanPath3(
			@RequestBean ReqBeanPath3 rb
		);

		public static interface ReqBeanPath3 {
			@Path("a")
			int getX();

			@Path("b")
			String getY();
		}

		@RemoteMethod(httpMethod="POST", path="/reqBeanPath/{a}/{b}")
		String reqBeanPath4(
			@RequestBean ReqBeanPath4 rb
		);

		public static interface ReqBeanPath4 {
			@Path
			@BeanProperty(name="a")
			int getX();

			@Path
			@BeanProperty(name="b")
			String getY();
		}

		@RemoteMethod(httpMethod="POST", path="/reqBeanPath/{a}/{b}")
		String reqBeanPath5(
			@RequestBean ReqBeanPath5 rb
		);

		public static interface ReqBeanPath5 {
			@Path
			@BeanProperty(name="a")
			int getX();

			@Path
			@BeanProperty(name="b")
			String getY();
		}

		@RemoteMethod(httpMethod="POST", path="/reqBeanPath/{a}/{b}")
		String reqBeanPath6(
			@RequestBean ReqBeanPath6 rb
		);

		public static interface ReqBeanPath6 {
			@Path
			Map<String,Object> getX();
		}

		@RemoteMethod(httpMethod="POST", path="/reqBeanPath/{a}/{b}")
		String reqBeanPath7(
			@RequestBean ReqBeanPath7 rb
		);

		public static interface ReqBeanPath7 {
			@Path
			ABean getX();
		}

		//--------------------------------------------------------------------------------
		// RequestBean tests - Query
		//--------------------------------------------------------------------------------

		@RemoteMethod(httpMethod="POST", path="/reqBeanQuery")
		String reqBeanQuery1(
			@RequestBean ReqBeanQuery1 rb
		);

		public static interface ReqBeanQuery1 {
			@Query
			int getA();

			@Query
			String getB();
		}

		public static class ReqBeanQuery1Impl implements ReqBeanQuery1 {
			@Override
			public int getA() {
				return 1;
			}
			@Override
			public String getB() {
				return "foo";
			}
		}


		@RemoteMethod(httpMethod="POST", path="/reqBeanQuery")
		String reqBeanQuery2(
			@RequestBean ReqBeanQuery2 rb
		);

		public static class ReqBeanQuery2 {
			@Query
			public int getA() {
				return 1;
			};

			@Query
			public String getB() {
				return "foo";
			}
		}

		@RemoteMethod(httpMethod="POST", path="/reqBeanQuery")
		String reqBeanQuery3(
			@RequestBean ReqBeanQuery3 rb
		);

		public static interface ReqBeanQuery3 {
			@Query("a")
			int getX();

			@Query("b")
			String getY();
		}

		@RemoteMethod(httpMethod="POST", path="/reqBeanQuery")
		String reqBeanQuery4(
			@RequestBean ReqBeanQuery4 rb
		);

		public static interface ReqBeanQuery4 {
			@Query
			@BeanProperty(name="a")
			int getX();

			@Query
			@BeanProperty(name="b")
			String getY();
		}

		@RemoteMethod(httpMethod="POST", path="/reqBeanQuery")
		String reqBeanQuery5(
			@RequestBean ReqBeanQuery5 rb
		);

		public static interface ReqBeanQuery5 {
			@Query
			@BeanProperty(name="a")
			int getX();

			@Query
			@BeanProperty(name="b")
			String getY();
		}

		@RemoteMethod(httpMethod="POST", path="/reqBeanQuery")
		String reqBeanQuery6(
			@RequestBean ReqBeanQuery6 rb
		);

		public static interface ReqBeanQuery6 {
			@Query
			Map<String,Object> getX();
		}

		@RemoteMethod(httpMethod="POST", path="/reqBeanQuery")
		String reqBeanQuery7(
			@RequestBean ReqBeanQuery7 rb
		);

		public static interface ReqBeanQuery7 {
			@Query
			ABean getX();
		}

		//--------------------------------------------------------------------------------
		// RequestBean tests - QueryIfNE
		//--------------------------------------------------------------------------------

		@RemoteMethod(httpMethod="POST", path="/reqBeanQueryIfNE")
		String reqBeanQueryIfNE1(
			@RequestBean ReqBeanQueryIfNE1 rb
		);

		public static interface ReqBeanQueryIfNE1 {
			@QueryIfNE
			String getA();

			@QueryIfNE
			String getB();

			@QueryIfNE
			String getC();
		}

		public static class ReqBeanQueryIfNE1Impl implements ReqBeanQueryIfNE1 {
			@Override
			public String getA() {
				return "foo";
			}
			@Override
			public String getB() {
				return "";
			}
			@Override
			public String getC() {
				return null;
			}
		}


		@RemoteMethod(httpMethod="POST", path="/reqBeanQueryIfNE")
		String reqBeanQueryIfNE2(
			@RequestBean ReqBeanQueryIfNE2 rb
		);

		public static class ReqBeanQueryIfNE2 {
			@QueryIfNE
			public String getA() {
				return "foo";
			};
			@QueryIfNE
			public String getB() {
				return "";
			}
			@QueryIfNE
			public String getC() {
				return null;
			}
		}

		@RemoteMethod(httpMethod="POST", path="/reqBeanQueryIfNE")
		String reqBeanQueryIfNE3(
			@RequestBean ReqBeanQueryIfNE3 rb
		);

		public static interface ReqBeanQueryIfNE3 {
			@QueryIfNE("a")
			String getX();

			@QueryIfNE("b")
			String getY();

			@QueryIfNE("c")
			String getZ();
		}

		@RemoteMethod(httpMethod="POST", path="/reqBeanQueryIfNE")
		String reqBeanQueryIfNE4(
			@RequestBean ReqBeanQueryIfNE4 rb
		);

		public static interface ReqBeanQueryIfNE4 {
			@QueryIfNE
			@BeanProperty(name="a")
			String getX();

			@QueryIfNE
			@BeanProperty(name="b")
			String getY();

			@QueryIfNE
			@BeanProperty(name="c")
			String getZ();
		}

		@RemoteMethod(httpMethod="POST", path="/reqBeanQueryIfNE")
		String reqBeanQueryIfNE5(
			@RequestBean ReqBeanQueryIfNE5 rb
		);

		public static interface ReqBeanQueryIfNE5 {
			@QueryIfNE
			@BeanProperty(name="a")
			String getX();

			@QueryIfNE
			@BeanProperty(name="b")
			String getY();

			@QueryIfNE
			@BeanProperty(name="c")
			String getZ();
		}

		@RemoteMethod(httpMethod="POST", path="/reqBeanQueryIfNE")
		String reqBeanQueryIfNE6(
			@RequestBean ReqBeanQueryIfNE6 rb
		);

		public static interface ReqBeanQueryIfNE6 {
			@QueryIfNE
			Map<String,Object> getX();
		}

		@RemoteMethod(httpMethod="POST", path="/reqBeanQueryIfNE")
		String reqBeanQueryIfNE7(
			@RequestBean ReqBeanQueryIfNE7 rb
		);

		public static interface ReqBeanQueryIfNE7 {
			@QueryIfNE
			NeBean getX();
		}

		//--------------------------------------------------------------------------------
		// RequestBean tests - FormData
		//--------------------------------------------------------------------------------

		@RemoteMethod(httpMethod="POST", path="/reqBeanFormData")
		String reqBeanFormData1(
			@RequestBean ReqBeanFormData1 rb
		);

		public static interface ReqBeanFormData1 {
			@FormData
			int getA();

			@FormData
			String getB();
		}

		public static class ReqBeanFormData1Impl implements ReqBeanFormData1 {
			@Override
			public int getA() {
				return 1;
			}
			@Override
			public String getB() {
				return "foo";
			}
		}


		@RemoteMethod(httpMethod="POST", path="/reqBeanFormData")
		String reqBeanFormData2(
			@RequestBean ReqBeanFormData2 rb
		);

		public static class ReqBeanFormData2 {
			@FormData
			public int getA() {
				return 1;
			};

			@FormData
			public String getB() {
				return "foo";
			}
		}

		@RemoteMethod(httpMethod="POST", path="/reqBeanFormData")
		String reqBeanFormData3(
			@RequestBean ReqBeanFormData3 rb
		);

		public static interface ReqBeanFormData3 {
			@FormData("a")
			int getX();

			@FormData("b")
			String getY();
		}

		@RemoteMethod(httpMethod="POST", path="/reqBeanFormData")
		String reqBeanFormData4(
			@RequestBean ReqBeanFormData4 rb
		);

		public static interface ReqBeanFormData4 {
			@FormData
			@BeanProperty(name="a")
			int getX();

			@FormData
			@BeanProperty(name="b")
			String getY();
		}

		@RemoteMethod(httpMethod="POST", path="/reqBeanFormData")
		String reqBeanFormData5(
			@RequestBean ReqBeanFormData5 rb
		);

		public static interface ReqBeanFormData5 {
			@FormData
			@BeanProperty(name="a")
			int getX();

			@FormData
			@BeanProperty(name="b")
			String getY();
		}

		@RemoteMethod(httpMethod="POST", path="/reqBeanFormData")
		String reqBeanFormData6(
			@RequestBean ReqBeanFormData6 rb
		);

		public static interface ReqBeanFormData6 {
			@FormData
			Map<String,Object> getX();
		}

		@RemoteMethod(httpMethod="POST", path="/reqBeanFormData")
		String reqBeanFormData7(
			@RequestBean ReqBeanFormData7 rb
		);

		public static interface ReqBeanFormData7 {
			@FormData
			ABean getX();
		}

		//--------------------------------------------------------------------------------
		// RequestBean tests - FormDataIfNE
		//--------------------------------------------------------------------------------

		@RemoteMethod(httpMethod="POST", path="/reqBeanFormDataIfNE")
		String reqBeanFormDataIfNE1(
			@RequestBean ReqBeanFormDataIfNE1 rb
		);

		public static interface ReqBeanFormDataIfNE1 {
			@FormDataIfNE
			String getA();

			@FormDataIfNE
			String getB();

			@FormDataIfNE
			String getC();
		}

		public static class ReqBeanFormDataIfNE1Impl implements ReqBeanFormDataIfNE1 {
			@Override
			public String getA() {
				return "foo";
			}
			@Override
			public String getB() {
				return "";
			}
			@Override
			public String getC() {
				return null;
			}
		}


		@RemoteMethod(httpMethod="POST", path="/reqBeanFormDataIfNE")
		String reqBeanFormDataIfNE2(
			@RequestBean ReqBeanFormDataIfNE2 rb
		);

		public static class ReqBeanFormDataIfNE2 {
			@FormDataIfNE
			public String getA() {
				return "foo";
			};
			@FormDataIfNE
			public String getB() {
				return "";
			}
			@FormDataIfNE
			public String getC() {
				return null;
			}
		}

		@RemoteMethod(httpMethod="POST", path="/reqBeanFormDataIfNE")
		String reqBeanFormDataIfNE3(
			@RequestBean ReqBeanFormDataIfNE3 rb
		);

		public static interface ReqBeanFormDataIfNE3 {
			@FormDataIfNE("a")
			String getX();

			@FormDataIfNE("b")
			String getY();

			@FormDataIfNE("c")
			String getZ();
		}

		@RemoteMethod(httpMethod="POST", path="/reqBeanFormDataIfNE")
		String reqBeanFormDataIfNE4(
			@RequestBean ReqBeanFormDataIfNE4 rb
		);

		public static interface ReqBeanFormDataIfNE4 {
			@FormDataIfNE
			@BeanProperty(name="a")
			String getX();

			@FormDataIfNE
			@BeanProperty(name="b")
			String getY();

			@FormDataIfNE
			@BeanProperty(name="c")
			String getZ();
		}

		@RemoteMethod(httpMethod="POST", path="/reqBeanFormDataIfNE")
		String reqBeanFormDataIfNE5(
			@RequestBean ReqBeanFormDataIfNE5 rb
		);

		public static interface ReqBeanFormDataIfNE5 {
			@FormDataIfNE
			@BeanProperty(name="a")
			String getX();

			@FormDataIfNE
			@BeanProperty(name="b")
			String getY();

			@FormDataIfNE
			@BeanProperty(name="c")
			String getZ();
		}

		@RemoteMethod(httpMethod="POST", path="/reqBeanFormDataIfNE")
		String reqBeanFormDataIfNE6(
			@RequestBean ReqBeanFormDataIfNE6 rb
		);

		public static interface ReqBeanFormDataIfNE6 {
			@FormDataIfNE
			Map<String,Object> getX();
		}

		@RemoteMethod(httpMethod="POST", path="/reqBeanFormDataIfNE")
		String reqBeanFormDataIfNE7(
			@RequestBean ReqBeanFormDataIfNE7 rb
		);

		public static interface ReqBeanFormDataIfNE7 {
			@FormDataIfNE
			NeBean getX();
		}

		//--------------------------------------------------------------------------------
		// RequestBean tests - Header
		//--------------------------------------------------------------------------------

		@RemoteMethod(httpMethod="POST", path="/reqBeanHeader")
		String reqBeanHeader1(
			@RequestBean ReqBeanHeader1 rb
		);

		public static interface ReqBeanHeader1 {
			@Header
			int getA();

			@Header
			String getB();
		}

		public static class ReqBeanHeader1Impl implements ReqBeanHeader1 {
			@Override
			public int getA() {
				return 1;
			}
			@Override
			public String getB() {
				return "foo";
			}
		}


		@RemoteMethod(httpMethod="POST", path="/reqBeanHeader")
		String reqBeanHeader2(
			@RequestBean ReqBeanHeader2 rb
		);

		public static class ReqBeanHeader2 {
			@Header
			public int getA() {
				return 1;
			};

			@Header
			public String getB() {
				return "foo";
			}
		}

		@RemoteMethod(httpMethod="POST", path="/reqBeanHeader")
		String reqBeanHeader3(
			@RequestBean ReqBeanHeader3 rb
		);

		public static interface ReqBeanHeader3 {
			@Header("a")
			int getX();

			@Header("b")
			String getY();
		}

		@RemoteMethod(httpMethod="POST", path="/reqBeanHeader")
		String reqBeanHeader4(
			@RequestBean ReqBeanHeader4 rb
		);

		public static interface ReqBeanHeader4 {
			@Header
			@BeanProperty(name="a")
			int getX();

			@Header
			@BeanProperty(name="b")
			String getY();
		}

		@RemoteMethod(httpMethod="POST", path="/reqBeanHeader")
		String reqBeanHeader5(
			@RequestBean ReqBeanHeader5 rb
		);

		public static interface ReqBeanHeader5 {
			@Header
			@BeanProperty(name="a")
			int getX();

			@Header
			@BeanProperty(name="b")
			String getY();
		}

		@RemoteMethod(httpMethod="POST", path="/reqBeanHeader")
		String reqBeanHeader6(
			@RequestBean ReqBeanHeader6 rb
		);

		public static interface ReqBeanHeader6 {
			@Header
			Map<String,Object> getX();
		}

		@RemoteMethod(httpMethod="POST", path="/reqBeanHeader")
		String reqBeanHeader7(
			@RequestBean ReqBeanHeader7 rb
		);

		public static interface ReqBeanHeader7 {
			@Header
			ABean getX();
		}

		//--------------------------------------------------------------------------------
		// RequestBean tests - HeaderIfNE
		//--------------------------------------------------------------------------------

		@RemoteMethod(httpMethod="POST", path="/reqBeanHeaderIfNE")
		String reqBeanHeaderIfNE1(
			@RequestBean ReqBeanHeaderIfNE1 rb
		);

		public static interface ReqBeanHeaderIfNE1 {
			@HeaderIfNE
			String getA();

			@HeaderIfNE
			String getB();

			@HeaderIfNE
			String getC();
		}

		public static class ReqBeanHeaderIfNE1Impl implements ReqBeanHeaderIfNE1 {
			@Override
			public String getA() {
				return "foo";
			}
			@Override
			public String getB() {
				return "";
			}
			@Override
			public String getC() {
				return null;
			}
		}


		@RemoteMethod(httpMethod="POST", path="/reqBeanHeaderIfNE")
		String reqBeanHeaderIfNE2(
			@RequestBean ReqBeanHeaderIfNE2 rb
		);

		public static class ReqBeanHeaderIfNE2 {
			@HeaderIfNE
			public String getA() {
				return "foo";
			};
			@HeaderIfNE
			public String getB() {
				return "";
			}
			@HeaderIfNE
			public String getC() {
				return null;
			}
		}

		@RemoteMethod(httpMethod="POST", path="/reqBeanHeaderIfNE")
		String reqBeanHeaderIfNE3(
			@RequestBean ReqBeanHeaderIfNE3 rb
		);

		public static interface ReqBeanHeaderIfNE3 {
			@HeaderIfNE("a")
			String getX();

			@HeaderIfNE("b")
			String getY();

			@HeaderIfNE("c")
			String getZ();
		}

		@RemoteMethod(httpMethod="POST", path="/reqBeanHeaderIfNE")
		String reqBeanHeaderIfNE4(
			@RequestBean ReqBeanHeaderIfNE4 rb
		);

		public static interface ReqBeanHeaderIfNE4 {
			@HeaderIfNE
			@BeanProperty(name="a")
			String getX();

			@HeaderIfNE
			@BeanProperty(name="b")
			String getY();

			@HeaderIfNE
			@BeanProperty(name="c")
			String getZ();
		}

		@RemoteMethod(httpMethod="POST", path="/reqBeanHeaderIfNE")
		String reqBeanHeaderIfNE5(
			@RequestBean ReqBeanHeaderIfNE5 rb
		);

		public static interface ReqBeanHeaderIfNE5 {
			@HeaderIfNE
			@BeanProperty(name="a")
			String getX();

			@HeaderIfNE
			@BeanProperty(name="b")
			String getY();

			@HeaderIfNE
			@BeanProperty(name="c")
			String getZ();
		}

		@RemoteMethod(httpMethod="POST", path="/reqBeanHeaderIfNE")
		String reqBeanHeaderIfNE6(
			@RequestBean ReqBeanHeaderIfNE6 rb
		);

		public static interface ReqBeanHeaderIfNE6 {
			@HeaderIfNE
			Map<String,Object> getX();
		}

		@RemoteMethod(httpMethod="POST", path="/reqBeanHeaderIfNE")
		String reqBeanHeaderIfNE7(
			@RequestBean ReqBeanHeaderIfNE7 rb
		);

		public static interface ReqBeanHeaderIfNE7 {
			@HeaderIfNE
			NeBean getX();
		}

		//--------------------------------------------------------------------------------
		// PartFormatters
		//--------------------------------------------------------------------------------

		@RemoteMethod(httpMethod="POST", path="/partFormatters/{p1}")
		String partFormatters(
			@Path(value="p1", serializer=DummyPartSerializer.class) String p1,
			@Header(value="h1", serializer=DummyPartSerializer.class) String h1,
			@HeaderIfNE(value="h2", serializer=DummyPartSerializer.class) String h2,
			@HeaderIfNE(value="h3", serializer=DummyPartSerializer.class) String h3,
			@Query(value="q1", serializer=DummyPartSerializer.class) String q1,
			@QueryIfNE(value="q2", serializer=DummyPartSerializer.class) String q2,
			@QueryIfNE(value="q3", serializer=DummyPartSerializer.class) String q3,
			@FormData(value="f1", serializer=DummyPartSerializer.class) String f1,
			@FormDataIfNE(value="f2", serializer=DummyPartSerializer.class) String f2,
			@FormDataIfNE(value="f3", serializer=DummyPartSerializer.class) String f3
		);

		//--------------------------------------------------------------------------------
		// Test return types.
		//--------------------------------------------------------------------------------

		// Various primitives

		@RemoteMethod(httpMethod="GET", path="/returnVoid")
		void returnVoid();

		@RemoteMethod(httpMethod="GET", path="/returnInt")
		int returnInt();

		@RemoteMethod(httpMethod="GET", path="/returnInteger")
		Integer returnInteger();

		@RemoteMethod(httpMethod="GET", path="/returnBoolean")
		boolean returnBoolean();

		@RemoteMethod(httpMethod="GET", path="/returnFloat")
		float returnFloat();

		@RemoteMethod(httpMethod="GET", path="/returnFloatObject")
		Float returnFloatObject();

		@RemoteMethod(httpMethod="GET", path="/returnString")
		String returnString();

		@RemoteMethod(httpMethod="GET", path="/returnNullString")
		String returnNullString();

		@RemoteMethod(httpMethod="GET", path="/returnInt3dArray")
		int[][][] returnInt3dArray();

		@RemoteMethod(httpMethod="GET", path="/returnInteger3dArray")
		Integer[][][] returnInteger3dArray();

		@RemoteMethod(httpMethod="GET", path="/returnString3dArray")
		String[][][] returnString3dArray();

		@RemoteMethod(httpMethod="GET", path="/returnIntegerList")
		List<Integer> returnIntegerList();

		@RemoteMethod(httpMethod="GET", path="/returnInteger3dList")
		List<List<List<Integer>>> returnInteger3dList();

		@RemoteMethod(httpMethod="GET", path="/returnInteger1d3dList")
		List<Integer[][][]> returnInteger1d3dList();

		@RemoteMethod(httpMethod="GET", path="/returnInt1d3dList")
		List<int[][][]> returnInt1d3dList();

		@RemoteMethod(httpMethod="GET", path="/returnStringList")
		List<String> returnStringList();

		// Beans

		@RemoteMethod(httpMethod="GET", path="/returnBean")
		ABean returnBean();

		@RemoteMethod(httpMethod="GET", path="/returnBean3dArray")
		ABean[][][] returnBean3dArray();

		@RemoteMethod(httpMethod="GET", path="/returnBeanList")
		List<ABean> returnBeanList();

		@RemoteMethod(httpMethod="GET", path="/returnBean1d3dList")
		List<ABean[][][]> returnBean1d3dList();

		@RemoteMethod(httpMethod="GET", path="/returnBeanMap")
		Map<String,ABean> returnBeanMap();

		@RemoteMethod(httpMethod="GET", path="/returnBeanListMap")
		Map<String,List<ABean>> returnBeanListMap();

		@RemoteMethod(httpMethod="GET", path="/returnBean1d3dListMap")
		Map<String,List<ABean[][][]>> returnBean1d3dListMap();

		@RemoteMethod(httpMethod="GET", path="/returnBeanListMapIntegerKeys")
		Map<Integer,List<ABean>> returnBeanListMapIntegerKeys();

		// Typed beans

		@RemoteMethod(httpMethod="GET", path="/returnTypedBean")
		TypedBean returnTypedBean();

		@RemoteMethod(httpMethod="GET", path="/returnTypedBean3dArray")
		TypedBean[][][] returnTypedBean3dArray();

		@RemoteMethod(httpMethod="GET", path="/returnTypedBeanList")
		List<TypedBean> returnTypedBeanList();

		@RemoteMethod(httpMethod="GET", path="/returnTypedBean1d3dList")
		List<TypedBean[][][]> returnTypedBean1d3dList();

		@RemoteMethod(httpMethod="GET", path="/returnTypedBeanMap")
		Map<String,TypedBean> returnTypedBeanMap();

		@RemoteMethod(httpMethod="GET", path="/returnTypedBeanListMap")
		Map<String,List<TypedBean>> returnTypedBeanListMap();

		@RemoteMethod(httpMethod="GET", path="/returnTypedBean1d3dListMap")
		Map<String,List<TypedBean[][][]>> returnTypedBean1d3dListMap();

		@RemoteMethod(httpMethod="GET", path="/returnTypedBeanListMapIntegerKeys")
		Map<Integer,List<TypedBean>> returnTypedBeanListMapIntegerKeys();

		// Swapped POJOs

		@RemoteMethod(httpMethod="GET", path="/returnSwappedPojo")
		SwappedPojo returnSwappedPojo();

		@RemoteMethod(httpMethod="GET", path="/returnSwappedPojo3dArray")
		SwappedPojo[][][] returnSwappedPojo3dArray();

		@RemoteMethod(httpMethod="GET", path="/returnSwappedPojoMap")
		Map<SwappedPojo,SwappedPojo> returnSwappedPojoMap();

		@RemoteMethod(httpMethod="GET", path="/returnSwappedPojo3dMap")
		Map<SwappedPojo,SwappedPojo[][][]> returnSwappedPojo3dMap();

		// Implicit swapped POJOs

		@RemoteMethod(httpMethod="GET", path="/returnImplicitSwappedPojo")
		ImplicitSwappedPojo returnImplicitSwappedPojo();

		@RemoteMethod(httpMethod="GET", path="/returnImplicitSwappedPojo3dArray")
		ImplicitSwappedPojo[][][] returnImplicitSwappedPojo3dArray();

		@RemoteMethod(httpMethod="GET", path="/returnImplicitSwappedPojoMap")
		Map<ImplicitSwappedPojo,ImplicitSwappedPojo> returnImplicitSwappedPojoMap();

		@RemoteMethod(httpMethod="GET", path="/returnImplicitSwappedPojo3dMap")
		Map<ImplicitSwappedPojo,ImplicitSwappedPojo[][][]> returnImplicitSwappedPojo3dMap();

		// Enums

		@RemoteMethod(httpMethod="GET", path="/returnEnum")
		TestEnum returnEnum();

		@RemoteMethod(httpMethod="GET", path="/returnEnum3d")
		TestEnum[][][] returnEnum3d();

		@RemoteMethod(httpMethod="GET", path="/returnEnumList")
		List<TestEnum> returnEnumList();

		@RemoteMethod(httpMethod="GET", path="/returnEnum3dList")
		List<List<List<TestEnum>>> returnEnum3dList();

		@RemoteMethod(httpMethod="GET", path="/returnEnum1d3dList")
		List<TestEnum[][][]> returnEnum1d3dList();

		@RemoteMethod(httpMethod="GET", path="/returnEnumMap")
		Map<TestEnum,TestEnum> returnEnumMap();

		@RemoteMethod(httpMethod="GET", path="/returnEnum3dArrayMap")
		Map<TestEnum,TestEnum[][][]> returnEnum3dArrayMap();

		@RemoteMethod(httpMethod="GET", path="/returnEnum1d3dListMap")
		Map<TestEnum,List<TestEnum[][][]>> returnEnum1d3dListMap();

		//--------------------------------------------------------------------------------
		// Test parameters
		//--------------------------------------------------------------------------------

		// Various primitives

		@RemoteMethod(httpMethod="POST", path="/setInt")
		void setInt(@Body int x);

		@RemoteMethod(httpMethod="POST", path="/setInteger")
		void setInteger(@Body Integer x);

		@RemoteMethod(httpMethod="POST", path="/setBoolean")
		void setBoolean(@Body boolean x);

		@RemoteMethod(httpMethod="POST", path="/setFloat")
		void setFloat(@Body float x);

		@RemoteMethod(httpMethod="POST", path="/setFloatObject")
		void setFloatObject(@Body Float x);

		@RemoteMethod(httpMethod="POST", path="/setString")
		void setString(@Body String x);

		@RemoteMethod(httpMethod="POST", path="/setNullString")
		void setNullString(@Body String x);

		@RemoteMethod(httpMethod="POST", path="/setInt3dArray")
		String setInt3dArray(@Body int[][][] x, @org.apache.juneau.remoteable.Query("I") int i);

		@RemoteMethod(httpMethod="POST", path="/setInteger3dArray")
		void setInteger3dArray(@Body Integer[][][] x);

		@RemoteMethod(httpMethod="POST", path="/setString3dArray")
		void setString3dArray(@Body String[][][] x);

		@RemoteMethod(httpMethod="POST", path="/setIntegerList")
		void setIntegerList(@Body List<Integer> x);

		@RemoteMethod(httpMethod="POST", path="/setInteger3dList")
		void setInteger3dList(@Body List<List<List<Integer>>> x);

		@RemoteMethod(httpMethod="POST", path="/setInteger1d3dList")
		void setInteger1d3dList(@Body List<Integer[][][]> x);

		@RemoteMethod(httpMethod="POST", path="/setInt1d3dList")
		void setInt1d3dList(@Body List<int[][][]> x);

		@RemoteMethod(httpMethod="POST", path="/setStringList")
		void setStringList(@Body List<String> x);

		// Beans

		@RemoteMethod(httpMethod="POST", path="/setBean")
		void setBean(@Body ABean x);

		@RemoteMethod(httpMethod="POST", path="/setBean3dArray")
		void setBean3dArray(@Body ABean[][][] x);

		@RemoteMethod(httpMethod="POST", path="/setBeanList")
		void setBeanList(@Body List<ABean> x);

		@RemoteMethod(httpMethod="POST", path="/setBean1d3dList")
		void setBean1d3dList(@Body List<ABean[][][]> x);

		@RemoteMethod(httpMethod="POST", path="/setBeanMap")
		void setBeanMap(@Body Map<String,ABean> x);

		@RemoteMethod(httpMethod="POST", path="/setBeanListMap")
		void setBeanListMap(@Body Map<String,List<ABean>> x);

		@RemoteMethod(httpMethod="POST", path="/setBean1d3dListMap")
		void setBean1d3dListMap(@Body Map<String,List<ABean[][][]>> x);

		@RemoteMethod(httpMethod="POST", path="/setBeanListMapIntegerKeys")
		void setBeanListMapIntegerKeys(@Body Map<Integer,List<ABean>> x);

		// Typed beans

		@RemoteMethod(httpMethod="POST", path="/setTypedBean")
		void setTypedBean(@Body TypedBean x);

		@RemoteMethod(httpMethod="POST", path="/setTypedBean3dArray")
		void setTypedBean3dArray(@Body TypedBean[][][] x);

		@RemoteMethod(httpMethod="POST", path="/setTypedBeanList")
		void setTypedBeanList(@Body List<TypedBean> x);

		@RemoteMethod(httpMethod="POST", path="/setTypedBean1d3dList")
		void setTypedBean1d3dList(@Body List<TypedBean[][][]> x);

		@RemoteMethod(httpMethod="POST", path="/setTypedBeanMap")
		void setTypedBeanMap(@Body Map<String,TypedBean> x);

		@RemoteMethod(httpMethod="POST", path="/setTypedBeanListMap")
		void setTypedBeanListMap(@Body Map<String,List<TypedBean>> x);

		@RemoteMethod(httpMethod="POST", path="/setTypedBean1d3dListMap")
		void setTypedBean1d3dListMap(@Body Map<String,List<TypedBean[][][]>> x);

		@RemoteMethod(httpMethod="POST", path="/setTypedBeanListMapIntegerKeys")
		void setTypedBeanListMapIntegerKeys(@Body Map<Integer,List<TypedBean>> x);

		// Swapped POJOs

		@RemoteMethod(httpMethod="POST", path="/setSwappedPojo")
		void setSwappedPojo(@Body SwappedPojo x);

		@RemoteMethod(httpMethod="POST", path="/setSwappedPojo3dArray")
		void setSwappedPojo3dArray(@Body SwappedPojo[][][] x);

		@RemoteMethod(httpMethod="POST", path="/setSwappedPojoMap")
		void setSwappedPojoMap(@Body Map<SwappedPojo,SwappedPojo> x);

		@RemoteMethod(httpMethod="POST", path="/setSwappedPojo3dMap")
		void setSwappedPojo3dMap(@Body Map<SwappedPojo,SwappedPojo[][][]> x);

		// Implicit swapped POJOs

		@RemoteMethod(httpMethod="POST", path="/setImplicitSwappedPojo")
		void setImplicitSwappedPojo(@Body ImplicitSwappedPojo x);

		@RemoteMethod(httpMethod="POST", path="/setImplicitSwappedPojo3dArray")
		void setImplicitSwappedPojo3dArray(@Body ImplicitSwappedPojo[][][] x);

		@RemoteMethod(httpMethod="POST", path="/setImplicitSwappedPojoMap")
		void setImplicitSwappedPojoMap(@Body Map<ImplicitSwappedPojo,ImplicitSwappedPojo> x);

		@RemoteMethod(httpMethod="POST", path="/setImplicitSwappedPojo3dMap")
		void setImplicitSwappedPojo3dMap(@Body Map<ImplicitSwappedPojo,ImplicitSwappedPojo[][][]> x);

		// Enums

		@RemoteMethod(httpMethod="POST", path="/setEnum")
		void setEnum(@Body TestEnum x);

		@RemoteMethod(httpMethod="POST", path="/setEnum3d")
		void setEnum3d(@Body TestEnum[][][] x);

		@RemoteMethod(httpMethod="POST", path="/setEnumList")
		void setEnumList(@Body List<TestEnum> x);

		@RemoteMethod(httpMethod="POST", path="/setEnum3dList")
		void setEnum3dList(@Body List<List<List<TestEnum>>> x);

		@RemoteMethod(httpMethod="POST", path="/setEnum1d3dList")
		void setEnum1d3dList(@Body List<TestEnum[][][]> x);

		@RemoteMethod(httpMethod="POST", path="/setEnumMap")
		void setEnumMap(@Body Map<TestEnum,TestEnum> x);

		@RemoteMethod(httpMethod="POST", path="/setEnum3dArrayMap")
		void setEnum3dArrayMap(@Body Map<TestEnum,TestEnum[][][]> x);

		@RemoteMethod(httpMethod="POST", path="/setEnum1d3dListMap")
		void setEnum1d3dListMap(@Body Map<TestEnum,List<TestEnum[][][]>> x);

		// Method returns status code

		@RemoteMethod(httpMethod="GET", path="/httpStatusReturn200", returns=ReturnValue.HTTP_STATUS)
		int httpStatusReturnInt200();

		@RemoteMethod(httpMethod="GET", path="/httpStatusReturn200", returns=ReturnValue.HTTP_STATUS)
		Integer httpStatusReturnInteger200();

		@RemoteMethod(httpMethod="GET", path="/httpStatusReturn404", returns=ReturnValue.HTTP_STATUS)
		int httpStatusReturnInt404();

		@RemoteMethod(httpMethod="GET", path="/httpStatusReturn404", returns=ReturnValue.HTTP_STATUS)
		Integer httpStatusReturnInteger404();

		@RemoteMethod(httpMethod="GET", path="/httpStatusReturn200", returns=ReturnValue.HTTP_STATUS)
		boolean httpStatusReturnBool200();

		@RemoteMethod(httpMethod="GET", path="/httpStatusReturn200", returns=ReturnValue.HTTP_STATUS)
		Boolean httpStatusReturnBoolean200();

		@RemoteMethod(httpMethod="GET", path="/httpStatusReturn404", returns=ReturnValue.HTTP_STATUS)
		boolean httpStatusReturnBool404();

		@RemoteMethod(httpMethod="GET", path="/httpStatusReturn404", returns=ReturnValue.HTTP_STATUS)
		Boolean httpStatusReturnBoolean404();
	}

	// Bean for testing NE annotations.
	public static class NeBean {
		public String a, b, c;

		public NeBean init() {
			this.a = "foo";
			this.b = "";
			this.c = null;
			return this;
		}
	}

	public static class DummyPartSerializer implements HttpPartSerializer {
		@Override
		public String serialize(HttpPartType type, Object value) {
			return "dummy-"+value;
		}
	}
}
