package com.bakdata.conquery.io.jackson;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.List;

import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.text.translate.JavaUnicodeEscaper;
import org.apache.mina.core.buffer.IoBuffer;

import com.bakdata.conquery.io.mina.ChunkedMessage;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JacksonUtil {

	public static String toJsonDebug(byte[] bytes) {
		return toJsonDebug(IoBuffer.wrap(bytes));
	}
	
	public static String toJsonDebug(Iterable<IoBuffer> list) {
		return toJsonDebug(stream(list));
	}
	
	public static InputStream stream(Iterable<IoBuffer> list) {
		return new SequenceInputStream(
			IteratorUtils.asEnumeration(
				IteratorUtils.transformedIterator(
						list.iterator(),
						JacksonUtil::stream
				)
			)
		);
	}

	public static String toJsonDebug(IoBuffer buffer) {
		return toJsonDebug(stream(buffer));
	}
	
	public static String toJsonDebug(ChunkedMessage msg) {
		return toJsonDebug(msg.createInputStream());
	}
	
	public static InputStream stream(IoBuffer buffer) {
		return new ByteArrayInputStream(
			buffer.array(),
			buffer.position()+buffer.arrayOffset(),
			buffer.remaining()
		);
	}

	public static String toJsonDebug(InputStream is) {
		StringBuilder sb = new StringBuilder();
		try (JsonParser parser = Jackson.BINARY_MAPPER.getFactory().createParser(is)) {

			for(int i=0;i<50;i++) {
				JsonToken t = parser.nextToken();
				if(t == null) {
					break;
				}
				if(t.asString() != null) {
					sb.append(t.asString());
				}
				else {
					switch (t) {
						case FIELD_NAME:
							sb.append('"').append(parser.getCurrentName()).append("\" : ");
							break;
						case VALUE_TRUE:
						case VALUE_FALSE:
							sb.append(parser.getBooleanValue()).append(',');
							break;
						case VALUE_NUMBER_INT:
							sb.append(parser.getIntValue()).append(',');
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
							String value = parser.getText();
							if(value.length() > 50) {
								value = value.substring(0,50)+"...";
							}
							value = JavaUnicodeEscaper
								.outsideOf(32, 0x7e)
								.translate(value);
							sb.append('"').append(value).append("\",");
							break;
						default:
							sb.append(t.toString());
							log.warn("I don't know how to handle "+t);
							break;
					}
				}
			}
			return sb.toString();
		}
		catch(Exception e) {
			log.warn("Failed to create the debug json", e);
			sb.append("DEBUG_JSON_ERROR");
			return sb.toString();
		}
	}
}
