package com.bakdata.conquery.models.worker;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import com.bakdata.conquery.apiv1.query.concept.specific.external.EntityResolver;
import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.PreviewConfig;
import com.bakdata.conquery.models.datasets.concepts.Searchable;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.MappableSingleColumnSelect;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.index.IndexService;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.jobs.SimpleJob;
import com.bakdata.conquery.models.jobs.UpdateFilterSearchJob;
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
public abstract class Namespace extends IdResolveContext {

	private final ObjectMapper preprocessMapper;

	private final ObjectMapper communicationMapper;

	@ToString.Include
	private final NamespaceStorage storage;

	private final ExecutionManager executionManager;

	// TODO: 01.07.2020 FK: This is not used a lot, as NamespacedMessages are highly convoluted and hard to decouple as is.
	private final JobManager jobManager;

	private final FilterSearch filterSearch;

	private final IndexService indexService;

	private final EntityResolver entityResolver;

	// Jackson's injectables that are available when deserializing requests (see PathParamInjector) or items from the storage
	private final List<Injectable> injectables;

	public Dataset getDataset() {
		return storage.getDataset();
	}

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

	public CentralRegistry getCentralRegistry() {
		return getStorage().getCentralRegistry();
	}

	public int getNumberOfEntities() {
		return getStorage().getNumberOfEntities();
	}

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

	public void clearIndexCache() {
		indexService.evictCache();
	}

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
	public CentralRegistry getMetaRegistry() {
		throw new UnsupportedOperationException();
	}


	/**
	 * Issues a job that initializes the search that is used by the frontend for recommendations in the filter interface of a concept.
	 */
	final void updateFilterSearch() {
		getJobManager().addSlowJob(new UpdateFilterSearchJob(this, getFilterSearch().getIndexConfig(), this::registerColumnValuesInSearch));
	}

	/**
	 * Issues a job that collects basic metrics for every concept and its nodes. This information is displayed in the frontend.
	 */
	abstract void updateMatchingStats();

	/**
	 * This collects the string values of the given {@link Column}s (each is a {@link com.bakdata.conquery.models.datasets.concepts.Searchable})
	 * and registers them in the namespace's {@link FilterSearch#registerValues(Searchable, Collection)}.
	 * After value registration for a column is complete, {@link FilterSearch#shrinkSearch(Searchable)} should be called.
	 *
	 * @param columns
	 */
	abstract void registerColumnValuesInSearch(Set<Column> columns);

	/**
	 * Hook for actions that are best done after all data has been imported and is in a consistent state.
	 * Such actions are for example search initialization and collection of matching statistics.
	 *
	 * @implNote This intentionally submits a SlowJob so that it will be queued after all jobs that are already in the queue (usually import jobs).
	 */
	public void postprocessData() {

		getJobManager().addSlowJob(new SimpleJob(
				"Initiate Update Matching Stats and FilterSearch",
				() -> {
					updateMatchingStats();
					updateFilterSearch();
					updateInternToExternMappings();
				}
		));

	}
}
