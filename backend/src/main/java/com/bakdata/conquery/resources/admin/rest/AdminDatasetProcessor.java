package com.bakdata.conquery.resources.admin.rest;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.mode.ImportHandler;
import com.bakdata.conquery.mode.StorageListener;
import com.bakdata.conquery.mode.ValidationMode;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.PreviewConfig;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.StructureNode;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.SelectFilter;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.MappableSingleColumnSelect;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.identifiable.ids.specific.InternToExternMapperId;
import com.bakdata.conquery.models.identifiable.ids.specific.SearchIndexId;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.identifiable.mapping.EntityIdMap;
import com.bakdata.conquery.models.index.InternToExternMapper;
import com.bakdata.conquery.models.index.search.SearchIndex;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.DistributedNamespace;
import com.bakdata.conquery.models.worker.LocalNamespace;
import com.bakdata.conquery.models.worker.Namespace;
import com.univocity.parsers.csv.CsvParser;
import io.dropwizard.core.setup.Environment;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RequiredArgsConstructor(onConstructor_ = {@Inject})
@Data
public class AdminDatasetProcessor {

	public static final int MAX_IMPORTS_TEXT_LENGTH = 100;
	private static final String ABBREVIATION_MARKER = "…";

	private final ConqueryConfig config;
	private final DatasetRegistry<? extends Namespace> datasetRegistry;
	private final MetaStorage metaStorage;
	private final JobManager jobManager;
	private final ImportHandler importHandler;
	private final StorageListener storageListener;
	private final Environment environment;


	/**
	 * Creates and initializes a new dataset if it does not already exist.
	 */
	public synchronized Dataset addDataset(Dataset dataset) throws IOException {

		final String name = dataset.getName();

		if (datasetRegistry.get(new DatasetId(name)) != null) {
			throw new WebApplicationException("Dataset already exists", Response.Status.CONFLICT);
		}

		return datasetRegistry.createNamespace(dataset, metaStorage, environment).getDataset();
	}

	/**
	 * Delete dataset if it is empty.
	 */
	public synchronized void deleteDataset(DatasetId dataset) {
		final Namespace namespace = datasetRegistry.get(dataset);

		try (Stream<Table> tableStream = namespace.getStorage().getTables()) {
			List<Table> tables = tableStream.toList();
			if (!tables.isEmpty()) {
				throw new WebApplicationException(
						String.format(
								"Cannot delete dataset `%s`, because it still has tables: `%s`",
								dataset,
								tables.stream()
									  .map(Table::getId)
									  .map(Objects::toString)
									  .collect(Collectors.joining(","))
						),
						Response.Status.CONFLICT
				);
			}
		}

		datasetRegistry.removeNamespace(dataset);

	}

	/**
	 * Add SecondaryId if it doesn't already exist.
	 */
	public synchronized void addSecondaryId(Namespace namespace, SecondaryIdDescription secondaryId) {
		final Dataset dataset = namespace.getDataset();
		secondaryId.setDataset(dataset.getId());

		if (namespace.getStorage().getSecondaryId(secondaryId.getId()) != null) {
			throw new WebApplicationException("SecondaryId already exists", Response.Status.CONFLICT);
		}

		log.info("Received new SecondaryId[{}]", secondaryId.getId());

		namespace.getStorage().addSecondaryId(secondaryId);
		storageListener.onAddSecondaryId(secondaryId);
	}

	/**
	 * Delete SecondaryId if it does not have any dependents.
	 */
	public synchronized void deleteSecondaryId(SecondaryIdDescriptionId secondaryId) {
		final Namespace namespace = datasetRegistry.get(secondaryId.getDataset());

		// Before we commit this deletion, we check if this SecondaryId still has dependent Columns.
		final List<Column> dependents;
		try (Stream<Table> tables = namespace.getStorage().getTables()) {

			dependents = tables.map(Table::getColumns).flatMap(Arrays::stream)
							   .filter(column -> secondaryId.equals(column.getSecondaryId()))
							   .toList();
		}

		if (!dependents.isEmpty()) {
			final Set<TableId> tables = dependents.stream().map(Column::getTable).map(Identifiable::getId).collect(Collectors.toSet());
			log.error(
					"SecondaryId[{}] still present on {}",
					secondaryId,
					tables
			);

			throw new ForbiddenException(String.format("SecondaryId still has dependencies. %s", tables));
		}

		log.info("Deleting SecondaryId[{}]", secondaryId);

		namespace.getStorage().removeSecondaryId(secondaryId);
		storageListener.onDeleteSecondaryId(secondaryId);
	}

	/**
	 * Add table to Dataset if it doesn't already exist.
	 */
	@SneakyThrows
	public synchronized void addTable(@NonNull Table table, Namespace namespace) {

		ValidatorHelper.failOnError(log, environment.getValidator().validate(table));


		if (namespace.getStorage().getTable(table.getId()) != null) {
			throw new WebApplicationException("Table already exists", Response.Status.CONFLICT);
		}

		Class<? extends ValidationMode> mode =
				switch (namespace) {
					case LocalNamespace ignored -> ValidationMode.Local.class;
					case DistributedNamespace ignored -> ValidationMode.Clustered.class;
					default -> throw new IllegalStateException("Unexpected Namespace class %s".formatted(namespace.getClass()));
				};


		Set<ConstraintViolation<Table>> violations = new HashSet<>();
		// Default.class isn't properly packaged, so do it manually
		violations.addAll(environment.getValidator().validate(table));
		violations.addAll(environment.getValidator().validate(table, mode));

		Optional<String> maybeViolations = ValidatorHelper.createViolationsString(violations, false);

		if (maybeViolations.isPresent()) {
			throw new BadRequestException(maybeViolations.get());
		}

		namespace.getStorage().addTable(table);
		storageListener.onAddTable(table);
	}


	/**
	 * update a concept of the given dataset.
	 * Therefore, the concept will be deleted first then added
	 */
	public synchronized void updateConcept(@NonNull Namespace namespace, @NonNull Concept<?> concept) {

		if (!namespace.getStorage().hasConcept(concept.getId())) {
			throw new NotFoundException("Can't find the concept in the dataset " + concept.getId());
		}

		//adds new content of the content
		addConcept(namespace, concept, true);
	}

	/**
	 * Add the concept to the dataset if it does not exist yet
	 */
	public synchronized void addConcept(@NonNull Namespace namespace, @NonNull Concept<?> concept, boolean force) {
		NamespaceStorage namespaceStorage = namespace.getStorage();

		ValidatorHelper.failOnError(log, environment.getValidator().validate(concept));

		if (namespaceStorage.hasConcept(concept.getId())) {
			if (!force) {
				throw new WebApplicationException("Can't replace already existing concept " + concept.getId(), Response.Status.CONFLICT);
			}
			deleteConcept(concept.getId());
			log.info("Force deleted previous concept: {}", concept.getId());
		}

		// Register the Concept in the ManagerNode and Workers
		namespaceStorage.updateConcept(concept);
		storageListener.onAddConcept(concept);
	}

	/**
	 * Deletes a concept.
	 */
	public synchronized void deleteConcept(ConceptId concept) {
		final Namespace namespace = datasetRegistry.get(concept.getDataset());

		namespace.getStorage().removeConcept(concept);
		storageListener.onDeleteConcept(concept);
	}

	public void setPreviewConfig(PreviewConfig previewConfig, Namespace namespace) {
		log.info("Received new {}", previewConfig);

		ValidatorHelper.failOnError(log, environment.getValidator().validate(previewConfig));

		namespace.getStorage().setPreviewConfig(previewConfig);
	}

	/**
	 * Uploads new IdMapping.
	 */
	public void setIdMapping(InputStream data, Namespace namespace) {
		log.info("Received IdMapping for Dataset[{}]", namespace.getDataset().getId());

		final CsvParser parser = config.getCsv()
									   .withSkipHeader(false)
									   .withParseHeaders(true)
									   .createParser();

		try {

			parser.beginParsing(data);

			final EntityIdMap mapping = EntityIdMap.generateIdMapping(parser, config.getIdColumns().getIds(), namespace.getStorage());
			namespace.getStorage().updateIdMapping(mapping);

		}
		finally {
			parser.stopParsing();
		}
	}

	/**
	 * Uploads new Structure for namespace.
	 */
	public void setStructure(Namespace namespace, StructureNode[] structure) {
		log.info("Add Structure for Dataset[{}]", namespace.getDataset().getId());

		namespace.getStorage().updateStructure(structure);
	}

	/**
	 * Reads an Import partially Importing it if not yet present, then submitting it for full import.
	 */
	public void addImport(Namespace namespace, InputStream inputStream) throws IOException {
		importHandler.addImport(namespace, inputStream);
	}

	/**
	 * Reads an Import partially Importing it if it is present, then submitting it for full import [Update of an import].
	 */
	public void updateImport(Namespace namespace, InputStream inputStream) {
		importHandler.updateImport(namespace, inputStream);
	}

	/**
	 * Deletes a table if it has no dependents or not forced to do so.
	 */
	public synchronized List<ConceptId> deleteTable(TableId table, boolean force) {
		final Namespace namespace = datasetRegistry.get(table.getDataset());

		final List<Concept<?>> dependentConcepts;
		try (Stream<Concept<?>> allConcepts = namespace.getStorage().getAllConcepts()) {
			dependentConcepts = allConcepts.flatMap(c -> c.getConnectors().stream())
										   .filter(con -> con.resolveTableId().equals(table))
										   .map(Connector::getConcept)
										   .collect(Collectors.toList());
		}

		if (force || dependentConcepts.isEmpty()) {
			for (Concept<?> concept : dependentConcepts) {
				deleteConcept(concept.getId());
			}

			namespace.getStorage().getAllImports()
					 .filter(imp -> imp.getTable().equals(table))
					 .forEach(this::deleteImport);

			namespace.getStorage().removeTable(table);
			storageListener.onRemoveTable(table);
		}

		return dependentConcepts.stream().map(Concept::getId).collect(Collectors.toList());
	}

	/**
	 * Deletes an import.
	 */
	public synchronized void deleteImport(ImportId imp) {
		importHandler.deleteImport(imp);
	}

	/**
	 * Issues a postprocessing of the imported data for initializing certain internal modules that are either expensive or need the whole data present.
	 */
	public void postprocessNamespace(DatasetId dataset) {
		final Namespace ns = getDatasetRegistry().get(dataset);

		ns.postprocessData();
	}

	public EntityIdMap getIdMapping(Namespace namespace) {
		return namespace.getStorage().getIdMapping();
	}

	public void addInternToExternMapping(Namespace namespace, InternToExternMapper internToExternMapper) {
		// TODO Use DatasetParamInjector on admin-api to avoid manual setup of dataset/storage
		internToExternMapper.setStorage(namespace.getStorage());

		ValidatorHelper.failOnError(log, environment.getValidator().validate(internToExternMapper));

		if (namespace.getStorage().getInternToExternMapper(internToExternMapper.getId()) != null) {
			throw new WebApplicationException("InternToExternMapping already exists", Response.Status.CONFLICT);
		}

		log.info("Received new InternToExternMapping[{}]", internToExternMapper.getId());

		// We don't call internToExternMapper::init this is done by the first select that needs the mapping
		namespace.getStorage().addInternToExternMapper(internToExternMapper);
	}

	public List<ConceptId> deleteInternToExternMapping(InternToExternMapperId internToExternMapper, boolean force) {
		final Namespace namespace = datasetRegistry.get(internToExternMapper.getDataset());

		final Set<Concept<?>> dependentConcepts = namespace.getStorage().getAllConcepts()
														   .filter(
																   c -> c.getSelects().stream()
																		 .filter(MappableSingleColumnSelect.class::isInstance)

																		 .map(MappableSingleColumnSelect.class::cast)
																		 .map(MappableSingleColumnSelect::getMapping)
																		 .anyMatch(internToExternMapper::equals)
														   )
														   .collect(Collectors.toSet());

		if (force || dependentConcepts.isEmpty()) {
			for (Concept<?> concept : dependentConcepts) {
				deleteConcept(concept.getId());
			}

			namespace.getStorage().removeInternToExternMapper(internToExternMapper);
		}

		return dependentConcepts.stream().map(Concept::getId).collect(Collectors.toList());
	}

	public void clearIndexCache() {
		datasetRegistry.resetIndexService();
	}

	public void addSearchIndex(Namespace namespace, SearchIndex searchIndex) {
		searchIndex.setDataset(namespace.getDataset().getId());

		ValidatorHelper.failOnError(log, environment.getValidator().validate(searchIndex));

		if (namespace.getStorage().getSearchIndex(searchIndex.getId()) != null) {
			throw new WebApplicationException("InternToExternMapping already exists", Response.Status.CONFLICT);
		}

		log.info("Received new SearchIndex[{}]", searchIndex.getId());
		namespace.getStorage().addSearchIndex(searchIndex);
	}

	public List<ConceptId> deleteSearchIndex(SearchIndexId searchIndex, boolean force) {
		final Namespace namespace = datasetRegistry.get(searchIndex.getDataset());

		final List<Concept<?>> dependentConcepts = namespace.getStorage().getAllConcepts()
															.filter(
																	c -> c.getConnectors().stream()
																		  .map(Connector::getFilters)
																		  .flatMap(Collection::stream)
																		  .filter(SelectFilter.class::isInstance)
																		  .map(SelectFilter.class::cast)
																		  .map(SelectFilter::getTemplate)
																		  .filter(Objects::nonNull)
																		  .anyMatch(searchIndex::equals)
															)
															.toList();

		if (force || dependentConcepts.isEmpty()) {
			for (Concept<?> concept : dependentConcepts) {
				deleteConcept(concept.getId());
			}

			namespace.getStorage().removeSearchIndex(searchIndex);
		}

		return dependentConcepts.stream().map(Concept::getId).collect(Collectors.toList());
	}

	public void deletePreviewConfig(Namespace namespace) {
		namespace.getStorage().removePreviewConfig();
	}
}
