package com.bakdata.conquery.models.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeChild;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TreeConceptTest {

	private static final Dataset DATASET = new Dataset("dataset");

	private static ConceptTreeChild child_a;
	private static ConceptTreeChild child_a_a;
	private static ConceptTreeChild child_a_b;
	private static ConceptTreeChild child_b;
	private static ConceptTreeChild child_b_a;

	private static final TreeConcept CONCEPT = new TreeConcept() {{
		setDataset(DATASET);
		setName("concept");
		setChildren(List.of(
				child_a = new ConceptTreeChild() {{
					setDataset(DATASET);
					setName("a");
					setChildren(List.of(
							child_a_a = new ConceptTreeChild() {{
								setDataset(DATASET);
								setName("a");
								setChildren(List.of());
							}},
							child_a_b = new ConceptTreeChild() {{
								setDataset(DATASET);
								setName("b");
								setChildren(List.of());
							}}
					));
				}},
				child_b = new ConceptTreeChild() {{
					setDataset(DATASET);
					setName("b");
					setChildren(List.of(
							child_b_a = new ConceptTreeChild() {{
								setDataset(DATASET);
								setName("a");
								setChildren(List.of());
							}}
					));
				}}

		));
	}};

	@BeforeAll
	public static void setParents() {
		child_a.setParent(CONCEPT);
		child_a_a.setParent(child_a);
		child_a_b.setParent(child_a);

		child_b.setParent(CONCEPT);
		child_b_a.setParent(child_b);
	}


	@Test
	void findByIdConcept() {
		assertThat(CONCEPT.findChildById(ConceptElementId.Parser.INSTANCE.parse("dataset.concept"))).containsSame(CONCEPT);
	}

	@Test
	void findByIdTopLevelChild() {
		assertThat(CONCEPT.findChildById(ConceptElementId.Parser.INSTANCE.parse("dataset.concept.a"))).containsSame(child_a);
		assertThat(CONCEPT.findChildById(ConceptElementId.Parser.INSTANCE.parse("dataset.concept.b"))).containsSame(child_b);
	}

	@Test
	void findByIdNestedLevelChild() {
		assertThat(CONCEPT.findChildById(ConceptElementId.Parser.INSTANCE.parse("dataset.concept.a.a"))).containsSame(child_a_a);
		assertThat(CONCEPT.findChildById(ConceptElementId.Parser.INSTANCE.parse("dataset.concept.a.b"))).containsSame(child_a_b);
		assertThat(CONCEPT.findChildById(ConceptElementId.Parser.INSTANCE.parse("dataset.concept.b.a"))).containsSame(child_b_a);
	}

	@Test
	void findByIdUnknownConcept() {
		assertThat(CONCEPT.findChildById(ConceptElementId.Parser.INSTANCE.parse("dataset.koncept"))).isEmpty();
	}

	@Test
	void findByIdUnknownChild() {
		assertThat(CONCEPT.findChildById(ConceptElementId.Parser.INSTANCE.parse("dataset.koncept.a"))).isEmpty();
		assertThat(CONCEPT.findChildById(ConceptElementId.Parser.INSTANCE.parse("dataset.concept.b.b"))).isEmpty();
	}
}
