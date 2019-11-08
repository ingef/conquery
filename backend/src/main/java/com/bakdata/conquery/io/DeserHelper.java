package com.bakdata.conquery.io;

import com.bakdata.conquery.util.io.SmallOut;
import com.esotericsoftware.kryo.io.Input;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

public class DeserHelper {
	
	public static BigDecimal readBigDecimal(Input input) throws IOException {
		int bytes = input.readInt(true);
		if(bytes == 0) {
			return null;
		}
		return new BigDecimal(new BigInteger(input.readBytes(bytes)), input.readInt(true));
	}
	
	public static void writeBigDecimal(SmallOut output, BigDecimal bigDecimal) throws IOException {
		if(bigDecimal == null) {
			output.writeInt(0, true);
			return;
		}
			
		byte[] bytes = bigDecimal.unscaledValue().toByteArray();
		output.writeInt(bytes.length, true);
		output.writeBytes(bytes);
		output.writeInt(bigDecimal.scale(), true);
	}
}
