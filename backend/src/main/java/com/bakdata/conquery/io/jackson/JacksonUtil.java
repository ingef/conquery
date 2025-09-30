package com.bakdata.conquery.io.jackson;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.translate.JavaUnicodeEscaper;
import org.apache.mina.core.buffer.IoBuffer;

@Slf4j @UtilityClass
public class JacksonUtil {

	/**
	 *	Partially read and parse InputStream as Json, directly storing it into String, just for debugging purposes.
	 */
	public static String toJsonDebug(InputStream is) {
		is.mark(1024);
		StringBuilder sb = new StringBuilder();
		try (JsonParser parser = Jackson.BINARY_MAPPER.getFactory().createParser(is)) {

			for (int i = 0; i < 50; i++) {
				JsonToken t = parser.nextToken();
				if (t == null) {
					break;
				}
				if (t.asString() != null) {
					sb.append(t.asString());
					continue;
				}

				switch (t) {
					case FIELD_NAME:
						sb.append('"').append(parser.getCurrentName()).append("\" : ");
						break;
					case VALUE_TRUE:
					case VALUE_FALSE:
						sb.append(parser.getBooleanValue()).append(',');
						break;
					case VALUE_NUMBER_INT:
						sb.append(parser.getLongValue()).append(',');
						break;
					case VALUE_NUMBER_FLOAT:
						sb.append(parser.getDoubleValue()).append(',');
						break;
					case VALUE_NULL:
						sb.append("null,");
					case VALUE_EMBEDDED_OBJECT:
						sb.append('"').append(IoBuffer.wrap(parser.getBinaryValue()).getHexDump(100)).append("\",");
						break;
					case VALUE_STRING:
						String value = StringUtils.abbreviate(parser.getText(), 50);

						value = JavaUnicodeEscaper
										.outsideOf(32, 0x7e)
										.translate(value);
						sb.append('"').append(value).append("\",");
						break;
					default:
						sb.append(t);
						log.warn("I don't know how to handle {}", t);
						break;
				}
			}
			return sb.toString();
		}
		catch (Exception e) {
			log.warn("Failed to create the debug json", e);
			if (!sb.isEmpty()) {
				sb.append("DEBUG_JSON_ERROR");

			}
			else {
				try {
					is.reset();
					sb.append(logHex(is));
				}
				catch (Exception hexException) {
					log.error("Unable to generate hex dump of input stream", e);
					sb.append("HEX_DUMP_ERROR");
				}
			}
			return sb.toString();
		}
	}

	public static String logHex(InputStream inputStream) throws IOException {
		inputStream.mark(1024);  // Mark to allow resetting later
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();

		byte[] temp = new byte[16]; // Read chunks of 16 bytes
		int bytesRead;
		while ((bytesRead = inputStream.read(temp)) != -1) {
			buffer.write(temp, 0, bytesRead);
			if (buffer.size() >= 128) break; // Limit logging to 128 bytes
		}

		inputStream.reset();  // Reset stream for further processing

		byte[] data = buffer.toByteArray();
		StringBuilder hexDump = new StringBuilder();
		for (byte b : data) {
			hexDump.append(String.format("%02X ", b));
		}

		return "Hex Dump of InputStream (first 128 bytes): " + hexDump;
	}
}
