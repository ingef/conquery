package com.bakdata.conquery.util;

import java.util.Arrays;

import lombok.experimental.UtilityClass;
import org.apache.mina.core.buffer.IoBuffer;

@UtilityClass
public class BufferUtil {

	public static byte[] toBytes(IoBuffer buffer) {
		return Arrays.copyOfRange(buffer.array(), buffer.arrayOffset()+ buffer.position(), buffer.arrayOffset()+buffer.limit());
	}
}
