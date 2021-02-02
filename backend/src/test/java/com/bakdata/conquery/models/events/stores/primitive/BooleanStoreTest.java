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

		final BitSetStore booleanStore = new BitSetStore(values);

		assertThat(booleanStore.doSelect(new int[]{0}, new int[]{10}).getValues())
				.isEqualTo(values);

		{
			final BitSet expected = new BitSet();
			expected.set(0);
			expected.set(1);
			expected.set(2, false);

			assertThat(booleanStore.doSelect(new int[]{0, 6}, new int[]{1, 2}).getValues())
					.isEqualTo(expected);
		}

		{
			final BitSet expected = new BitSet();
			expected.set(0);

			assertThat(booleanStore.doSelect(new int[]{0}, new int[]{1}).getValues())
					.isEqualTo(expected);
		}

	}

	@Test
	public void testSerialization() throws JsonProcessingException {
		final BitSet bitSet = new BitSet();

		bitSet.set(1, 3);
		bitSet.set(100);
		bitSet.set(128, false);

		final BitSetStore booleanStore = new BitSetStore(bitSet);

		final BitSetStore booleanStore1 = Jackson.MAPPER.readValue(Jackson.MAPPER.writeValueAsString(booleanStore), BitSetStore.class);

		assertThat(booleanStore1.getValues().get(128)).isEqualTo(false);

		assertThat(booleanStore.getValues()).isEqualTo(booleanStore1.getValues());

	}

}