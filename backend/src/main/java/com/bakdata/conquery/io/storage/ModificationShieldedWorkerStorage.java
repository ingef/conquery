package com.bakdata.conquery.io.storage;

import java.util.Collection;

import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.specific.BucketId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * Provides a view on the storage that does not allow modification of the storage (update, delete).
 */
@RequiredArgsConstructor
@ToString(of = "delegate")
public class ModificationShieldedWorkerStorage implements NsIdResolver {

	private final WorkerStorage delegate;


	public Import getImport(ImportId id) {
		return delegate.getImport(id);
	}

	public Collection<Import> getAllImports() {
		return delegate.getAllImports();
	}



	public Dataset getDataset() {
		return delegate.getDataset();
	}


	public Collection<? extends Concept<?>> getAllConcepts() {
		return delegate.getAllConcepts();
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

	public Concept<?> getConcept(ConceptId conceptId) {
		return delegate.getConcept(conceptId);
	}

	@Override
	public <ID extends Id<VALUE> & NamespacedId, VALUE extends Identifiable<?>> VALUE get(ID id) {
		return delegate.get(id);
	}

	@Override
	public MutableInjectableValues inject(MutableInjectableValues values) {
		return values.add(NsIdResolver.class, this);
	}
}
