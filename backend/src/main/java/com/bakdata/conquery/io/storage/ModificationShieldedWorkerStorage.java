package com.bakdata.conquery.io.storage;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.identifiable.ids.specific.BucketId;
import com.bakdata.conquery.models.identifiable.ids.specific.CBlockId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.worker.WorkerInformation;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.experimental.Delegate;

/**
 * Provides a view on the storage that does not allow modification of the storage (update, delete).
 */
@RequiredArgsConstructor
@ToString(of = "delegate")
public class ModificationShieldedWorkerStorage implements WorkerStorage, Injectable {

	@Delegate
	private final WorkerStorage delegate;

	@Override
	public void addCBlock(CBlock cBlock) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeCBlock(CBlockId id) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addBucket(Bucket bucket) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeBucket(BucketId id) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setWorker(WorkerInformation worker) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateWorker(WorkerInformation worker) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void openStores(ObjectMapper objectMapper) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeStorage() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addImport(Import imp) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateImport(Import imp) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeImport(ImportId id) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateDataset(Dataset dataset) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addTable(Table table) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeTable(TableId table) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addSecondaryId(SecondaryIdDescription secondaryIdDescription) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeSecondaryId(SecondaryIdDescriptionId secondaryIdDescriptionId) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void updateConcept(Concept<?> concept) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeConcept(ConceptId id) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void close() {
		throw new UnsupportedOperationException();
	}

	@Override
	public MutableInjectableValues inject(MutableInjectableValues values) {
		return values.add(WorkerStorageImpl.class, this);
	}

}
