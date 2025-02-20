package com.bakdata.conquery.integration.tests;

import static com.bakdata.conquery.integration.common.IntegrationUtils.getPostQueryURI;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import com.bakdata.conquery.apiv1.query.concept.specific.CQAnd;
import com.bakdata.conquery.integration.IntegrationTest;
import com.bakdata.conquery.util.support.StandaloneSupport;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.http.HttpStatus;

public class UserErrorTest extends IntegrationTest.Simple implements ProgrammaticIntegrationTest {
	@Override
	public void execute(StandaloneSupport conquery) throws Exception {

		final URI postQueryURI = getPostQueryURI(conquery);

		{
			// Send the wrong request body and expect a BAD_REQUEST (400)
			final Invocation.Builder request = conquery.getClient()
													   .target(postQueryURI)
													   .request(MediaType.APPLICATION_JSON_TYPE);
			try (final Response response = request
					.post(Entity.entity(new CQAnd(), MediaType.APPLICATION_JSON_TYPE))) {
				assertThat(response.getStatus()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
			}
		}

	}
}
