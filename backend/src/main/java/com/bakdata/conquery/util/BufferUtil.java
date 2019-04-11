package com.bakdata.conquery.util;

import java.nio.charset.StandardCharsets;

import org.apache.mina.core.buffer.IoBuffer;

import lombok.experimental.UtilityClass;

@UtilityClass
public class BufferUtil {
	public static String toUtf8String(IoBuffer buffer) {
		return new String(buffer.array(), buffer.arrayOffset()+ buffer.position(), buffer.limit()-buffer.position(), StandardCharsets.UTF_8);
	}
}
