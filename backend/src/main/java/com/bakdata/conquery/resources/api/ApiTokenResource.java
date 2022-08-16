package com.bakdata.conquery.resources.api;

import com.bakdata.conquery.apiv1.auth.ApiTokenDataRepresentation;
import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.auth.apitoken.ApiToken;
import com.bakdata.conquery.models.auth.apitoken.ApiTokenRealm;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.entities.Subject;
import io.dropwizard.auth.Auth;
import lombok.RequiredArgsConstructor;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints to create and manage scoped {@link ApiToken}s.
 */
@Path("token")
@Consumes(ExtraMimeTypes.JSON_STRING)
@Produces(ExtraMimeTypes.JSON_STRING)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ApiTokenResource {

	public static final String TOKEN = "token";
	private final ApiTokenRealm realm;


	@POST
	public ApiToken createToken(@Auth Subject subject, @Valid ApiTokenDataRepresentation.Request tokenData){

		checkRealUser(subject);

		return realm.createApiToken(subject.getUser(), tokenData);
	}

	@GET
	public List<ApiTokenDataRepresentation.Response> listUserTokens(@Auth Subject subject) {

		checkRealUser(subject);

		return realm.listUserToken(subject);
	}

	@DELETE
	@Path("{" + TOKEN + "}")
	public Response deleteToken(@Auth Subject subject, @PathParam(TOKEN) UUID id) {

		checkRealUser(subject);

		realm.deleteToken(subject, id);

		return Response.ok().build();
	}

	private static void checkRealUser(Subject subject) {
		if (!(subject instanceof User)){
			throw new ForbiddenException("Only real users can request API-tokens");
		}
	}
}
