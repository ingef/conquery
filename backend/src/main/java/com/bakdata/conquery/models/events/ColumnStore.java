package com.bakdata.conquery.models.events;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.function.Function;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import it.unimi.dsi.fastutil.objects.Object2BooleanFunction;
import it.unimi.dsi.fastutil.objects.Object2ByteFunction;
import it.unimi.dsi.fastutil.objects.Object2DoubleFunction;
import it.unimi.dsi.fastutil.objects.Object2FloatFunction;
import it.unimi.dsi.fastutil.objects.Object2IntFunction;
import it.unimi.dsi.fastutil.objects.Object2LongFunction;
import it.unimi.dsi.fastutil.objects.Object2ShortFunction;

@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.PROPERTY, property = "value")
@CPSBase
public interface ColumnStore<T> {

	public static <ELT, OUT> OUT transform(Object[] values, Class<OUT> to, Function<Object, ELT> transformer) {
		final Object[] out = (Object[]) Array.newInstance(to, values.length);

		for (int i = 0; i < values.length; i++) {
			if (values[i] != null) {
				out[i] = transformer.apply(values[i]);
			}
		}
		return (OUT) out;
	}

	public static int[] transform2int(Object[] values, Object2IntFunction<Object> transformer) {
		final int[] out = new int[values.length];

		for (int i = 0; i < values.length; i++) {
			if (values[i] != null) {
				out[i] = (int) transformer.apply(values[i]);
			}
		}
		return out;
	}

	public static long[] transform2long(Object[] values, Object2LongFunction<Object> transformer) {
		final long[] out = new long[values.length];

		for (int i = 0; i < values.length; i++) {
			if (values[i] != null) {
				out[i] = (long) transformer.apply(values[i]);
			}
		}
		return out;
	}


	public static double[] transform2double(Object[] values, Object2DoubleFunction<Object> transformer) {
		final double[] out = new double[values.length];

		for (int i = 0; i < values.length; i++) {
			if (values[i] != null) {
				out[i] = transformer.apply(values[i]);
			}
		}
		return out;
	}

	public static float[] transform2float(Object[] values, Object2FloatFunction<Object> transformer) {
		final float[] out = new float[values.length];

		for (int i = 0; i < values.length; i++) {
			if (values[i] != null) {
				out[i] = transformer.apply(values[i]);
			}
		}

		return out;
	}

	public static short[] transform2short(Object[] values, Object2ShortFunction<Object> transformer) {
		short[] out = new short[values.length];

		for (int i = 0; i < values.length; i++) {
			if (values[i] != null) {
				out[i] = transformer.apply(values[i]);
			}
		}
		return out;
	}

	public static byte[] transform2byte(Object[] values, Object2ByteFunction<Object> transformer) {
		byte[] out = new byte[values.length];

		for (int i = 0; i < values.length; i++) {
			if (values[i] != null) {
				out[i] = transformer.apply(values[i]);
			}
		}
		return out;
	}



	public static boolean[] transform2bool(Object[] values, Object2BooleanFunction<Object> transformer) {
		boolean[] out = new boolean[values.length];

		for (int i = 0; i < values.length; i++) {
			if (values[i] != null) {
				out[i] = transformer.apply(values[i]);
			}
		}
		return out;
	}

	void set(int event, T value);

	boolean has(int event);

	T get(int event);

	int getString(int event);

	long getInteger(int event);

	boolean getBoolean(int event);

	double getReal(int event);

	BigDecimal getDecimal(int event);

	long getMoney(int event);

	int getDate(int event);

	CDateRange getDateRange(int event);

	Object getAsObject(int event);
}
