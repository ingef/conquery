package com.bakdata.conquery.integration.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;

import com.bakdata.conquery.apiv1.execution.FullExecutionStatus;
import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.apiv1.query.SecondaryIdQuery;
import com.bakdata.conquery.apiv1.query.concept.filter.CQTable;
import com.bakdata.conquery.apiv1.query.concept.filter.FilterValue;
import com.bakdata.conquery.apiv1.query.concept.specific.CQAnd;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.apiv1.query.concept.specific.CQReusedQuery;
import com.bakdata.conquery.integration.common.IntegrationUtils;
import com.bakdata.conquery.integration.json.JsonIntegrationTest;
import com.bakdata.conquery.integration.json.QueryTest;
import com.bakdata.conquery.integration.json.TestDataImporter;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.FilterId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
import com.bakdata.conquery.models.query.ManagedQuery;
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
	public Set<StandaloneSupport.Mode> forModes() {
		return Set.of(StandaloneSupport.Mode.WORKER, StandaloneSupport.Mode.SQL);
	}

	@Override
	public void execute(String name, TestConquery testConquery) throws Exception {

		final StandaloneSupport conquery = testConquery.getSupport(name);


		final String testJson = In.resource("/tests/query/SECONDARY_ID_MIXED/SECONDARY_IDS_MIXED.test.json").withUTF8().readAll();

		final DatasetId dataset = conquery.getDataset();


		final QueryTest test = JsonIntegrationTest.readJson(dataset, testJson);

		// Manually import data, so we can do our own work.
		ValidatorHelper.failOnError(log, conquery.getValidator().validate(test));
		TestDataImporter testImporter = conquery.getTestImporter();

		testImporter.importSecondaryIds(conquery, test.getContent().getSecondaryIds());
		testImporter.importTables(conquery, test.getContent().getTables(), test.getContent().isAutoConcept());
		testImporter.importConcepts(conquery, test.getRawConcepts());
		testImporter.importTableContents(conquery, test.getContent().getTables());

		final SecondaryIdQuery query = (SecondaryIdQuery) IntegrationUtils.parseQuery(conquery, test.getRawQuery());

		final long expectedSize = 3L;

		final ManagedExecutionId id = IntegrationUtils.assertQueryResult(conquery, query, expectedSize, ExecutionState.DONE, conquery.getTestUser(), 201);

		assertThat(id).isNotNull();

		final MetaStorage metaStorage = conquery.getMetaStorage();
		final ManagedQuery execution = (ManagedQuery) metaStorage.getExecution(id);

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

			IntegrationUtils.assertQueryResult(conquery, reused, expectedSize, ExecutionState.DONE, conquery.getTestUser(), 201);
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
			final ConceptId conceptId = new ConceptId(conquery.getDataset(), "concept");
			final NamespaceStorage namespaceStorage = conquery.getNamespaceStorage();
			final Concept<?> concept = namespaceStorage.getConcept(conceptId);
			cqConcept.setElements(List.of(concept.getId()));
			final CQTable cqTable = new CQTable();
			cqTable.setConcept(cqConcept);

			ConnectorId connector1 = new ConnectorId(conceptId, "connector1");
			connector1.setDomain(conquery.getDatasetRegistry());
			final Connector connector = connector1.resolve();
			cqTable.setConnector(connector.getId());
			cqTable.setFilters(List.of(new FilterValue.CQRealRangeFilter(new FilterId(connector.getId(), "filter"), new Range<>(BigDecimal.valueOf(1.01d), BigDecimal.valueOf(1.01d)))));

			cqConcept.setTables(List.of(cqTable));
			cqConcept.setExcludeFromSecondaryId(false);


			root.setChildren(List.of(reuse, cqConcept));

			reused.setSecondaryId(query.getSecondaryId());

			IntegrationUtils.assertQueryResult(conquery, reused, 1L, ExecutionState.DONE, conquery.getTestUser(), 201);
		}

		// Reuse Multiple times with different query types
		{
			final SecondaryIdQuery reused1 = new SecondaryIdQuery();
			reused1.setRoot(new CQReusedQuery(execution.getId()));

			reused1.setSecondaryId(query.getSecondaryId());

			final ManagedExecutionId reused1Id = IntegrationUtils.assertQueryResult(conquery, reused1, expectedSize, ExecutionState.DONE, conquery.getTestUser(), 201);
			final ManagedQuery execution1 = (ManagedQuery) metaStorage.getExecution(reused1Id);
			{
				final SecondaryIdQuery reused2 = new SecondaryIdQuery();
				reused2.setRoot(new CQReusedQuery(execution1.getId()));

				reused2.setSecondaryId(query.getSecondaryId());

				final ManagedExecutionId
						reused2Id =
						IntegrationUtils.assertQueryResult(conquery, reused2, expectedSize, ExecutionState.DONE, conquery.getTestUser(), 201);
				final ManagedQuery execution2 = (ManagedQuery) metaStorage.getExecution(reused2Id);

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
				reusedDiffId.setSecondaryId(new SecondaryIdDescriptionId(conquery.getDataset(), "ignored"));

				final ManagedExecutionId
						executionId =
						IntegrationUtils.assertQueryResult(conquery, reusedDiffId, 2L, ExecutionState.DONE, conquery.getTestUser(), 201);

				assertThat(executionId)
						.as("Query should NOT be reused.")
						.isNotEqualTo(reused1Id);
			}

			{
				// Reuse by another user (create a copy of the actual query)

				final SecondaryIdQuery reused = new SecondaryIdQuery();
				reused.setRoot(new CQReusedQuery(execution.getId()));

				reused.setSecondaryId(query.getSecondaryId());

				User shareHolder = new User("shareholder", "ShareHolder", conquery.getMetaStorage());
				conquery.getAdminProcessor().addUser(shareHolder);

				shareHolder.addPermissions(Set.of(
						dataset.createPermission(Set.of(Ability.READ)),
						execution.createPermission(Set.of(Ability.READ))
				));

				ManagedExecutionId copyId = IntegrationUtils.assertQueryResult(conquery, reused, expectedSize, ExecutionState.DONE, shareHolder, 201);

				ManagedExecution copy = metaStorage.getExecution(copyId);


				// Contentwise the label and tags should be the same
				assertThat(copy).usingRecursiveComparison().comparingOnlyFields("label","tags").isEqualTo(execution);

				// However the Object holding the tags must be different, so the two are not linked here
				assertThat(copy.getTags()).isNotSameAs(execution.getTags());

				// And the ids must be different
				assertThat(copy.getId()).isNotSameAs(execution.getId());
			}
		}
	}

}
