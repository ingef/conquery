package com.bakdata.conquery.resources.api;

import static com.bakdata.conquery.resources.ResourceConstants.CONCEPT;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import com.bakdata.conquery.apiv1.frontend.FEList;
import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.resources.hierarchies.HAuthorized;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Produces({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@Consumes({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@Path("concepts/{" + CONCEPT + "}")
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ConceptResource extends HAuthorized {

	private final ConceptsProcessor processor;

	@PathParam(CONCEPT)
	protected Concept<?> concept;

	@GET
	public Response getNode() {
		final FEList result = processor.getNode(concept);

		// check if browser still has this version cached
		if (request.getHeaderString(HttpHeaders.IF_NONE_MATCH) != null && result.getCacheId()
																				.equals(EntityTag.valueOf(request.getHeaderString(HttpHeaders.IF_NONE_MATCH)))) {
			return Response.status(HttpServletResponse.SC_NOT_MODIFIED).build();
		}
		return Response.ok(result).tag(result.getCacheId()).build();

	}

	@POST
	@Path("resolve")
	public ConceptsProcessor.ResolvedConceptsResult resolve(@PathParam(CONCEPT) Concept concept, @NotNull ConceptResource.ConceptCodeList conceptCodes) {
		subject.authorize(concept, Ability.READ);

		final List<String> codes = conceptCodes.getConcepts().stream().map(String::trim).collect(Collectors.toList());

		if (concept instanceof TreeConcept) {
			return processor.resolveConceptElements((TreeConcept) concept, codes);
		}
		throw new WebApplicationException("can only resolved elements on tree concepts", Response.Status.BAD_REQUEST);
	}


	@Data
	public static class ConceptCodeList {
		private final List<String> concepts;
	}
}
