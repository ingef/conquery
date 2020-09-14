package com.bakdata.conquery.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class PackedUnsigned1616 {

    private static final int RIGHT = 0xFFFF;
    public static final int MAX_VALUE = 65535;

    public int pack(int left, int right) {
    	check(left);
    	check(right);
        return (left << 16) | (right & RIGHT);
    }

    private void check(int v) {
		if(v < 0 || v > MAX_VALUE) {
			throw new IllegalArgumentException(String.format("Packed unsigned16 must be in [0;%d], but %d is not.", MAX_VALUE, v));
		}
	}

	public int getLeft(int packed) {
        return packed >>> 16;
    }

    public int getRight(int packed) {
        return packed & RIGHT;
    }
}