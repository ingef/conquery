package com.bakdata.conquery.integration.tests;

import static com.bakdata.conquery.integration.common.LoadingUtil.importSecondaryIds;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;

import com.bakdata.conquery.integration.common.IntegrationUtils;
import com.bakdata.conquery.integration.common.LoadingUtil;
import com.bakdata.conquery.integration.json.JsonIntegrationTest;
import com.bakdata.conquery.integration.json.QueryTest;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.filters.Filter;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.apiv1.FullExecutionStatus;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;
import com.bakdata.conquery.models.identifiable.ids.specific.FilterId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.apiv1.query.SecondaryIdQuery;
import com.bakdata.conquery.apiv1.query.concept.filter.CQTable;
import com.bakdata.conquery.apiv1.query.concept.filter.FilterValue;
import com.bakdata.conquery.apiv1.query.concept.specific.CQAnd;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.apiv1.query.concept.specific.CQReusedQuery;
import com.bakdata.conquery.resources.ResourceConstants;
import com.bakdata.conquery.resources.api.QueryResource;
import com.bakdata.conquery.resources.hierarchies.HierarchyHelper;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.bakdata.conquery.util.support.TestConquery;
import com.github.powerlibraries.io.In;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class ReusedQueryTest implements ProgrammaticIntegrationTest {

	@Override
	public void execute(String name, TestConquery testConquery) throws Exception {

		final StandaloneSupport conquery = testConquery.getSupport(name);


		final String testJson = In.resource("/tests/query/SECONDARY_ID_MIXED/SECONDARY_IDS_MIXED.test.json").withUTF8().readAll();

		final Dataset dataset = conquery.getDataset();


		final QueryTest test = (QueryTest) JsonIntegrationTest.readJson(dataset, testJson);

		// Manually import data, so we can do our own work.
		{
			ValidatorHelper.failOnError(log, conquery.getValidator().validate(test));

			importSecondaryIds(conquery, test.getContent().getSecondaryIds());
			conquery.waitUntilWorkDone();

			LoadingUtil.importTables(conquery, test.getContent());
			conquery.waitUntilWorkDone();

			LoadingUtil.importConcepts(conquery, test.getRawConcepts());
			conquery.waitUntilWorkDone();

			LoadingUtil.importTableContents(conquery, test.getContent().getTables(), conquery.getDataset());
			conquery.waitUntilWorkDone();
		}

		final SecondaryIdQuery query = (SecondaryIdQuery) IntegrationUtils.parseQuery(conquery, test.getRawQuery());

		final ManagedExecutionId id = IntegrationUtils.assertQueryResult(conquery, query, 4L, ExecutionState.DONE, conquery.getTestUser(), 201);

		assertThat(id).isNotNull();

		final ManagedQuery execution = (ManagedQuery) conquery.getMetaStorage().getExecution(id);

		// Normal reuse
		{

			final ConceptQuery reused = new ConceptQuery(new CQReusedQuery(execution.getId()));

			IntegrationUtils.assertQueryResult(conquery, reused, 2L, ExecutionState.DONE, conquery.getTestUser(), 201);
		}

		// Reuse by API
		{
			final URI reexecuteUri =
					HierarchyHelper.hierarchicalPath(conquery.defaultApiURIBuilder(), QueryResource.class, "reexecute")
								   .buildFromMap(Map.of(
										   ResourceConstants.DATASET, conquery.getDataset().getName(),
										   ResourceConstants.QUERY, execution.getId().toString()
								   ));

			final FullExecutionStatus status = conquery.getClient().target(reexecuteUri)
																	.request(MediaType.APPLICATION_JSON)
																	.post(Entity.entity(null, MediaType.APPLICATION_JSON_TYPE))
																	.readEntity(FullExecutionStatus.class);

			assertThat(status.getStatus()).isIn(ExecutionState.RUNNING, ExecutionState.DONE);

		}

		// Reuse in SecondaryId
		{
			final SecondaryIdQuery reused = new SecondaryIdQuery();
			reused.setRoot(new CQReusedQuery(execution.getId()));

			reused.setSecondaryId(query.getSecondaryId());

			IntegrationUtils.assertQueryResult(conquery, reused, 4L, ExecutionState.DONE, conquery.getTestUser(), 201);
		}

		// Reuse in SecondaryId, but do exclude
		{
			final SecondaryIdQuery reused = new SecondaryIdQuery();

			final CQAnd root = new CQAnd();
			reused.setRoot(root);

			final CQReusedQuery reuse = new CQReusedQuery(execution.getId());
			reuse.setExcludeFromSecondaryId(true);

			// We select only a single event of the query by the exact filtering.
			final CQConcept cqConcept = new CQConcept();
			final ConceptId conceptId = new ConceptId(conquery.getDataset().getId(), "concept");
			final Concept<?> concept = conquery.getNamespaceStorage().getConcept(conceptId);
			cqConcept.setElements(List.of(concept));
			final CQTable cqTable = new CQTable();
			cqTable.setConcept(cqConcept);

			final CentralRegistry centralRegistry = conquery.getNamespaceStorage().getCentralRegistry();
			final Connector connector = centralRegistry.resolve(new ConnectorId(conceptId, "connector1"));
			cqTable.setConnector(connector);
			cqTable.setFilters(List.of(new FilterValue.CQRealRangeFilter((Filter<Range<BigDecimal>>) centralRegistry.resolve(new FilterId(connector.getId(), "filter")), new Range<>(BigDecimal.valueOf(1.01d), BigDecimal.valueOf(1.01d)))));

			cqConcept.setTables(List.of(cqTable));
			cqConcept.setExcludeFromSecondaryIdQuery(false);


			root.setChildren(List.of(reuse, cqConcept));

			reused.setSecondaryId(query.getSecondaryId());

			IntegrationUtils.assertQueryResult(conquery, reused, 1L, ExecutionState.DONE, conquery.getTestUser(), 201);
		}

		// Reuse Multiple times with different query types
		{
			final SecondaryIdQuery reused1 = new SecondaryIdQuery();
			reused1.setRoot(new CQReusedQuery(execution.getId()));

			reused1.setSecondaryId(query.getSecondaryId());

			final ManagedExecutionId reused1Id = IntegrationUtils.assertQueryResult(conquery, reused1, 4L, ExecutionState.DONE, conquery.getTestUser(), 201);
			final ManagedQuery execution1 = (ManagedQuery) conquery.getMetaStorage().getExecution(reused1Id);
			{
				final SecondaryIdQuery reused2 = new SecondaryIdQuery();
				reused2.setRoot(new CQReusedQuery(execution1.getId()));

				reused2.setSecondaryId(query.getSecondaryId());

				final ManagedExecutionId
						reused2Id =
						IntegrationUtils.assertQueryResult(conquery, reused2, 4L, ExecutionState.DONE, conquery.getTestUser(), 201);
				final ManagedQuery execution2 = (ManagedQuery) conquery.getMetaStorage().getExecution(reused2Id);

				assertThat(reused2Id)
						.as("Query should be reused.")
						.isEqualTo(reused1Id);

				// Now we change to ConceptQuery
				final ConceptQuery reused3 = new ConceptQuery(new CQReusedQuery(execution2.getId()));

				IntegrationUtils.assertQueryResult(conquery, reused3, 2L, ExecutionState.DONE, conquery.getTestUser(), 201);
			}

			{
				final SecondaryIdQuery reusedDiffId = new SecondaryIdQuery();
				reusedDiffId.setRoot(new CQReusedQuery(execution1.getId()));

				// ignored is a single global value and therefore the same as by-PID
				reusedDiffId.setSecondaryId(conquery.getNamespace()
													.getStorage()
													.getSecondaryId(new SecondaryIdDescriptionId(conquery.getDataset().getId(), "ignored")));

				final ManagedExecutionId
						executionId =
						IntegrationUtils.assertQueryResult(conquery, reusedDiffId, 2L, ExecutionState.DONE, conquery.getTestUser(), 201);

				assertThat(executionId)
						.as("Query should NOT be reused.")
						.isNotEqualTo(reused1Id);
			}
		}
	}
}
