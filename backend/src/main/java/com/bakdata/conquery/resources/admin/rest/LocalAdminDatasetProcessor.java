package com.bakdata.conquery.resources.admin.rest;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Validator;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Import;
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
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.identifiable.mapping.EntityIdMap;
import com.bakdata.conquery.models.index.InternToExternMapper;
import com.bakdata.conquery.models.index.search.SearchIndex;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.jobs.SimpleJob;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.sql.conquery.SqlDatasetRegistry;
import com.bakdata.conquery.sql.conquery.SqlNamespace;
import com.univocity.parsers.csv.CsvParser;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@Getter
public class LocalAdminDatasetProcessor implements AdminDatasetProcessor<SqlNamespace> {
	private final ConqueryConfig config;
	private final Validator validator;
	private final SqlDatasetRegistry datasetRegistry;
	private final JobManager jobManager;


	@Override
	@SneakyThrows(IOException.class)
	public synchronized Dataset addDataset(Dataset dataset) {

		final String name = dataset.getName();
		if (datasetRegistry.get(new DatasetId(name)) != null) {
			throw new WebApplicationException("Dataset already exists", Response.Status.CONFLICT);
		}

		return datasetRegistry.createNamespace(dataset, getValidator()).getDataset();
	}

	@Override
	public synchronized void deleteDataset(Dataset dataset) {
		final Namespace namespace = datasetRegistry.get(dataset.getId());

		if (!namespace.getStorage().getTables().isEmpty()) {
			throw new WebApplicationException(
					String.format(
							"Cannot delete dataset `%s`, because it still has tables: `%s`",
							dataset.getId(),
							namespace.getStorage().getTables().stream()
									 .map(Table::getId)
									 .map(Objects::toString)
									 .collect(Collectors.joining(","))
					),
					Response.Status.CONFLICT
			);
		}

		datasetRegistry.removeNamespace(dataset.getId());

	}

	@Override
	public synchronized void addSecondaryId(SqlNamespace namespace, SecondaryIdDescription secondaryId) {
		final Dataset dataset = namespace.getDataset();
		secondaryId.setDataset(dataset);

		if (namespace.getStorage().getSecondaryId(secondaryId.getId()) != null) {
			throw new WebApplicationException("SecondaryId already exists", Response.Status.CONFLICT);
		}

		log.info("Received new SecondaryId[{}]", secondaryId.getId());
		namespace.getStorage().addSecondaryId(secondaryId);
	}

	@Override
	public synchronized void deleteSecondaryId(@NonNull SecondaryIdDescription secondaryId) {
		final SqlNamespace namespace = datasetRegistry.get(secondaryId.getDataset().getId());

		// Before we commit this deletion, we check if this SecondaryId still has dependent Columns.
		final List<Column> dependents = namespace.getStorage().getTables().stream()
												 .map(Table::getColumns).flatMap(Arrays::stream)
												 .filter(column -> secondaryId.equals(column.getSecondaryId()))
												 .toList();

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

		namespace.getStorage().removeSecondaryId(secondaryId.getId());
	}

	@Override
	@SneakyThrows
	public synchronized void addTable(@NonNull Table table, SqlNamespace namespace) {
		Dataset dataset = namespace.getDataset();

		if (table.getDataset() == null) {
			table.setDataset(dataset);
		}
		else if (!table.getDataset().equals(dataset)) {
			throw new IllegalArgumentException();
		}


		if (namespace.getStorage().getTable(table.getId()) != null) {
			throw new WebApplicationException("Table already exists", Response.Status.CONFLICT);
		}

		ValidatorHelper.failOnError(log, validator.validate(table));

		namespace.getStorage().addTable(table);
	}


	@Override
	public synchronized void updateConcept(@NonNull Dataset dataset, @NonNull Concept<?> concept) {
		concept.setDataset(dataset);
		if (!datasetRegistry.get(dataset.getId()).getStorage().hasConcept(concept.getId())) {
			throw new NotFoundException("Can't find the concept in the dataset " + concept.getId());
		}

		//adds new content of the content
		addConcept(dataset, concept, true);
	}

	@Override
	public synchronized void addConcept(@NonNull Dataset dataset, @NonNull Concept<?> concept, boolean force) {
		concept.setDataset(dataset);
		ValidatorHelper.failOnError(log, validator.validate(concept));

		if (datasetRegistry.get(dataset.getId()).getStorage().hasConcept(concept.getId())) {
			if (!force) {
				throw new WebApplicationException("Can't replace already existing concept " + concept.getId(), Response.Status.CONFLICT);
			}
			deleteConcept(concept);
			log.info("Force deleted previous concept: {}", concept.getId());
		}
		// Register the Concept in the ManagerNode and Workers
		datasetRegistry.get(dataset.getId()).getStorage().updateConcept(concept);
	}


	@Override
	public void setPreviewConfig(PreviewConfig previewConfig, SqlNamespace namespace) {
		log.info("Received new {}", previewConfig);

		ValidatorHelper.failOnError(log, getValidator().validate(previewConfig));

		namespace.getStorage().setPreviewConfig(previewConfig);
	}

	@Override
	public void setIdMapping(InputStream data, SqlNamespace namespace) {
		log.info("Received IdMapping for Dataset[{}]", namespace.getDataset().getId());

		CsvParser parser = config.getCsv()
								 .withSkipHeader(false)
								 .withParseHeaders(true)
								 .createParser();

		try {

			parser.beginParsing(data);

			EntityIdMap mapping = EntityIdMap.generateIdMapping(parser, config.getIdColumns().getIds());
			namespace.getStorage().updateIdMapping(mapping);

		}
		finally {
			parser.stopParsing();
		}
	}

	@Override
	public void setStructure(SqlNamespace namespace, StructureNode[] structure) {
		log.info("Add Structure for Dataset[{}]", namespace.getDataset().getId());

		namespace.getStorage().updateStructure(structure);
	}


	@Override
	@SneakyThrows
	public void addImport(SqlNamespace namespace, InputStream inputStream) throws IOException {
		throw new UnsupportedOperationException("Import does not work for local execution mode");
	}

	@Override
	@SneakyThrows
	public void updateImport(SqlNamespace namespace, InputStream inputStream) {
		throw new UnsupportedOperationException("Import does not work for local execution mode");
	}

	@Override
	public synchronized void deleteImport(Import imp) {
		throw new UnsupportedOperationException("Import does not work for local execution mode");
	}

	@Override
	public synchronized List<ConceptId> deleteTable(Table table, boolean force) {
		final SqlNamespace namespace = datasetRegistry.get(table.getDataset().getId());

		final List<Concept<?>> dependentConcepts = namespace.getStorage().getAllConcepts().stream().flatMap(c -> c.getConnectors().stream())
															.filter(con -> con.getTable().equals(table))
															.map(Connector::getConcept)
															.collect(Collectors.toList());

		if (force || dependentConcepts.isEmpty()) {
			for (Concept<?> concept : dependentConcepts) {
				deleteConcept(concept);
			}

			namespace.getStorage().getAllImports().stream()
					 .filter(imp -> imp.getTable().equals(table))
					 .forEach(this::deleteImport);

			namespace.getStorage().removeTable(table.getId());
		}

		return dependentConcepts.stream().map(Concept::getId).collect(Collectors.toList());
	}

	@Override
	public synchronized void deleteConcept(Concept<?> concept) {
		final SqlNamespace namespace = datasetRegistry.get(concept.getDataset().getId());
		namespace.getStorage().removeConcept(concept.getId());
	}

	@Override
	public void updateMatchingStats(Dataset dataset) {
		final SqlNamespace ns = getDatasetRegistry().get(dataset.getId());

		ns.getJobManager().addSlowJob(new SimpleJob(
				"Initiate Update Matching Stats and FilterSearch",
				() -> {
					ns.getFilterSearch().updateSearch();
					ns.updateInternToExternMappings();
				}
		));
	}

	@Override
	public EntityIdMap getIdMapping(SqlNamespace namespace) {
		return namespace.getStorage().getIdMapping();
	}

	@Override
	public void addInternToExternMapping(SqlNamespace namespace, InternToExternMapper internToExternMapper) {
		internToExternMapper.setDataset(namespace.getDataset());

		ValidatorHelper.failOnError(log, validator.validate(internToExternMapper));

		if (namespace.getStorage().getInternToExternMapper(internToExternMapper.getId()) != null) {
			throw new WebApplicationException("InternToExternMapping already exists", Response.Status.CONFLICT);
		}

		log.info("Received new InternToExternMapping[{}]", internToExternMapper.getId());

		// We don't call internToExternMapper::init this is done by the first select that needs the mapping
		namespace.getStorage().addInternToExternMapper(internToExternMapper);
	}

	@Override
	public List<ConceptId> deleteInternToExternMapping(InternToExternMapper internToExternMapper, boolean force) {
		final Namespace namespace = datasetRegistry.get(internToExternMapper.getDataset().getId());

		final Set<Concept<?>> dependentConcepts = namespace.getStorage().getAllConcepts().stream()
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
				deleteConcept(concept);
			}

			namespace.getStorage().removeInternToExternMapper(internToExternMapper.getId());
		}

		return dependentConcepts.stream().map(Concept::getId).collect(Collectors.toList());
	}

	@Override
	public void clearIndexCache(SqlNamespace namespace) {
		namespace.clearIndexCache();
	}

	@Override
	public void addSearchIndex(SqlNamespace namespace, SearchIndex searchIndex) {
		searchIndex.setDataset(namespace.getDataset());

		ValidatorHelper.failOnError(log, validator.validate(searchIndex));

		if (namespace.getStorage().getSearchIndex(searchIndex.getId()) != null) {
			throw new WebApplicationException("InternToExternMapping already exists", Response.Status.CONFLICT);
		}

		log.info("Received new SearchIndex[{}]", searchIndex.getId());
		namespace.getStorage().addSearchIndex(searchIndex);
	}

	@Override
	public List<ConceptId> deleteSearchIndex(SearchIndex searchIndex, boolean force) {
		final Namespace namespace = datasetRegistry.get(searchIndex.getDataset().getId());

		final List<Concept<?>> dependentConcepts = namespace.getStorage().getAllConcepts().stream()
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
				deleteConcept(concept);
			}

			namespace.getStorage().removeSearchIndex(searchIndex.getId());
		}

		return dependentConcepts.stream().map(Concept::getId).collect(Collectors.toList());
	}

	@Override
	public void deletePreviewConfig(SqlNamespace namespace) {
		namespace.getStorage().removePreviewConfig();
	}
}
