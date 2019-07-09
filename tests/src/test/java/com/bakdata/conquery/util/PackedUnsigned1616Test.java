package com.bakdata.conquery.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;

import org.junit.jupiter.api.Test;

class PackedUnsigned1616Test {

	
	@Test
	void testPackingUnpacking() {
		Random r = new Random(7);
		for(int i=0;i<10000;i++) {
			int a = r.nextInt(PackedUnsigned1616.MAX_VALUE+1);
			int b = r.nextInt(PackedUnsigned1616.MAX_VALUE+1);
			
			int packed = PackedUnsigned1616.pack(a, b);
			assertThat(PackedUnsigned1616.getLeft(packed)).isEqualTo(a);
			assertThat(PackedUnsigned1616.getRight(packed)).isEqualTo(b);
		}
	}
}
