package com.bakdata.conquery.models.worker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.query.concept.specific.external.CQExternal;
import com.bakdata.conquery.apiv1.query.concept.specific.external.DateFormat;
import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.config.IdColumnConfig;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.PreviewConfig;
import com.bakdata.conquery.models.datasets.concepts.Searchable;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.MappableSingleColumnSelect;
import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.mapping.EntityIdMap;
import com.bakdata.conquery.models.index.IndexService;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.jobs.SimpleJob;
import com.bakdata.conquery.models.jobs.UpdateFilterSearchJob;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.models.query.FilterSearch;
import com.bakdata.conquery.util.DateReader;
import com.bakdata.conquery.util.io.IdColumnUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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

	/**
	 * Helper method to try and resolve entities in values using the specified format.
	 */
	public CQExternal.ResolveStatistic resolveEntities(
			@NotEmpty String[][] values,
			@NotEmpty List<String> format,
			EntityIdMap mapping,
			IdColumnConfig idColumnConfig,
			@NotNull DateReader dateReader,
			boolean onlySingles
	) {
		final Map<String, CDateSet> resolved = new HashMap<>();

		final List<String[]> unresolvedDate = new ArrayList<>();
		final List<String[]> unresolvedId = new ArrayList<>();

		// extract dates from rows
		final CDateSet[] rowDates = readDates(values, format, dateReader);

		// Extract extra data from rows by Row, to be collected into by entities
		// Row -> Column -> Value
		final Map<String, String>[] extraDataByRow = readExtras(values, format);

		final List<Function<String[], EntityIdMap.ExternalId>> readers = IdColumnUtil.getIdReaders(format, idColumnConfig.getIdMappers());

		// We will not be able to resolve anything...
		if (readers.isEmpty()) {
			return new CQExternal.ResolveStatistic(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyList(), List.of(values));
		}

		// Entity -> Column -> Values
		final Map<String, Map<String, List<String>>> extraDataByEntity = new HashMap<>();

		// ignore the first row, because this is the header
		for (int rowNum = 1; rowNum < values.length; rowNum++) {

			final String[] row = values[rowNum];

			if (rowDates[rowNum] == null) {
				unresolvedDate.add(row);
				continue;
			}

			String resolvedId = tryResolveId(row, readers, mapping);

			if (resolvedId == null) {
				unresolvedId.add(row);
				continue;
			}

			//read the dates from the row
			resolved.put(resolvedId, rowDates[rowNum]);

			// Entity was resolved for row so we collect the data.
			if (extraDataByRow[rowNum] != null) {

				for (Map.Entry<String, String> entry : extraDataByRow[rowNum].entrySet()) {
					extraDataByEntity.computeIfAbsent(resolvedId, (ignored) -> new HashMap<>())
									 .computeIfAbsent(entry.getKey(), (ignored) -> new ArrayList<>())
									 .add(entry.getValue());
				}
			}
		}

		if (onlySingles) {
			// Check that there is at most one value per entity and per column
			final boolean alright = extraDataByEntity.values().stream()
													 .map(Map::values)
													 .flatMap(Collection::stream)
													 .allMatch(l -> l.size() <= 1);
			if (!alright) {
				throw new ConqueryError.ExternalResolveOnePerRowError();
			}
		}

		return new CQExternal.ResolveStatistic(resolved, extraDataByEntity, unresolvedDate, unresolvedId);
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

	abstract String tryInnerResolveId(final EntityIdMap mapping, final EntityIdMap.ExternalId externalId);

	/**
	 * Try to extract a {@link com.bakdata.conquery.models.identifiable.mapping.EntityIdMap.ExternalId} from the row,
	 * then try to map it to an internal {@link com.bakdata.conquery.models.query.entity.Entity}
	 */
	private String tryResolveId(String[] row, List<Function<String[], EntityIdMap.ExternalId>> readers, EntityIdMap mapping) {
		String resolvedId = null;

		for (Function<String[], EntityIdMap.ExternalId> reader : readers) {
			final EntityIdMap.ExternalId externalId = reader.apply(row);

			if (externalId == null) {
				continue;
			}

			// differs between SQL and Worker mode
			String innerResolved = tryInnerResolveId(mapping, externalId);

			if (innerResolved == null) {
				continue;
			}

			// Only if all resolvable ids agree on the same entity, do we return the id.
			if (resolvedId != null && !innerResolved.equals(resolvedId)) {
				log.error("`{}` maps to different Entities", (Object) row);
				continue;
			}

			resolvedId = innerResolved;
		}
		return resolvedId;
	}

	/**
	 * For each row try and collect all dates.
	 *
	 * @return Row -> Dates
	 */
	private static CDateSet[] readDates(String[][] values, List<String> format, DateReader dateReader) {
		final CDateSet[] out = new CDateSet[values.length];

		final List<DateFormat> dateFormats = format.stream()
												   .map(Namespace::resolveDateFormat)
												   // Don't use Stream#toList to preserve null-values
												   .collect(Collectors.toList());

		/*
		 If no format is provided, put empty dates into output.
		 This indicates that no date context was provided and
		 the entries are not restricted by any date restriction,
		 but can also don't contribute to any date aggregation.
		 */
		if (dateFormats.stream().allMatch(Objects::isNull)) {
			// Initialize empty
			for (int row = 0; row < values.length; row++) {
				out[row] = CDateSet.createEmpty();
			}
			return out;
		}

		for (int row = 1; row < values.length; row++) {
			try {
				final CDateSet dates = CDateSet.createEmpty();

				// Collect all specified dates into a single set.
				for (int col = 0; col < dateFormats.size(); col++) {
					final DateFormat dateFormat = dateFormats.get(col);

					if (dateFormat == null) {
						continue;
					}
					dateFormat.readDates(values[row][col], dateReader, dates);
				}

				if (dates.isEmpty()) {
					continue;
				}

				if (out[row] == null) {
					out[row] = CDateSet.createEmpty();
				}

				out[row].addAll(dates);
			}
			catch (Exception e) {
				log.warn("Failed to parse Date from {}", row, e);
			}
		}

		return out;
	}

	/**
	 * Try and extract Extra data from input to be returned as extra-data in output.
	 * <p>
	 * Line -> ( Column -> Value )
	 */
	private static Map<String, String>[] readExtras(String[][] values, List<String> format) {

		final String[] names = values[0];
		final Map<String, String>[] extrasByRow = new Map[values.length];

		for (int line = 1; line < values.length; line++) {
			for (int col = 0; col < format.size(); col++) {
				if (!format.get(col).equals(CQExternal.FORMAT_EXTRA)) {
					continue;
				}


				if (extrasByRow[line] == null) {
					extrasByRow[line] = new HashMap<>(names.length);
				}

				extrasByRow[line].put(names[col], values[line][col]);
			}
		}

		return extrasByRow;
	}

	/**
	 * Try to resolve a date format, return nothing if not possible.
	 */
	private static DateFormat resolveDateFormat(String name) {
		try {
			return DateFormat.valueOf(name);
		}
		catch (IllegalArgumentException e) {
			return null; // Does not exist
		}
	}

}
