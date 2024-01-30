package com.bakdata.conquery.integration.sql.programmatic;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;

import com.bakdata.conquery.apiv1.execution.FullExecutionStatus;
import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.apiv1.query.concept.specific.CQReusedQuery;
import com.bakdata.conquery.integration.common.IntegrationUtils;
import com.bakdata.conquery.integration.json.JsonIntegrationTest;
import com.bakdata.conquery.integration.json.QueryTest;
import com.bakdata.conquery.integration.tests.ProgrammaticIntegrationTest;
import com.bakdata.conquery.integration.tests.ReusedQueryTest;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.resources.ResourceConstants;
import com.bakdata.conquery.resources.api.QueryResource;
import com.bakdata.conquery.resources.hierarchies.HierarchyHelper;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.bakdata.conquery.util.support.TestConquery;
import com.github.powerlibraries.io.In;
import lombok.extern.slf4j.Slf4j;

/**
 * This test duplicates parts of the {@link com.bakdata.conquery.integration.tests.ReusedQueryTest}, which itself is not SQL-ready yet, because it uses
 * secondary ID's in its test. As soon as the SQL connector supports secondary ID's, this test can be removed.
 */
@Slf4j
public class SqlReusedQueryTest implements ProgrammaticIntegrationTest {

	@Override
	public Set<StandaloneSupport.Mode> forModes() {
		return Set.of(StandaloneSupport.Mode.SQL);
	}

	@Override
	public void execute(String name, TestConquery testConquery) throws Exception {

		final StandaloneSupport conquery = testConquery.getSupport(name);

		final String testJson = In.resource("/tests/sql/filter/count_distinct/count_distinct.json").withUTF8().readAll();

		final Dataset dataset = conquery.getDataset();
		final QueryTest test = JsonIntegrationTest.readJson(dataset, testJson);

		ReusedQueryTest.importManually(conquery, test);

		final Query query = IntegrationUtils.parseQuery(conquery, test.getRawQuery());
		final ManagedExecutionId id = IntegrationUtils.assertQueryResult(conquery, query, 2L, ExecutionState.DONE, conquery.getTestUser(), 201);

		assertThat(id).isNotNull();

		final MetaStorage metaStorage = conquery.getMetaStorage();
		final ManagedExecution execution = metaStorage.getExecution(id);

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
	}

}
