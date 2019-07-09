package com.bakdata.conquery.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;

import org.junit.jupiter.api.Test;

class PackedIntTest {

	
	@Test
	void testPackingUnpacking() {
		Random r = new Random(7);
		for(int i=0;i<10000;i++) {
			int a = r.nextInt();
			int b = r.nextInt();
			
			long packed = PackedInt.pack(a, b);
			assertThat(PackedInt.getLeft(packed)).isEqualTo(a);
			assertThat(PackedInt.getRight(packed)).isEqualTo(b);
		}
	}
}
