package com.bakdata.conquery.io.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.concepts.MatchingStats;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeChild;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.util.extensions.NamespaceStorageExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class NamespaceStorageMatchingStatsTest {

	@RegisterExtension
	private static final NamespaceStorageExtension NAMESPACE_STORAGE_EXTENSION = new NamespaceStorageExtension();

	@Test
	void removesAllMatchingStatsForConcept() throws Exception {
		final NamespaceStorage storage = NAMESPACE_STORAGE_EXTENSION.getStorage();
		final Dataset dataset = new Dataset("dataset");
		dataset.setStorageProvider(storage);
		storage.updateDataset(dataset);

		final TreeConcept concept = new TreeConcept();
		concept.setName("concept");
		concept.setNamespacedStorageProvider(storage);
		final ConceptTreeChild child = new ConceptTreeChild();
		child.setName("child");
		child.setParent(concept);
		concept.setChildren(List.of(child));
		concept.init();
		storage.updateConcept(concept);

		final ConceptId otherConcept = new ConceptId(dataset.getId(), "other-concept");
		final MatchingStats conceptStats = MatchingStats.singleEntry("source", new MatchingStats.Entry(4, 3, 1, 2));
		final MatchingStats childStats = MatchingStats.singleEntry("source", new MatchingStats.Entry(2, 2, 1, 2));
		final MatchingStats otherStats = MatchingStats.singleEntry("source", new MatchingStats.Entry(1, 1, 1, 1));

		storage.updateMatchingStats(concept.getId(), conceptStats);
		storage.updateMatchingStats(child.getId(), childStats);
		storage.updateMatchingStats(otherConcept, otherStats);

		assertThat(storage.getMatchingStats(concept.getId())).isSameAs(conceptStats);
		assertThat(storage.getMatchingStats(child.getId())).isSameAs(childStats);

		storage.removeConcept(concept.getId());

		assertThat(storage.getMatchingStats(concept.getId())).isNull();
		assertThat(storage.getMatchingStats(child.getId())).isNull();
		assertThat(storage.getMatchingStats(otherConcept)).isSameAs(otherStats);
	}
}
