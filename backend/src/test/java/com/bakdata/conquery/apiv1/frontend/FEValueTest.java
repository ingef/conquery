package com.bakdata.conquery.apiv1.frontend;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;


class FEValueTest {
	@Test
	public void test() {
		final FEValue firstA = new FEValue("a", "Label A", "Label A");
		final FEValue secondA = new FEValue("a", "Label A2", "Label A");

		final FEValue similarA = new FEValue("b", "Label A", "Label A");

		final Set<FEValue> filter = new HashSet<>();

		filter.add(firstA);

		assertThat(filter.add(firstA)).isFalse();
		assertThat(filter.add(secondA)).isFalse();

		assertThat(filter.add(similarA)).isTrue();
	}
}