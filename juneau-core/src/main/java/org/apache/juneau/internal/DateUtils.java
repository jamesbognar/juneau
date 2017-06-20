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
package org.apache.juneau.internal;

import java.lang.ref.*;
import java.text.*;
import java.util.*;

import javax.xml.bind.*;

/**
 * A utility class for parsing and formatting HTTP dates as used in cookies and
 * other headers.  This class handles dates as defined by RFC 2616 section
 * 3.3.1 as well as some other common non-standard formats.
 * <p>
 * This class was copied from HttpClient 4.3.
 */
public final class DateUtils {

	/**
	 * Date format pattern used to parse HTTP date headers in RFC 1123 format.
	 */
	public static final String PATTERN_RFC1123 = "EEE, dd MMM yyyy HH:mm:ss zzz";

	/**
	 * Date format pattern used to parse HTTP date headers in RFC 1036 format.
	 */
	public static final String PATTERN_RFC1036 = "EEE, dd-MMM-yy HH:mm:ss zzz";

	/**
	 * Date format pattern used to parse HTTP date headers in ANSI C <code>asctime()</code> format.
	 */
	public static final String PATTERN_ASCTIME = "EEE MMM d HH:mm:ss yyyy";
	private static final String[] DEFAULT_PATTERNS = new String[] { PATTERN_RFC1123, PATTERN_RFC1036, PATTERN_ASCTIME };
	private static final Date DEFAULT_TWO_DIGIT_YEAR_START;
	private static final TimeZone GMT = TimeZone.getTimeZone("GMT");
	static {
		final Calendar calendar = Calendar.getInstance();
		calendar.setTimeZone(GMT);
		calendar.set(2000, Calendar.JANUARY, 1, 0, 0, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		DEFAULT_TWO_DIGIT_YEAR_START = calendar.getTime();
	}

	/**
	 * Parses a date value. The formats used for parsing the date value are retrieved from the default http params.
	 *
	 * @param dateValue the date value to parse
	 * @return the parsed date or null if input could not be parsed
	 */
	public static Date parseDate(final String dateValue) {
		return parseDate(dateValue, null, null);
	}

	/**
	 * Parses the date value using the given date formats.
	 *
	 * @param dateValue the date value to parse
	 * @param dateFormats the date formats to use
	 *
	 * @return the parsed date or null if input could not be parsed
	 */
	public static Date parseDate(final String dateValue, final String[] dateFormats) {
		return parseDate(dateValue, dateFormats, null);
	}

	/**
	 * Parses the date value using the given date formats.
	 *
	 * @param dateValue the date value to parse
	 * @param dateFormats the date formats to use
	 * @param startDate During parsing, two digit years will be placed in the range <code>startDate</code> to
	 * 	<code>startDate + 100 years</code>. This value may be <code>null</code>. When
	 * 	<code>null</code> is given as a parameter, year <code>2000</code> will be used.
	 *
	 * @return the parsed date or null if input could not be parsed
	 */
	public static Date parseDate(final String dateValue, final String[] dateFormats, final Date startDate) {
		final String[] localDateFormats = dateFormats != null ? dateFormats : DEFAULT_PATTERNS;
		final Date localStartDate = startDate != null ? startDate : DEFAULT_TWO_DIGIT_YEAR_START;
		String v = dateValue;
		// trim single quotes around date if present
		// see issue #5279
		if (v.length() > 1 && v.startsWith("'") && v.endsWith("'")) {
			v = v.substring(1, v.length() - 1);
		}
		for (final String dateFormat : localDateFormats) {
			final SimpleDateFormat dateParser = DateFormatHolder.formatFor(dateFormat);
			dateParser.set2DigitYearStart(localStartDate);
			final ParsePosition pos = new ParsePosition(0);
			final Date result = dateParser.parse(v, pos);
			if (pos.getIndex() != 0) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Parses an ISO8601 string and converts it to a {@link Calendar}.
	 * 
	 * @param s The string to parse.
	 * @return The parsed value, or <jk>null</jk> if the string was <jk>null</jk> or empty.
	 */
	public static Calendar parseISO8601Calendar(String s) {
		if (StringUtils.isEmpty(s))
			return null;
		return DatatypeConverter.parseDateTime(toValidISO8601DT(s));
	}

	/**
	 * Formats the given date according to the RFC 1123 pattern.
	 *
	 * @param date The date to format.
	 * @return An RFC 1123 formatted date string.
	 *
	 * @see #PATTERN_RFC1123
	 */
	public static String formatDate(final Date date) {
		return formatDate(date, PATTERN_RFC1123);
	}

	/**
	 * Formats the given date according to the specified pattern. The pattern must conform to that used by the
	 * {@link SimpleDateFormat simple date format} class.
	 *
	 * @param date The date to format.
	 * @param pattern The pattern to use for formatting the date.
	 * @return A formatted date string.
	 * @throws IllegalArgumentException If the given date pattern is invalid.
	 * @see SimpleDateFormat
	 */
	public static String formatDate(final Date date, final String pattern) {
		final SimpleDateFormat formatter = DateFormatHolder.formatFor(pattern);
		return formatter.format(date);
	}

	/**
	 * Clears thread-local variable containing {@link java.text.DateFormat} cache.
	 */
	public static void clearThreadLocal() {
		DateFormatHolder.clearThreadLocal();
	}

	/**
	 * A factory for {@link SimpleDateFormat}s. The instances are stored in a threadlocal way because SimpleDateFormat
	 * is not threadsafe as noted in {@link SimpleDateFormat its javadoc}.
	 *
	 */
	final static class DateFormatHolder {
		private static final ThreadLocal<SoftReference<Map<String,SimpleDateFormat>>> THREADLOCAL_FORMATS = new ThreadLocal<SoftReference<Map<String,SimpleDateFormat>>>() {
			@Override
			protected SoftReference<Map<String,SimpleDateFormat>> initialValue() {
				return new SoftReference<Map<String,SimpleDateFormat>>(new HashMap<String,SimpleDateFormat>());
			}
		};

		/**
		 * creates a {@link SimpleDateFormat} for the requested format string.
		 *
		 * @param pattern a non-<code>null</code> format String according to {@link SimpleDateFormat}. The format is not
		 * 	checked against <code>null</code> since all paths go through {@link DateUtils}.
		 * @return the requested format. This simple dateformat should not be used to
		 * 	{@link SimpleDateFormat#applyPattern(String) apply} to a different pattern.
		 */
		public static SimpleDateFormat formatFor(final String pattern) {
			final SoftReference<Map<String,SimpleDateFormat>> ref = THREADLOCAL_FORMATS.get();
			Map<String,SimpleDateFormat> formats = ref.get();
			if (formats == null) {
				formats = new HashMap<String,SimpleDateFormat>();
				THREADLOCAL_FORMATS.set(new SoftReference<Map<String,SimpleDateFormat>>(formats));
			}
			SimpleDateFormat format = formats.get(pattern);
			if (format == null) {
				format = new SimpleDateFormat(pattern, Locale.US);
				format.setTimeZone(TimeZone.getTimeZone("GMT"));
				formats.put(pattern, format);
			}
			return format;
		}

		public static void clearThreadLocal() {
			THREADLOCAL_FORMATS.remove();
		}
	}

	/**
	 * Pads out an ISO8601 string so that it can be parsed using {@link DatatypeConverter#parseDateTime(String)}.
	 * <ul>
	 * 	<li><js>"2001-07-04T15:30:45-05:00"</js> --&gt; <js>"2001-07-04T15:30:45-05:00"</js>
	 * 	<li><js>"2001-07-04T15:30:45Z"</js> --&gt; <js>"2001-07-04T15:30:45Z"</js>
	 * 	<li><js>"2001-07-04T15:30:45.1Z"</js> --&gt; <js>"2001-07-04T15:30:45.1Z"</js>
	 * 	<li><js>"2001-07-04T15:30Z"</js> --&gt; <js>"2001-07-04T15:30:00Z"</js>
	 * 	<li><js>"2001-07-04T15:30"</js> --&gt; <js>"2001-07-04T15:30:00"</js>
	 * 	<li><js>"2001-07-04"</js> --&gt; <li><js>"2001-07-04T00:00:00"</js>
	 * 	<li><js>"2001-07"</js> --&gt; <js>"2001-07-01T00:00:00"</js>
	 * 	<li><js>"2001"</js> --&gt; <js>"2001-01-01T00:00:00"</js>
	 * </ul>
	 *
	 * @param in The string to pad.
	 * @return The padded string.
	 */
	public static final String toValidISO8601DT(String in) {

		// "2001-07-04T15:30:45Z"
		final int
			S1 = 1, // Looking for -
			S2 = 2, // Found -, looking for -
			S3 = 3, // Found -, looking for T
			S4 = 4, // Found T, looking for :
			S5 = 5, // Found :, looking for :
			S6 = 6; // Found :

		int state = 1;
		for (int i = 0; i < in.length(); i++) {
			char c = in.charAt(i);
			if (state == S1) {
				if (c == '-')
					state = S2;
			} else if (state == S2) {
				if (c == '-')
					state = S3;
			} else if (state == S3) {
				if (c == 'T')
					state = S4;
			} else if (state == S4) {
				if (c == ':')
					state = S5;
			} else if (state == S5) {
				if (c == ':')
					state = S6;
			}
		}

		switch(state) {
			case S1: return in + "-01-01T00:00:00";
			case S2: return in + "-01T00:00:00";
			case S3: return in + "T00:00:00";
			case S4: return in + ":00:00";
			case S5: return in + ":00";
			default: return in;
		}
	}
}