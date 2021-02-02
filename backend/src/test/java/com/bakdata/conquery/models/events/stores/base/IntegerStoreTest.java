package com.bakdata.conquery.models.events.stores.base;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;

import com.bakdata.conquery.models.events.stores.ColumnStore;
import org.junit.jupiter.api.Test;

class IntegerStoreTest {

	@Test
	public void integerStore() {
		List<Long> values = Arrays.asList(1L, 2L, -10L, (long) Integer.MIN_VALUE, null);

		final IntegerStore store = IntegerStore.create(values.size());

		for (int index = 0; index < values.size(); index++) {
			store.set(index, values.get(index));
		}

		for (int index = 0; index < values.size(); index++) {
			if (values.get(index) != null) {
				assertThat(store.get(index)).isEqualTo(values.get(index));
			}
			else {
				assertThat(store.has(index)).isFalse();
			}
		}

		final ColumnStore<Long> selection = store.doSelect(new int[]{0, 4}, new int[]{2, 1});

		assertThat(selection.get(0)).isEqualTo(1);
		assertThat(selection.get(1)).isEqualTo(2);

		assertThat(selection.has(2)).isFalse();

		assertThatThrownBy(() -> selection.getDecimal(0)).isNotNull();
	}

	@Test
	public void byteStore() {
		List<Long> values = Arrays.asList(1L, 2L, -10L, Byte.MAX_VALUE - 1L, (long) Byte.MIN_VALUE, null);

		final ByteStore store = ByteStore.create(values.size());

		for (int index = 0; index < values.size(); index++) {
			store.set(index, values.get(index));
		}

		for (int index = 0; index < values.size(); index++) {
			if (values.get(index) != null) {
				assertThat(store.get(index)).isEqualTo(values.get(index));
			}
			else {
				assertThat(store.has(index)).isFalse();
			}
		}

		final ColumnStore<Long> selection = store.doSelect(new int[]{0, 5}, new int[]{2, 1});

		assertThat(selection.get(0)).isEqualTo(1);
		assertThat(selection.get(1)).isEqualTo(2);

		assertThat(selection.has(2)).isFalse();

		assertThatThrownBy(() -> selection.getDecimal(0)).isNotNull();
	}


	@Test
	public void shortStore() {
		List<Long> values = Arrays.asList(1L, 2L, -10L, Short.MAX_VALUE - 1L, (long) Short.MIN_VALUE, null);

		final ShortStore store = ShortStore.create(values.size());

		for (int index = 0; index < values.size(); index++) {
			store.set(index, values.get(index));
		}

		for (int index = 0; index < values.size(); index++) {
			if (values.get(index) != null) {
				assertThat(store.get(index)).isEqualTo(values.get(index));
			}
			else {
				assertThat(store.has(index)).isFalse();
			}
		}

		final ColumnStore<Long> selection = store.doSelect(new int[]{0, 5}, new int[]{2, 1});

		assertThat(selection.get(0)).isEqualTo(1);
		assertThat(selection.get(1)).isEqualTo(2);

		assertThat(selection.has(2)).isFalse();

		assertThatThrownBy(() -> selection.getDecimal(0)).isNotNull();

	}

}