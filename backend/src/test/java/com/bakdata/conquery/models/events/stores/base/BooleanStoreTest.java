package com.bakdata.conquery.models.events.stores.base;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.BitSet;

import org.junit.jupiter.api.Test;

class BooleanStoreTest {

	@Test
	public void select() {
		final BitSet values = new BitSet(10);
		values.set(0, 9);
		values.set(7, false);

		final BooleanStore booleanStore = new BooleanStore(values);

		assertThat(booleanStore.select(new int[]{0}, new int[]{10}).getValues())
				.isEqualTo(values);

		{
			final BitSet expected = new BitSet();
			expected.set(0);
			expected.set(1);
			expected.set(2, false);

			assertThat(booleanStore.select(new int[]{0, 6}, new int[]{1, 2}).getValues())
					.isEqualTo(expected);
		}

		{
			final BitSet expected = new BitSet();
			expected.set(0);

			assertThat(booleanStore.select(new int[]{0}, new int[]{1}).getValues())
					.isEqualTo(expected);
		}

	}

}