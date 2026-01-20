package com.bakdata.conquery.mode;

import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.DistributedNamespace;
import com.bakdata.conquery.models.worker.Namespace;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * Listener for updates of stored entities in ConQuery.
 */
@Data
public abstract class StorageListener<T extends Namespace>{

	private final JobManager jobManager;
	private final DatasetRegistry<T> datasetRegistry;

	public abstract void onAddSecondaryId(SecondaryIdDescription secondaryId);

	public abstract void onDeleteSecondaryId(SecondaryIdDescriptionId description);

	public abstract void onAddTable(Table table);

	public abstract void onRemoveTable(TableId table);

	public abstract void onAddConcept(Concept<?> concept);

	public abstract void onDeleteConcept(ConceptId concept);

}
