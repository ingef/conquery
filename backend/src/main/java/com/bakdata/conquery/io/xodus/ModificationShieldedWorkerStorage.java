package com.bakdata.conquery.io.xodus;

import java.util.Collection;

import javax.validation.Validator;

import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.dictionary.DirectDictionary;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.ids.specific.BucketId;
import com.bakdata.conquery.models.identifiable.ids.specific.CBlockId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
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

	public Dictionary getDictionary(DictionaryId id) {
		return delegate.getDictionary(id);
	}

	public DirectDictionary getPrimaryDictionary() {
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

	public CBlock getCBlock(CBlockId id) {
		return delegate.getCBlock(id);
	}

	public Collection<CBlock> getAllCBlocks() {
		return delegate.getAllCBlocks();
	}
}
