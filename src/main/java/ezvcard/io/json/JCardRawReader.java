package ezvcard.io.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import ezvcard.VCardDataType;
import ezvcard.io.json.namesilo.NamesiloProperties;
import ezvcard.io.json.namesilo.NamesiloProperty;
import ezvcard.io.json.namesilo.NamesiloValue;
import ezvcard.parameter.VCardParameters;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 Copyright (c) 2012-2020, Michael Angstadt
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met: 

 1. Redistributions of source code must retain the above copyright notice, this
 list of conditions and the following disclaimer. 
 2. Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation
 and/or other materials provided with the distribution. 

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * Parses an vCard JSON data stream (jCard).
 * @author Michael Angstadt
 * @see <a href="http://tools.ietf.org/html/rfc7095">RFC 7095</a>
 */
public class JCardRawReader implements Closeable {
	private final Reader reader;
	private JsonParser parser;
	private boolean eof = false;
	private JCardDataStreamListener listener;
	private boolean strict = false;

	/**
	 * @param reader the reader to wrap
	 */
	public JCardRawReader(Reader reader) {
		this.reader = reader;
	}

	/**
	 * @param parser the parser to read from
	 * @param strict true if the parser's current token is expected to be
	 * positioned at the start of a jCard, false if not. If this is true, and
	 * the parser is not positioned at the beginning of a jCard, a
	 * {@link JCardParseException} will be thrown. If this if false, the parser
	 * will consume input until it reaches the beginning of a jCard.
	 */
	public JCardRawReader(JsonParser parser, boolean strict) {
		reader = null;
		this.parser = parser;
		this.strict = strict;
	}

	/**
	 * Gets the current line number.
	 * @return the line number
	 */
	public int getLineNum() {
		return (parser == null) ? 0 : parser.getCurrentLocation().getLineNr();
	}

	/**
	 * Reads the next vCard from the jCard data stream.
	 * @param listener handles the vCard data as it is read off the wire
	 * @throws JCardParseException if the jCard syntax is incorrect (the JSON
	 * syntax may be valid, but it is not in the correct jCard format).
	 * @throws JsonParseException if the JSON syntax is incorrect
	 * @throws IOException if there is a problem reading from the input stream
	 */
	public void readNext(JCardDataStreamListener listener) throws IOException {
		if (parser == null) {
			JsonFactory factory = new JsonFactory();
			parser = factory.createParser(reader);
		} else if (parser.isClosed()) {
			return;
		}

		this.listener = listener;

		//find the next vCard object
		JsonToken prev = parser.getCurrentToken();
		JsonToken cur;
		while ((cur = parser.nextToken()) != null) {
			// the eurodns registrar names the value string "vcards" instead. so we switched to startsWith() to accommodate
			if (prev == JsonToken.START_ARRAY && cur == JsonToken.VALUE_STRING
					&& ("vcard".equals(parser.getValueAsString()) || "vcards".equals(parser.getValueAsString()))) {
				//found
				break;
			}

			if (strict) {
				//the parser was expecting the jCard to be there
				if (prev != JsonToken.START_ARRAY) {
					if (prev == JsonToken.START_OBJECT && "properties".equals(parser.getCurrentName())) {
						parseNamesilo();
						return;
					}
					throw new JCardParseException(JsonToken.START_ARRAY, prev);
				}
				if (cur == JsonToken.START_ARRAY) {
					// gets tricky here, as we need to accommodate all the crappy formatting provided by the various
					// registrars without advancing the parser token past the point of no return. thus, we only advance
					// as far as needed in order to determine who the registrar is and apply the proper parsing.
					JsonToken next = parser.nextToken();
					String propertyName = parser.getValueAsString();
					if ("vcard".equalsIgnoreCase(propertyName)) {
						if (parse101Domain()){
							return;
						}
						if (parseDreamscapeDomain()){
							return;
						}
						throw new JCardParseException("Invalid value for first token: expected \"vcard\" , was \"" + parser.getValueAsString() + "\"", JsonToken.VALUE_STRING, cur);
					}

					if (parseNamecheap())
						return;
					if (parseDirectnic())
						return;
				}
				if (cur == JsonToken.START_OBJECT) {
					parseOnomae();
					return;
				}
				if (cur != JsonToken.VALUE_STRING) {
					throw new JCardParseException(JsonToken.VALUE_STRING, cur);
				}

				throw new JCardParseException("Invalid value for first token: expected \"vcard\" , was \"" + parser.getValueAsString() + "\"", JsonToken.VALUE_STRING, cur);
			}

			prev = cur;
		}

		if (cur == null) {
			//EOF
			eof = true;
			return;
		}

		listener.beginVCard();
		try {
			parseProperties();
		} catch (JCardParseException jpe) {
			// ascio provides vcard properties that are not nested in an outer array. thus we provide custom parsing
			// for such properties.
			if (parser.getCurrentToken() == JsonToken.VALUE_STRING) {
				parsePropertiesUnarrayed(false);
				return;
			}
			throw jpe;
		}

		if (parser.nextToken() != JsonToken.END_ARRAY) {
			if (parser.getCurrentToken() == JsonToken.START_ARRAY) {
				JsonToken next = parser.nextToken();
				if (next == JsonToken.VALUE_STRING) {
					// 35.com sometimes includes vcard properties in sibling arrays, rather than all nested in a single
					// array. we just skip all properties in the sibling array.
					parser.skipChildren();
					return;
				} else if (next == JsonToken.START_ARRAY) {
					if (parser.nextToken() == JsonToken.VALUE_STRING) {
						// pandi doesn't nest the vard properties inside of a parent array, but rather as part of the
						// vcardArray array. except each property is double nested. ugh.
						parseProperty();
						checkNext(JsonToken.END_ARRAY);
						while (parser.nextToken() != JsonToken.END_ARRAY) {
							checkCurrent(JsonToken.START_ARRAY);
							checkNext(JsonToken.START_ARRAY);
							parser.nextToken();
							parseProperty();
							checkNext(JsonToken.END_ARRAY);
						}
						return;
					}
				}
			}
			throw new JCardParseException(JsonToken.END_ARRAY, parser.currentToken());
		}
	}

	private void parsePandiProperties() {
		boolean first = true;

	}

	/**
	 * Frickin namesilo doesn't adhere to the vcard spec at all. Rather they provide their own custom json. Which we
	 * attempt to parse here.
	 *
	 * "vcardArray": {
	 *   "properties": [
	 *     {
	 *       "name": "FN",
	 *       "value": {
	 *         "stringValue": "Domain Administrator",
	 *         "typeName": "text"
	 *       }
	 *     },
	 *     {
	 *       "name": "ADR",
	 *       "value": {
	 *         "components": [
	 *           {
	 *             "name": "pobox",
	 *             "value": {
	 *               "typeName": "text"
	 *             }
	 *           },
	 *           {
	 *             "name": "ext",
	 *             "value": {
	 *               "typeName": "text"
	 *             }
	 *           },
	 *           {
	 *             "name": "street",
	 *             "value": {
	 *               "values": [
	 *                 {
	 *                   "stringValue": "1928 E. Highland Ave. Ste F104",
	 *                   "typeName": "text"
	 *                 },
	 *                 {
	 *                   "stringValue": "PMB# 255",
	 *                   "typeName": "text"
	 *                 }
	 *               ],
	 *               "typeName": "text"
	 *             }
	 *           },
	 *           {
	 *             "name": "locality",
	 *             "value": {
	 *               "values": [
	 *                 {
	 *                   "stringValue": "Phoenix",
	 *                   "typeName": "text"
	 *                 }
	 *               ],
	 *               "typeName": "text"
	 *             }
	 *           },
	 *           {
	 *             "name": "region",
	 *             "value": {
	 *               "values": [
	 *                 {
	 *                   "stringValue": "AZ",
	 *                   "typeName": "text"
	 *                 }
	 *               ],
	 *               "typeName": "text"
	 *             }
	 *           },
	 *           {
	 *             "name": "code",
	 *             "value": {
	 *               "values": [
	 *                 {
	 *                   "stringValue": "85016",
	 *                   "typeName": "text"
	 *                 }
	 *               ],
	 *               "typeName": "text"
	 *             }
	 *           },
	 *           {
	 *             "name": "country",
	 *             "value": {
	 *               "values": [
	 *                 {
	 *                   "stringValue": "US",
	 *                   "typeName": "text"
	 *                 }
	 *               ],
	 *               "typeName": "text"
	 *             }
	 *           }
	 *         ],
	 *         "typeName": "text"
	 *       }
	 *     },
	 *     {
	 *       "name": "ORG",
	 *       "value": {
	 *         "components": [
	 *           {
	 *             "name": "name",
	 *             "value": {
	 *               "stringValue": "See PrivacyGuardian.org",
	 *               "typeName": "text"
	 *             }
	 *           }
	 *         ],
	 *         "typeName": "text"
	 *       }
	 *     },
	 *     {
	 *       "name": "TEL",
	 *       "parameters": {},
	 *       "value": {
	 *         "stringValue": "tel:+0.3478717726",
	 *         "typeName": "uri"
	 *       }
	 *     },
	 *     {
	 *       "name": "EMAIL",
	 *       "value": {
	 *         "stringValue": "pw-a3b2c5eac8da5223be1aea812b2b1e3b@privacyguardian.org",
	 *         "typeName": "text"
	 *       }
	 *     }
	 *   ]
	 * }
	 * @throws IOException upon parse failure
	 */
	private void parseNamesilo() throws IOException {
		listener.beginVCard();
		NamesiloProperties properties = parser.readValueAs(NamesiloProperties.class);

		for (NamesiloProperty property : properties.getProperties()) {
			String propertyName = property.getName();
			if (propertyName == null || "".equalsIgnoreCase(propertyName))
				continue;

			NamesiloValue value = property.getValue();
			if (value == null)
				continue;

			String dataTypeStr = "unknown";
			if (value.getTypeName() != null)
				dataTypeStr = value.getTypeName().toLowerCase();
			VCardDataType dataType = "unknown".equals(dataTypeStr) ? null : VCardDataType.get(dataTypeStr);

			VCardParameters parameters = new VCardParameters();
			String group = null;

			List<JsonValue> values = new ArrayList<>();
			if (value.getStringValue() != null) {
				values.add(new JsonValue(value.getStringValue()));
			} else if (value.getComponents() != null && !value.getComponents().isEmpty()) {
				List<JsonValue> subvals = new ArrayList<>();
				for (NamesiloProperty component : value.getComponents()) {
					Object v = component.getValue().getValue();
					if (v instanceof List)
						subvals.add(new JsonValue(((List) v)));
					else
						subvals.add(new JsonValue(v));
				}
				values.add(new JsonValue(subvals));
			}
			JCardValue jCardValue = new JCardValue(values);

			listener.readProperty(group, propertyName.toLowerCase(), parameters, dataType, jCardValue);
		}
	}

	/**
	 * namecheap sometimes provides invalid vcard. it includes no "vcard" value string, nor does it nest the properties
	 * within an array. rather, it provides json as shown below. so we special case their format in order to allow
	 * parsing to succeed.
	 *
	 * "vcardArray": [
	 *   [
	 *     "version",
	 *     {},
	 *     "text",
	 *     "4.0"
	 *   ],
	 *   [
	 *     "fn",
	 *     {},
	 *     "text",
	 *     "REACTIVATION PERIOD"
	 *   ]
	 * ]
	 *
	 * @return true if parse was successful
	 * @throws IOException upon parse failure
	 */
	private boolean parseNamecheap() throws IOException {
		try {
			listener.beginVCard();
			parsePropertiesUnarrayed(false);
			return true;
		} catch (JCardParseException pe) {
			return false;
		}
	}

	private boolean parse101Domain() throws IOException {
		try {
			listener.beginVCard();
			parser.nextToken();
			parsePropertiesUnarrayed(false	);
			return true;
		} catch (JCardParseException pe) {
		return false;

		}
	}

	private boolean parseDreamscapeDomain() throws IOException{
		try{
			listener.beginVCard();
			do{
				parser.nextToken();
			} while (parser.nextToken() != JsonToken.END_OBJECT);
			parsePropertiesUnarrayed(true);
			return true;
		} catch (JCardParseException pe) {
			return false;
		}
	}


	/**
	 * directnic omits the "vcard" value string
	 * @return true if parse was successful
	 * @throws IOException
	 */
	private boolean parseDirectnic() throws IOException {
		do {
			parser.nextToken();
			parseProperty();
		} while (parser.nextToken() != JsonToken.END_ARRAY);
		return true;
	}

	/**
	 * onomae provides invalid vcard. they wrap the vcard in an Object, and then nest the properties inside two arrays
	 * instead of just a single one
	 *
	 * "vcardArray": [
	 *   {
	 *     "vcard": [
	 *       [
	 *         [
	 *           "version",
	 *           {},
	 *           "text",
	 *           "4.0"
	 *         ],
	 *         [
	 *           "fn",
	 *           {},
	 *           "text",
	 *           "Whois Privacy Protection Service by onamae.com"
	 *         ],
	 *         [
	 *           "org",
	 *           {},
	 *           "text",
	 *           "Whois Privacy Protection Service by onamae.com"
	 *         ],
	 *         [
	 *           "adr",
	 *           {
	 *             "type": "work"
	 *           },
	 *           "text",
	 *           [
	 *             "Cerulean Tower 11F",
	 *             "26-1 Sakuragaoka-cho",
	 *             "Shibuya-ku",
	 *             "Tokyo",
	 *             "150-8512",
	 *             "JP"
	 *           ]
	 *         ],
	 *         [
	 *           "tel",
	 *           {
	 *             "type": "voice"
	 *           },
	 *           "text",
	 *           "81.35456256"
	 *         ],
	 *         [
	 *           "tel",
	 *           {
	 *             "type": "fax"
	 *           },
	 *           "text",
	 *           ""
	 *         ]
	 *       ]
	 *     ]
	 *   }
	 * ]
	 *
	 * @throws IOException upon parse failure
	 */
	private void parseOnomae() throws IOException {
		if (parser.getCurrentToken() == JsonToken.START_OBJECT && parser.nextToken() == JsonToken.FIELD_NAME && "vcard".equalsIgnoreCase(parser.getCurrentName())) {
			listener.beginVCard();

			checkNext(JsonToken.START_ARRAY);
			checkNext(JsonToken.START_ARRAY);

			//read properties
			while (parser.nextToken() != JsonToken.END_ARRAY) { //until we reach the end properties array
				checkCurrent(JsonToken.START_ARRAY);
				parser.nextToken();
				parseProperty();
			}
			check(JsonToken.END_ARRAY, parser.nextToken());
		}
		check(JsonToken.END_OBJECT, parser.nextToken());

	}

	/**
	 * Parses vcard properties that are not nested in an outer array.
	 *
	 * "vcardArray": [
	 *   "vcard",
	 *   [
	 *     "version",
	 *     {},
	 *     "text",
	 *     "4.0"
	 *   ],
	 *   [
	 *     "fn",
	 *     {},
	 *     "text",
	 *     "Ascio Technologies, Inc"
	 *   ],
	 * ]
	 * @param requiresInitialSkip true if the parser position is not already on the start array of the property values itself.
	 * @throws IOException upon parse error
	 */
	private void parsePropertiesUnarrayed(boolean requiresInitialSkip) throws IOException {
		int loop = 0;
		if (requiresInitialSkip)
			loop = 1;
		do {
			if (loop >= 1)
				parser.nextToken();
			parseProperty();
			loop++;
		} while (parser.nextToken() != JsonToken.END_ARRAY);
	}



	private void parseProperties() throws IOException {
		//start properties array
		checkNext(JsonToken.START_ARRAY);

		//read properties
		while (parser.nextToken() != JsonToken.END_ARRAY) { //until we reach the end properties array
			checkCurrent(JsonToken.START_ARRAY);
			parser.nextToken();
			parseProperty();
		}
	}

	private void parseProperty() throws IOException {
		//get property name
		if (parser.getCurrentToken() != JsonToken.VALUE_STRING) {
			if (parser.getCurrentToken() == JsonToken.END_ARRAY) {
				// an empty property array. we can safely ignore it.
				return;
			}
			throw new JCardParseException(JsonToken.VALUE_STRING, parser.getCurrentToken());
		}
		String propertyName = parser.getValueAsString().toLowerCase();

		//get parameters
		VCardParameters parameters = parseParameters();
		if (parameters != null) {
			// advance the parser to the data type string if we successfully parsed parameters.
			//get data type
			checkNext(JsonToken.VALUE_STRING);
		} else {
			// if parameters is null, then parameters were not present and the parser is already currently on the
			// data type string.
			parameters = new VCardParameters();
		}
		//get group
		List<String> removed = parameters.removeAll("group");
		String group = removed.isEmpty() ? null : removed.get(0);

		String dataTypeStr = parser.getText().toLowerCase();
		VCardDataType dataType = "unknown".equals(dataTypeStr) ? null : VCardDataType.get(dataTypeStr);

		//get property value(s)
		List<JsonValue> values = parseValues();

		JCardValue value = new JCardValue(values);
		listener.readProperty(group, propertyName, parameters, dataType, value);
	}

	private VCardParameters parseParameters() throws IOException {
		JsonToken next = parser.nextToken();
		if (next == JsonToken.START_ARRAY) {
			// name.com registrar sometimes provides parameters in an array instead of an object. every instance we've
			// seen of the array so far is empty, so we're just ignoring it for now
			while (parser.nextToken() != JsonToken.END_ARRAY) {
			}
			return new VCardParameters();
		} else if (next == JsonToken.VALUE_STRING) {
			// dinahosting provides contact-uri properties with no parameters field. we just ignore them. ie
			//  [
			//     "CONTACT-URI",
			//     "uri",
			//     "https://dinahosting.com/dominios/contacto-whois/dominio/ysana.info"
			// ]
			return null;
		}

		checkCurrent(JsonToken.START_OBJECT);

		VCardParameters parameters = new VCardParameters();
		while (parser.nextToken() != JsonToken.END_OBJECT) {
			String parameterName = parser.getText();

			if (parser.nextToken() == JsonToken.START_ARRAY) {
				//multi-valued parameter
				while (parser.nextToken() != JsonToken.END_ARRAY) {
					parameters.put(parameterName, parser.getText());
				}
			} else {
				parameters.put(parameterName, parser.getValueAsString());
			}
		}

		return parameters;
	}

	private List<JsonValue> parseValues() throws IOException {
		List<JsonValue> values = new ArrayList<JsonValue>();
		while (parser.nextToken() != JsonToken.END_ARRAY) { //until we reach the end of the property array
			JsonValue value = parseValue();
			values.add(value);
		}
		return values;
	}

	private Object parseValueElement() throws IOException {
		switch (parser.getCurrentToken()) {
		case VALUE_FALSE:
		case VALUE_TRUE:
			return parser.getBooleanValue();
		case VALUE_NUMBER_FLOAT:
			return parser.getDoubleValue();
		case VALUE_NUMBER_INT:
			return parser.getLongValue();
		case VALUE_NULL:
			return null;
		default:
			return parser.getText();
		}
	}

	private List<JsonValue> parseValueArray() throws IOException {
		List<JsonValue> array = new ArrayList<JsonValue>();

		while (parser.nextToken() != JsonToken.END_ARRAY) {
			JsonValue value = parseValue();
			array.add(value);
		}

		return array;
	}

	private Map<String, JsonValue> parseValueObject() throws IOException {
		Map<String, JsonValue> object = new HashMap<String, JsonValue>();

		while (parser.nextToken() != JsonToken.END_OBJECT) {
			checkCurrent(JsonToken.FIELD_NAME);

			String key = parser.getText();
			parser.nextToken();
			JsonValue value = parseValue();
			object.put(key, value);
		}

		return object;
	}

	private JsonValue parseValue() throws IOException {
		switch (parser.getCurrentToken()) {
		case START_ARRAY:
			return new JsonValue(parseValueArray());
		case START_OBJECT:
			return new JsonValue(parseValueObject());
		default:
			return new JsonValue(parseValueElement());
		}
	}

	private void checkNext(JsonToken expected) throws IOException {
		JsonToken actual = parser.nextToken();
		check(expected, actual);
	}

	private void checkCurrent(JsonToken expected) throws JCardParseException {
		JsonToken actual = parser.getCurrentToken();
		check(expected, actual);
	}

	private void check(JsonToken expected, JsonToken actual) throws JCardParseException {
		if (actual != expected) {
			throw new JCardParseException(expected, actual);
		}
	}

	/**
	 * Determines whether the end of the data stream has been reached.
	 * @return true if the end has been reached, false if not
	 */
	public boolean eof() {
		return eof;
	}

	/**
	 * Handles the vCard data as it is read off the data stream.
	 * @author Michael Angstadt
	 */
	public interface JCardDataStreamListener {
		/**
		 * Called when a vCard has been found in the stream.
		 */
		void beginVCard();

		/**
		 * Called when a property is read.
		 * @param group the group or null if there is not group
		 * @param propertyName the property name (e.g. "summary")
		 * @param parameters the parameters
		 * @param dataType the data type or null for "unknown"
		 * @param value the property value
		 */
		void readProperty(String group, String propertyName, VCardParameters parameters, VCardDataType dataType, JCardValue value);
	}

	/**
	 * Closes the underlying {@link Reader} object.
	 */
	public void close() throws IOException {
		if (parser != null) {
			parser.close();
		}
		if (reader != null) {
			reader.close();
		}
	}
}
