package com.bakdata.conquery.models.preproc;

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

	public Object getObject(int index) {
		return switch (representations[index]) {
			case NULL, EMPTY -> null;
			case OBJECT -> objects[index];
			case LONG -> longs[index];
			case DOUBLE -> doubles[index];
			case BOOLEAN -> booleans[index];
			default -> throw new IllegalStateException("Unknown output representation");
		};
	}

	public long getLong(int index) {
		if (representations[index] == LONG) {
			return longs[index];
		}

		return ((Number) objects[index]).longValue();
	}

	public double getDouble(int index) {
		if (representations[index] == DOUBLE) {
			return doubles[index];
		}

		return ((Number) objects[index]).doubleValue();
	}

	public boolean getBoolean(int index) {
		if (representations[index] == BOOLEAN) {
			return booleans[index];
		}

		return (Boolean) objects[index];
	}

	public void clear() {
		Arrays.fill(representations, EMPTY);
		Arrays.fill(objects, null);
	}
}
