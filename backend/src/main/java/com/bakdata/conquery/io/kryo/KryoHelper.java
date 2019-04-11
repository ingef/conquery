package com.bakdata.conquery.io.kryo;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class KryoHelper {
	
	public static BigDecimal readBigDecimal(Input input) {
		int bytes = input.readInt(true);
		if(bytes == 0) {
			return null;
		}
		return new BigDecimal(new BigInteger(input.readBytes(bytes)), input.readInt(true));
	}
	
	public static void writeBigDecimal(Output output, BigDecimal bigDecimal) {
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
