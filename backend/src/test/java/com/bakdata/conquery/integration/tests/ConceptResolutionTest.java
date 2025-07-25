package com.bakdata.conquery.integration.tests;

import static com.bakdata.conquery.resources.ResourceConstants.CONCEPT;
import static com.bakdata.conquery.resources.ResourceConstants.DATASET;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.bakdata.conquery.integration.IntegrationTest;
import com.bakdata.conquery.integration.json.ConqueryTestSpec;
import com.bakdata.conquery.integration.json.JsonIntegrationTest;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.resources.admin.rest.AdminDatasetResource;
import com.bakdata.conquery.resources.api.ConceptResource;
import com.bakdata.conquery.resources.api.ConceptsProcessor.ResolvedConceptsResult;
import com.bakdata.conquery.resources.hierarchies.HierarchyHelper;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.github.powerlibraries.io.In;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConceptResolutionTest extends IntegrationTest.Simple implements ProgrammaticIntegrationTest {

	@Override
	public void execute(StandaloneSupport conquery) throws Exception {
		//read test sepcification
		String testJson = In.resource("/tests/query/SIMPLE_TREECONCEPT_QUERY/SIMPLE_TREECONCEPT_Query.test.json").withUTF8().readAll();

		DatasetId dataset = conquery.getDataset();

		ConqueryTestSpec test = JsonIntegrationTest.readJson(dataset, testJson);
		ValidatorHelper.failOnError(log, conquery.getValidator().validate(test));

		test.importRequiredData(conquery);

		final URI matchingStatsUri = HierarchyHelper.hierarchicalPath(conquery.defaultAdminURIBuilder()
															, AdminDatasetResource.class, "postprocessNamespace")
													.buildFromMap(Map.of(DATASET, conquery.getDataset()));

		conquery.getClient().target(matchingStatsUri)
				.request(MediaType.APPLICATION_JSON_TYPE)
				.post(null);

		conquery.waitUntilWorkDone();

		Stream<Concept<?>> allConcepts = conquery.getNamespace().getStorage().getAllConcepts();
		TreeConcept concept = (TreeConcept) allConcepts.iterator().next();
		allConcepts.close();

		final URI resolveUri =
				HierarchyHelper.hierarchicalPath(
									   conquery.defaultApiURIBuilder(),
									   ConceptResource.class, "resolve"
							   )
							   .buildFromMap(
									   Map.of(
											   DATASET, conquery.getDataset(),
											   CONCEPT, concept.getId()
									   )
							   );

		final Response response = conquery.getClient().target(resolveUri)
										  .request(MediaType.APPLICATION_JSON_TYPE)
										  .post(Entity.entity(new ConceptResource.ConceptCodeList(
												  List.of("A1", "unknown")
										  ), MediaType.APPLICATION_JSON_TYPE));


		ResolvedConceptsResult resolved = response.readEntity(ResolvedConceptsResult.class);
		//check the resolved values
		assertThat(resolved).isNotNull();
		assertThat(resolved.getResolvedConcepts().stream().map(Id::toString)).containsExactlyInAnyOrder("ConceptResolutionTest.test_tree.test_child1");
		assertThat(resolved.getUnknownCodes()).containsExactlyInAnyOrder("unknown");

	}
}