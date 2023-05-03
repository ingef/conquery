package com.bakdata.conquery.resources.admin.rest;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

import javax.validation.Validator;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.PreviewConfig;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.StructureNode;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.mapping.EntityIdMap;
import com.bakdata.conquery.models.index.InternToExternMapper;
import com.bakdata.conquery.models.index.search.SearchIndex;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.Namespace;
import lombok.NonNull;
import lombok.SneakyThrows;

public interface AdminDatasetProcessor<N extends Namespace> {

	int MAX_IMPORTS_TEXT_LENGTH = 100;

	/**
	 * Creates and initializes a new dataset if it does not already exist.
	 */
	@SneakyThrows(IOException.class)
	Dataset addDataset(Dataset dataset);

	/**
	 * Delete dataset if it is empty.
	 */
	void deleteDataset(Dataset dataset);

	/**
	 * Add SecondaryId if it doesn't already exist.
	 */
	void addSecondaryId(N namespace, SecondaryIdDescription secondaryId);

	/**
	 * Delete SecondaryId if it does not have any dependents.
	 */
	void deleteSecondaryId(@NonNull SecondaryIdDescription secondaryId);

	/**
	 * Add table to Dataset if it doesn't already exist.
	 */
	@SneakyThrows
	void addTable(@NonNull Table table, N namespace);

	/**
	 * update a concept of the given dataset. Therefore, the concept will be deleted first then added
	 */
	void updateConcept(@NonNull Dataset dataset, @NonNull Concept<?> concept);

	/**
	 * Add the concept to the dataset if it does not exist yet
	 */
	void addConcept(@NonNull Dataset dataset, @NonNull Concept<?> concept, boolean force);

	void setPreviewConfig(PreviewConfig previewConfig, N namespace);

	/**
	 * Uploads new IdMapping.
	 */
	void setIdMapping(InputStream data, N namespace);

	/**
	 * Uploads new Structure for namespace.
	 */
	void setStructure(N namespace, StructureNode[] structure);

	/**
	 * Reads an Import partially Importing it if not yet present, then submitting it for full import.
	 */
	@SneakyThrows
	void addImport(N namespace, InputStream inputStream) throws IOException;

	/**
	 * Reads an Import partially Importing it if it is present, then submitting it for full import [Update of an import].
	 */
	@SneakyThrows
	void updateImport(N namespace, InputStream inputStream) throws IOException;

	/**
	 * Deletes an import.
	 */
	void deleteImport(Import imp);

	default void clearDependentConcepts(Collection<Concept<?>> allConcepts, Table table) {
		for (Concept<?> c : allConcepts) {
			for (Connector con : c.getConnectors()) {
				if (!con.getTable().equals(table)) {
					continue;
				}

				con.getConcept().clearMatchingStats();
			}
		}
	}

	/**
	 * Deletes a table if it has no dependents or not forced to do so.
	 */
	List<ConceptId> deleteTable(Table table, boolean force);

	/**
	 * Deletes a concept.
	 */
	void deleteConcept(Concept<?> concept);

	/**
	 * Issues all Shards to do an UpdateMatchingStats.
	 *
	 * @implNote This intentionally submits a SlowJob so that it will be queued after all jobs that are already in the queue (usually
	 * 	import jobs).
	 */
	void updateMatchingStats(Dataset dataset);

	EntityIdMap getIdMapping(N namespace);

	void addInternToExternMapping(N namespace, InternToExternMapper internToExternMapper);

	List<ConceptId> deleteInternToExternMapping(InternToExternMapper internToExternMapper, boolean force);

	void clearIndexCache(N namespace);

	void addSearchIndex(N namespace, SearchIndex searchIndex);

	List<ConceptId> deleteSearchIndex(SearchIndex searchIndex, boolean force);

	void deletePreviewConfig(N namespace);

	ConqueryConfig getConfig();

	Validator getValidator();

	DatasetRegistry getDatasetRegistry();

	JobManager getJobManager();
}
