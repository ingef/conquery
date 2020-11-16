package com.bakdata.conquery.io.xodus;

import java.io.IOException;
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
import org.apache.commons.lang3.NotImplementedException;

/**
 * Provides a view on the storage that does not allow modification of the storage (update, delete).
 */
@RequiredArgsConstructor
public class ModificationShieldedWorkerStorage implements WorkerStorage {

	private final WorkerStorage delegate;

	public Validator getValidator() {
		return delegate.getValidator();
	}

	public CentralRegistry getCentralRegistry() {
		return delegate.getCentralRegistry();
	}

	@Override
	public void loadData() {
		throw new NotImplementedException();
	}

	@Override
	public void clear() {
		throw new NotImplementedException();
	}

	@Override
	public String getStorageOrigin() {
		return delegate.getStorageOrigin();
	}

	@Override
	public void addDictionary(Dictionary dict) {
		throw new NotImplementedException();
	}

	public Dictionary getDictionary(DictionaryId id) {
		return delegate.getDictionary(id);
	}

	@Override
	public void updateDictionary(Dictionary dict) {
		throw new NotImplementedException();
	}

	@Override
	public void removeDictionary(DictionaryId id) {
		throw new NotImplementedException();
	}

	@Override
	public Dictionary computeDictionary(DictionaryId id) {
		throw new NotImplementedException();
	}

	public DirectDictionary getPrimaryDictionary() {
		return delegate.getPrimaryDictionary();
	}

	@Override
	public void addImport(Import imp) {
		throw new NotImplementedException();
	}

	public Import getImport(ImportId id) {
		return delegate.getImport(id);
	}

	public Collection<Import> getAllImports() {
		return delegate.getAllImports();
	}

	@Override
	public void updateImport(Import imp) {
		throw new NotImplementedException();
	}

	@Override
	public void removeImport(ImportId id) {
		throw new NotImplementedException();
	}

	public Dataset getDataset() {
		return delegate.getDataset();
	}

	@Override
	public void updateDataset(Dataset dataset) {
		throw new NotImplementedException();
	}

	public Concept<?> getConcept(ConceptId id) {
		return delegate.getConcept(id);
	}

	public boolean hasConcept(ConceptId id) {
		return delegate.hasConcept(id);
	}

	@Override
	public void updateConcept(Concept<?> concept) {
		throw new NotImplementedException();
	}

	@Override
	public void removeConcept(ConceptId id) {
		throw new NotImplementedException();
	}

	public Collection<? extends Concept<?>> getAllConcepts() {
		return delegate.getAllConcepts();
	}

	public WorkerInformation getWorker() {
		return delegate.getWorker();
	}

	@Override
	public void setWorker(WorkerInformation worker) {
		throw new NotImplementedException();
	}

	@Override
	public void updateWorker(WorkerInformation worker) {
		throw new NotImplementedException();
	}

	@Override
	public void addBucket(Bucket bucket) {
		throw new NotImplementedException();
	}

	public Bucket getBucket(BucketId id) {
		return delegate.getBucket(id);
	}

	@Override
	public void removeBucket(BucketId id) {
		throw new NotImplementedException();
	}

	public Collection<Bucket> getAllBuckets() {
		return delegate.getAllBuckets();
	}

	@Override
	public void addCBlock(CBlock cBlock) {
		throw new NotImplementedException();
	}

	public CBlock getCBlock(CBlockId id) {
		return delegate.getCBlock(id);
	}

	@Override
	public void updateCBlock(CBlock cBlock) {
		throw new NotImplementedException();
	}

	@Override
	public void removeCBlock(CBlockId id) {
		throw new NotImplementedException();
	}

	public Collection<CBlock> getAllCBlocks() {
		return delegate.getAllCBlocks();
	}

	@Override
	public void close() throws IOException {
		throw new NotImplementedException();
	}
}
