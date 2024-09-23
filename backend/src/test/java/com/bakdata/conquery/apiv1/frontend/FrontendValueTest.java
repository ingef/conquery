package com.bakdata.conquery.apiv1.frontend;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;


class FrontendValueTest {
	/**
	 * This taste ensures that changes to FEValue maintain, that equals/hashcode respect {@link FrontendValue#getValue()} only.
	 */
	@Test
	public void testDistinctByNameOnly() {
		final FrontendValue firstA = new FrontendValue("a", "Label A", "Label A");
		final FrontendValue secondA = new FrontendValue("a", "Label A2", "Label A");

		final FrontendValue similarA = new FrontendValue("b", "Label A", "Label A");

		final Set<FrontendValue> filter = new HashSet<>();

		filter.add(firstA);

		assertThat(filter.add(firstA)).isFalse();
		assertThat(filter.add(secondA)).isFalse();

		assertThat(filter.add(similarA)).isTrue();

		assertThat(Stream.of(firstA, secondA, similarA).distinct()).containsExactly(firstA, similarA);
	}
}