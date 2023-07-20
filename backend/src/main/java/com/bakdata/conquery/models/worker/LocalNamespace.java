package com.bakdata.conquery.models.worker;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.PreviewConfig;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.MappableSingleColumnSelect;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.index.IndexService;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.jobs.SimpleJob;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.models.query.FilterSearch;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@ToString(onlyExplicitlyIncluded = true)
@RequiredArgsConstructor
public class LocalNamespace extends IdResolveContext implements Namespace {

	private final ObjectMapper preprocessMapper;
	private final ObjectMapper communicationMapper;
	@ToString.Include
	private final NamespaceStorage storage;

	private final ExecutionManager executionManager;

	// TODO: 01.07.2020 FK: This is not used a lot, as NamespacedMessages are highly convoluted and hard to decouple as is.
	private final JobManager jobManager;

	private final FilterSearch filterSearch;

	private final IndexService indexService;

	// Jackson's injectables that are available when deserializing requests (see PathParamInjector) or items from the storage
	private final List<Injectable> injectables;

	public int getBucket(String entity, int bucketSize) {
		return storage.getEntityBucket(entity)
					  .orElseGet(() -> storage.assignEntityBucket(entity, bucketSize));
	}

	@Override
	public void close() {
		try {
			jobManager.close();
		}
		catch (Exception e) {
			log.error("Unable to close namespace jobmanager of {}", this, e);
		}

		try {
			log.info("Closing namespace storage of {}", getStorage().getDataset().getId());
			storage.close();
		}
		catch (IOException e) {
			log.error("Unable to close namespace storage of {}.", this, e);
		}
	}

	@Override
	public void remove() {
		try {
			jobManager.close();
		}
		catch (Exception e) {
			log.error("Unable to close namespace jobmanager of {}", this, e);
		}

		log.info("Removing namespace storage of {}", getStorage().getDataset().getId());
		storage.removeStorage();
	}

	@Override
	public CentralRegistry getCentralRegistry() {
		return getStorage().getCentralRegistry();
	}

	@Override
	public int getNumberOfEntities() {
		return storage.getNumberOfEntities();
	}

	@Override
	public void updateInternToExternMappings() {
		storage.getAllConcepts().stream()
			   .flatMap(c -> c.getConnectors().stream())
			   .flatMap(con -> con.getSelects().stream())
			   .filter(MappableSingleColumnSelect.class::isInstance)
			   .map(MappableSingleColumnSelect.class::cast)
			   .forEach((s) -> jobManager.addSlowJob(new SimpleJob("Update internToExtern Mappings [" + s.getId() + "]", s::loadMapping)));

		storage.getSecondaryIds().stream()
			   .filter(desc -> desc.getMapping() != null)
			   .forEach((s) -> jobManager.addSlowJob(new SimpleJob("Update internToExtern Mappings [" + s.getId() + "]", s.getMapping()::init)));
	}

	@Override
	public void clearIndexCache() {
		indexService.evictCache();
	}

	@Override
	public PreviewConfig getPreviewConfig() {
		return getStorage().getPreviewConfig();
	}

	@Override
	public CentralRegistry findRegistry(DatasetId dataset) throws NoSuchElementException {
		if (!this.getDataset().getId().equals(dataset)) {
			throw new NoSuchElementException("Wrong dataset: '" + dataset + "' (expected: '" + this.getDataset().getId() + "')");
		}
		return storage.getCentralRegistry();
	}

	@Override
	public Dataset getDataset() {
		return storage.getDataset();
	}

	@Override
	public CentralRegistry getMetaRegistry() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ExecutionManager getExecutionManager() {
		return executionManager;
	}
}
