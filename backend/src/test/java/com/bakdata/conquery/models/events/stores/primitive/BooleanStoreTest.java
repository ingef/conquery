package com.bakdata.conquery.models.events.stores.primitive;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.BitSet;

import com.bakdata.conquery.io.jackson.Jackson;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;

class BooleanStoreTest {

	@Test
	public void select() {
		final BitSet values = new BitSet(10);
		values.set(0, 9);
		values.set(7, false);

		final BitSetStore booleanStore = new BitSetStore(values, new BitSet(10), 10);

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

	@Test
	public void testSerialization() throws JsonProcessingException {
		final BitSet bitSet = new BitSet();

		bitSet.set(1, 3);
		bitSet.set(100);
		bitSet.set(128, false);

		final BitSetStore booleanStore = new BitSetStore(bitSet, new BitSet(128), 128);

		final BitSetStore booleanStore1 = Jackson.getMapper().readValue(Jackson.getMapper().writeValueAsString(booleanStore), BitSetStore.class);

		assertThat(booleanStore1.getValues().get(128)).isEqualTo(false);

		assertThat(booleanStore.getValues()).isEqualTo(booleanStore1.getValues());

	}


	@Test
	public void danglingFalse() {
		final BitSetStore store = BitSetStore.create(10);

		for (int event = 0; event < store.getLines(); event++) {
			store.setNull(event);
		}

		store.setBoolean(8, true);
		store.setBoolean(9, false);

		assertThat(store.getBoolean(8)).isTrue();
		assertThat(store.getBoolean(9)).isFalse();
	}

}