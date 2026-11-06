package com.bakdata.conquery.models.preproc;

import java.math.BigDecimal;
import java.util.Arrays;

/**
 * Reusable staging area for a parsed output row. Primitive representations avoid boxing values before a row is committed.
 */
public final class OutputRow {

	private static final byte EMPTY = 0;
	private static final byte NULL = 1;
	private static final byte OBJECT = 2;
	private static final byte LONG = 3;
	private static final byte DOUBLE = 4;
	private static final byte BOOLEAN = 5;
	private static final byte BIG_DECIMAL = 6;

	private final byte[] representations;
	private final Object[] objects;
	private final long[] longs;
	private final double[] doubles;
	private final boolean[] booleans;

	public OutputRow(int size) {
		representations = new byte[size];
		objects = new Object[size];
		longs = new long[size];
		doubles = new double[size];
		booleans = new boolean[size];
	}

	public int size() {
		return representations.length;
	}

	public boolean isNull(int index) {
		return representations[index] == NULL;
	}

	public void setNull(int index) {
		representations[index] = NULL;
		objects[index] = null;
	}

	public void setObject(int index, Object value) {
		if (value == null) {
			setNull(index);
			return;
		}

		representations[index] = OBJECT;
		objects[index] = value;
	}

	public void setLong(int index, long value) {
		representations[index] = LONG;
		longs[index] = value;
		objects[index] = null;
	}

	public void setDouble(int index, double value) {
		representations[index] = DOUBLE;
		doubles[index] = value;
		objects[index] = null;
	}

	public void setBoolean(int index, boolean value) {
		representations[index] = BOOLEAN;
		booleans[index] = value;
		objects[index] = null;
	}

	public void setBigDecimal(int index, BigDecimal value) {
		if (value == null) {
			setNull(index);
			return;
		}

		representations[index] = BIG_DECIMAL;
		objects[index] = value;
	}

	public Object getObject(int index) {
		return switch (representations[index]) {
			case NULL -> null;
			case OBJECT -> objects[index];
			default -> throw unexpectedRepresentation(index, OBJECT);
		};
	}

	public long getLong(int index) {
		if (representations[index] != LONG) {
			throw unexpectedRepresentation(index, LONG);
		}

		return longs[index];
	}

	public double getDouble(int index) {
		if (representations[index] != DOUBLE) {
			throw unexpectedRepresentation(index, DOUBLE);
		}

		return doubles[index];
	}

	public boolean getBoolean(int index) {
		if (representations[index] != BOOLEAN) {
			throw unexpectedRepresentation(index, BOOLEAN);
		}

		return booleans[index];
	}

	public BigDecimal getBigDecimal(int index) {
		if (representations[index] != BIG_DECIMAL) {
			throw unexpectedRepresentation(index, BIG_DECIMAL);
		}

		return (BigDecimal) objects[index];
	}

	public void clear() {
		Arrays.fill(representations, EMPTY);
		Arrays.fill(objects, null);
	}

	private IllegalStateException unexpectedRepresentation(int index, byte expected) {
		return new IllegalStateException("Expected %s at output index %d, but found %s"
										 .formatted(representationName(expected), index, representationName(representations[index])));
	}

	private static String representationName(byte representation) {
		return switch (representation) {
			case EMPTY -> "EMPTY";
			case NULL -> "NULL";
			case OBJECT -> "OBJECT";
			case LONG -> "LONG";
			case DOUBLE -> "DOUBLE";
			case BOOLEAN -> "BOOLEAN";
			case BIG_DECIMAL -> "BIG_DECIMAL";
			default -> "UNKNOWN(" + representation + ")";
		};
	}
}
