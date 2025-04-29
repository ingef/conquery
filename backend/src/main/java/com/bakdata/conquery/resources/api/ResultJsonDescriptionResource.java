package com.bakdata.conquery.resources.api;

import static com.bakdata.conquery.io.result.ResultUtil.determineCharset;
import static com.bakdata.conquery.resources.ResourceConstants.QUERY;

import java.net.MalformedURLException;
import java.net.URL;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import com.bakdata.conquery.io.result.json.ResultJsonDescriptionProcessor;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.resources.ResourceConstants;
import io.dropwizard.auth.Auth;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("result/json")
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ResultJsonDescriptionResource {

	private final ResultJsonDescriptionProcessor processor;

	@GET
	@Path("{" + QUERY + "}.json")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAsJson(
			@Auth Subject subject,
			@PathParam(QUERY) ManagedExecution execution,
			@HeaderParam(HttpHeaders.USER_AGENT) String userAgent,
			@QueryParam("charset") String queryCharset
	) {

		if(!subject.isPermitted(execution, Ability.JSON_RESULT)){
			return Response.status(Response.Status.FORBIDDEN).build();
		}

		//TODO handle ManagedForm separately
		if (execution instanceof ManagedQuery managedQuery) {
			log.info("Result for {} download on dataset {} by subject {} ({}).", execution, execution.getDataset(), subject.getId(), subject.getName());

			return processor.createResult(subject, managedQuery, determineCharset(userAgent, queryCharset));
		}

		//TODO response
		return Response.status(Response.Status.BAD_REQUEST).build();
	}

	public static <E extends ManagedQuery> URL getDownloadURL(UriBuilder uriBuilder, E exec) throws MalformedURLException {
		return uriBuilder.path(ResultJsonDescriptionResource.class)
						 .path(ResultJsonDescriptionResource.class, "getAsJson")
						 .resolveTemplate(ResourceConstants.QUERY, exec.getId().toString())
						 .build()
						 .toURL();
	}
}
