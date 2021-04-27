package com.bakdata.conquery.models.concepts.tree;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.bakdata.conquery.models.concepts.conditions.ColumnEqualCondition;
import com.bakdata.conquery.models.concepts.conditions.ConceptTreeCondition;
import com.bakdata.conquery.models.concepts.conditions.PrefixCondition;
import com.bakdata.conquery.models.concepts.conditions.PrefixRangeCondition;
import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.Range;
import org.junit.jupiter.api.Test;

class ConceptTreeChildTest {

	@Test
	public void columnSpan() {
		final ConceptTreeChild root = new ConceptTreeChild();
		root.setCondition(new PrefixRangeCondition("A00", "A10"));

		final ConceptTreeChild child1 = new ConceptTreeChild();
		child1.setCondition(new PrefixRangeCondition("A01", "A02.2"));

		final ConceptTreeChild child2 = new ConceptTreeChild();
		child2.setCondition(new ColumnEqualCondition(Set.of("A01"), "my_column"));

		root.setChildren(List.of(child1, child2));

		final Map<String, ImmutableRangeSet<Prefix>> should_span = Map.of(
				ConceptTreeCondition.COLUMN_PLACEHOLDER, ImmutableRangeSet.of(Range.closed(new Prefix("A00"), new Prefix("A10"))),
				"my_column", ImmutableRangeSet.of(Range.singleton(new Prefix("A01")))
		);

		assertThat(root.getColumnSpan()).isEqualTo(should_span);
	}

	@Test
	public void enclosesSubRange() {
		final ConceptTreeChild root = new ConceptTreeChild();
		root.setCondition(new PrefixRangeCondition("A00", "A10"));

		final ConceptTreeChild child1 = new ConceptTreeChild();
		child1.setCondition(new PrefixRangeCondition("A01", "A02.2"));

		root.setChildren(List.of(child1));

		assertThat(root.isEnclosingChildren()).isTrue();
	}

	@Test
	public void enclosesSelfRange() {
		final ConceptTreeChild root = new ConceptTreeChild();
		root.setCondition(new PrefixRangeCondition("A00", "A10"));

		final ConceptTreeChild child4 = new ConceptTreeChild();
		child4.setCondition(root.getCondition());

		root.setChildren(List.of(child4));
		assertThat(root.isEnclosingChildren()).isTrue();
	}

	@Test
	public void enclosesOnOtherDimension() {
		final ConceptTreeChild root = new ConceptTreeChild();
		root.setCondition(new PrefixRangeCondition("A00", "A10"));

		final ConceptTreeChild child3 = new ConceptTreeChild();
		child3.setCondition(new ColumnEqualCondition(Set.of("G"), "column"));

		root.setChildren(List.of(child3));
		assertThat(root.isEnclosingChildren()).isTrue();
	}

	@Test
	public void doesNotEncloseExcessSingleton() {
		final ConceptTreeChild root = new ConceptTreeChild();
		root.setCondition(new PrefixRangeCondition("A00", "A10"));

		final ConceptTreeChild child2 = new ConceptTreeChild();
		child2.setCondition(new PrefixCondition(new String[]{"A11"}));

		root.setChildren(List.of(child2));

		assertThat(root.isEnclosingChildren()).isFalse();
	}

	@Test
	public void overlaps() {
		final ConceptTreeChild root = new ConceptTreeChild();
		root.setCondition(new PrefixRangeCondition("A00", "A10"));

		final ConceptTreeChild child1 = new ConceptTreeChild();
		child1.setCondition(new PrefixRangeCondition("A01", "A04.2"));

		root.setChildren(List.of(child1));

		assertThat(root.isChildrenAreNonOverlapping()).isTrue();


		final ConceptTreeChild child2 = new ConceptTreeChild();
		child2.setCondition(new PrefixCondition(new String[]{"A11"}));

		root.setChildren(List.of(child1, child2));

		assertThat(root.isChildrenAreNonOverlapping()).isTrue();

		final ConceptTreeChild child3 = new ConceptTreeChild();
		child3.setCondition(new PrefixCondition(new String[]{"A03.5"}));
		root.setChildren(List.of(child1, child3));

		assertThat(root.isChildrenAreNonOverlapping()).isFalse();

		final ConceptTreeChild child4 = new ConceptTreeChild();
		child4.setCondition(new PrefixRangeCondition("A02", "A07"));
		root.setChildren(List.of(child1, child4));

		assertThat(root.isChildrenAreNonOverlapping()).isFalse();
	}

}