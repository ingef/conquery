package com.bakdata.conquery.io.xodus;

import java.util.Collection;
import java.util.List;

import javax.validation.Validator;

import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.dictionary.EncodedDictionary;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.ids.specific.BucketId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.worker.WorkerInformation;
import lombok.RequiredArgsConstructor;

/**
 * Provides a view on the storage that does not allow modification of the storage (update, delete).
 */
@RequiredArgsConstructor
public class ModificationShieldedWorkerStorage {

	private final WorkerStorage delegate;

	public Validator getValidator() {
		return delegate.getValidator();
	}

	public CentralRegistry getCentralRegistry() {
		return delegate.getCentralRegistry();
	}

	public String getStorageOrigin() {
		return delegate.getStorageOrigin();
	}


	public EncodedDictionary getPrimaryDictionary() {
		return delegate.getPrimaryDictionary();
	}

	public Import getImport(ImportId id) {
		return delegate.getImport(id);
	}

	public Collection<Import> getAllImports() {
		return delegate.getAllImports();
	}



	public Dataset getDataset() {
		return delegate.getDataset();
	}


	public List<Table> getTables() {
		return delegate.getTables();
	}

	public Concept<?> getConcept(ConceptId id) {
		return delegate.getConcept(id);
	}

	public boolean hasConcept(ConceptId id) {
		return delegate.hasConcept(id);
	}


	public Collection<? extends Concept<?>> getAllConcepts() {
		return delegate.getAllConcepts();
	}

	public WorkerInformation getWorker() {
		return delegate.getWorker();
	}


	public Bucket getBucket(BucketId id) {
		return delegate.getBucket(id);
	}


	public Collection<Bucket> getAllBuckets() {
		return delegate.getAllBuckets();
	}


	public Collection<CBlock> getAllCBlocks() {
		return delegate.getAllCBlocks();
	}

	public Table getTable(TableId tableId){
		return delegate.getTable(tableId);
	}

	public List<SecondaryIdDescription> getSecondaryIds() {
		return delegate.getSecondaryIds();
	}

	public SecondaryIdDescription getSecondaryId(SecondaryIdDescriptionId descriptionId) {
		return delegate.getSecondaryId(descriptionId);
	}
}
