package com.bakdata.conquery.models.events.stores.primitive;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import com.bakdata.conquery.models.events.stores.root.IntegerStore;
import org.junit.jupiter.api.Test;

class IntArrayStoreTest {

	@Test
	public void integerStore() {
		List<Long> values = Arrays.asList(1L, 2L, -10L, (long) Integer.MIN_VALUE, null);

		final IntArrayStore store = IntArrayStore.create(values.size());

		for (int index = 0; index < values.size(); index++) {
			if(values.get(index) == null){
				store.setNull(index);
				continue;
			}
			store.setInteger(index, values.get(index));
		}

		for (int index = 0; index < values.size(); index++) {
			if (values.get(index) == null) {
				assertThat(store.has(index)).isFalse();
			}
			else {
				assertThat(store.getInteger(index)).isEqualTo(values.get(index));
			}
		}

		final IntegerStore selection = store.select(new int[]{0, 4}, new int[]{2, 1});

		assertThat(selection.getInteger(0)).isEqualTo(1);
		assertThat(selection.getInteger(1)).isEqualTo(2);

		assertThat(selection.has(2)).isFalse();
	}

	@Test
	public void byteStore() {
		List<Long> values = Arrays.asList(1L, 2L, -10L, Byte.MAX_VALUE - 1L, (long) Byte.MIN_VALUE, null);

		final ByteArrayStore store = ByteArrayStore.create(values.size());

		for (int index = 0; index < values.size(); index++) {
			if(values.get(index) == null){
				store.setNull(index);
				continue;
			}

			store.setInteger(index, values.get(index));
		}

		for (int index = 0; index < values.size(); index++) {
			if (values.get(index) == null) {
				assertThat(store.has(index)).isFalse();
			}
			else {
				assertThat(store.getInteger(index)).isEqualTo(values.get(index));
			}
		}

		final IntegerStore selection = store.select(new int[]{0, 5}, new int[]{2, 1});

		assertThat(selection.getInteger(0)).isEqualTo(1);
		assertThat(selection.getInteger(1)).isEqualTo(2);

		assertThat(selection.has(2)).isFalse();

	}


	@Test
	public void shortStore() {
		List<Long> values = Arrays.asList(1L, 2L, -10L, Short.MAX_VALUE - 1L, (long) Short.MIN_VALUE, null);

		final ShortArrayStore store = ShortArrayStore.create(values.size());

		for (int index = 0; index < values.size(); index++) {
			if(values.get(index) == null){
				store.setNull(index);
				continue;
			}

			store.setInteger(index, values.get(index));
		}

		for (int index = 0; index < values.size(); index++) {
			if (values.get(index) == null) {
				assertThat(store.has(index)).isFalse();
			}
			else {
				assertThat(store.getInteger(index)).isEqualTo(values.get(index));
			}
		}

		final IntegerStore selection = store.select(new int[]{0, 5}, new int[]{2, 1});

		assertThat(selection.getInteger(0)).isEqualTo(1);
		assertThat(selection.getInteger(1)).isEqualTo(2);

		assertThat(selection.has(2)).isFalse();


	}

}