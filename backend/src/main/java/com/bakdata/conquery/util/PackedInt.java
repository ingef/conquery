package com.bakdata.conquery.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class PackedInt {

    private static final long RIGHT = 0xFFFFFFFFL;

    public long pack(int left, int right) {
        return (((long)left) << 32) | (right & RIGHT);
    }

	public int getLeft(long packed) {
        return (int)(packed >> 32);
    }

    public int getRight(long packed) {
        return (int)(packed & RIGHT);
    }
}