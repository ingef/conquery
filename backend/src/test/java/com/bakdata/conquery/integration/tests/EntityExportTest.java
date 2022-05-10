package com.bakdata.conquery.integration.tests;

import static com.bakdata.conquery.integration.common.LoadingUtil.importSecondaryIds;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.bakdata.conquery.integration.common.LoadingUtil;
import com.bakdata.conquery.integration.json.JsonIntegrationTest;
import com.bakdata.conquery.integration.json.QueryTest;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.resources.ResourceConstants;
import com.bakdata.conquery.resources.api.QueryResource;
import com.bakdata.conquery.resources.hierarchies.HierarchyHelper;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.bakdata.conquery.util.support.TestConquery;
import com.github.powerlibraries.io.In;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.description.LazyTextDescription;

/**
 * Adapted from {@link com.bakdata.conquery.integration.tests.deletion.ImportDeletionTest}, tests {@link com.bakdata.conquery.resources.api.QueryResource#getEntityData(Subject, QueryResource.EntityPreview, Optional, HttpServletRequest)}.
 */
@Slf4j
public class EntityExportTest implements ProgrammaticIntegrationTest {


	@Override
	public void execute(String name, TestConquery testConquery) throws Exception {

		final StandaloneSupport conquery = testConquery.getSupport(name);

		final String testJson = In.resource("/tests/query/DELETE_IMPORT_TESTS/SIMPLE_TREECONCEPT_Query.test.json").withUTF8().readAll();

		final Dataset dataset = conquery.getDataset();

		final QueryTest test = (QueryTest) JsonIntegrationTest.readJson(dataset, testJson);

		// Manually import data, so we can do our own work.
		{
			ValidatorHelper.failOnError(log, conquery.getValidator().validate(test));

			importSecondaryIds(conquery, test.getContent().getSecondaryIds());
			conquery.waitUntilWorkDone();

			LoadingUtil.importTables(conquery, test.getContent().getTables());
			conquery.waitUntilWorkDone();

			LoadingUtil.importConcepts(conquery, test.getRawConcepts());
			conquery.waitUntilWorkDone();

			LoadingUtil.importTableContents(conquery, test.getContent().getTables());
			conquery.waitUntilWorkDone();
		}

		final URI entityExport = HierarchyHelper.hierarchicalPath(conquery.defaultApiURIBuilder(), QueryResource.class, "getEntityData")
												.buildFromMap(Map.of(ResourceConstants.DATASET, conquery.getDataset().getName()));

		// Api uses NsIdRef so we have to use the real objects here.
		final List<Connector> allConnectors = conquery.getNamespaceStorage().getAllConcepts().stream()
													  .map(Concept::getConnectors)
													  .flatMap(List::stream)
													  .collect(Collectors.toList());

		final Response allEntityDataResponse =
				conquery.getClient().target(entityExport)
						.request(MediaType.APPLICATION_JSON_TYPE)
						.post(Entity.json(new QueryResource.EntityPreview("ID", "3", Range.all(), allConnectors)));

		assertThat(allEntityDataResponse.getStatusInfo().getFamily())
				.describedAs(new LazyTextDescription(() -> allEntityDataResponse.readEntity(String.class)))
				.isEqualTo(Response.Status.Family.SUCCESSFUL);

		final String rawData = allEntityDataResponse.readEntity(String.class);

		assertThat(rawData)
				.isEqualTo("result,Datumswerte,test_table test_column,test_table2 test_column\n3,10.11.2013,A1,\n");

	}

}
