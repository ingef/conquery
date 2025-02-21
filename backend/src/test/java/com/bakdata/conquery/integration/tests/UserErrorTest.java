package com.bakdata.conquery.integration.tests;

import static com.bakdata.conquery.integration.common.IntegrationUtils.getPostQueryURI;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.Map;

import com.bakdata.conquery.apiv1.query.concept.specific.CQAnd;
import com.bakdata.conquery.integration.IntegrationTest;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.resources.ResourceConstants;
import com.bakdata.conquery.resources.api.ConceptResource;
import com.bakdata.conquery.resources.hierarchies.HierarchyHelper;
import com.bakdata.conquery.util.support.StandaloneSupport;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.apache.http.HttpStatus;

public class UserErrorTest extends IntegrationTest.Simple implements ProgrammaticIntegrationTest {
	@Override
	public void execute(StandaloneSupport conquery) throws Exception {


		{
			final URI postQueryUri = getPostQueryURI(conquery);
			// Send the wrong request body and expect a BAD_REQUEST (400)
			final Invocation.Builder request = conquery.getClient()
													   .target(postQueryUri)
													   .request(MediaType.APPLICATION_JSON_TYPE);
			try (final Response response = request
					.post(Entity.entity(new CQAnd(), MediaType.APPLICATION_JSON_TYPE))) {
				assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
				assertThat(response.readEntity(String.class)).isEqualTo("There is no type AND for [simple type, class com.bakdata.conquery.apiv1.query.QueryDescription]. Try: [ABSOLUTE_FORM_QUERY, ARRAY_CONCEPT_QUERY, CONCEPT_QUERY, ENTITY_DATE_QUERY, ENTITY_PREVIEW, EXPORT_FORM, EXTERNAL_FORM, FULL_EXPORT_FORM, RELATIVE_FORM_QUERY, SECONDARY_ID_QUERY, TABLE_EXPORT, TEST_FORM_ABS_URL, TEST_FORM_REL_URL]");
			}
		}

		{
			// Request a non existed item
			final ConceptId unknownConcept = new ConceptId(conquery.getDataset().getId(), "unknown_concept");
			final UriBuilder getConceptNodeUri = HierarchyHelper.hierarchicalPath(conquery.defaultApiURIBuilder(), ConceptResource.class, "getNode");
			final Invocation.Builder request = conquery.getClient()
													   .target(getConceptNodeUri)
													   .resolveTemplates(
															   Map.of(
																	   ResourceConstants.DATASET, conquery.getDataset().getId().toString(),
																	   ResourceConstants.CONCEPT, unknownConcept.toString()
															   )
													   ).request(MediaType.APPLICATION_JSON_TYPE);
			try (final Response response = request
					.get()) {
				assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_NOT_FOUND);
				assertThat(response.readEntity(HttpError.class)).isEqualTo(new HttpError(404, "Unable to resolve id: UserErrorTest.unknown_concept"));
			}
		}

	}

	private record HttpError(int code, String message) {
	}
}
