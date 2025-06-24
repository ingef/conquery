package com.bakdata.conquery.resources.api;

import static com.bakdata.conquery.resources.ResourceConstants.CONCEPT;

import java.util.List;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import com.bakdata.conquery.apiv1.frontend.FrontendList;
import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.resources.hierarchies.HAuthorized;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Produces({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@Consumes({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@Path("concepts/{" + CONCEPT + "}")
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class ConceptResource extends HAuthorized {

	private final ConceptsProcessor processor;

	@GET
	public Response getNode(@PathParam(CONCEPT) ConceptId concept) {
		subject.authorize(concept.getDataset(), Ability.READ);
		subject.authorize(concept, Ability.READ);

		final FrontendList result = processor.getNode(concept.resolve());

		// check if browser still has this version cached
		if (request.getHeaderString(HttpHeaders.IF_NONE_MATCH) != null && result.getCacheId()
																				.equals(EntityTag.valueOf(request.getHeaderString(HttpHeaders.IF_NONE_MATCH)))) {
			return Response.status(HttpServletResponse.SC_NOT_MODIFIED).build();
		}
		return Response.ok(result).tag(result.getCacheId()).build();

	}

	@POST
	@Path("resolve")
	public ConceptsProcessor.ResolvedConceptsResult resolve(@PathParam(CONCEPT) ConceptId conceptId, ConceptResource.ConceptCodeList conceptCodes) {
		final Concept<?> concept = conceptId.resolve();

		subject.authorize(concept.getDataset(), Ability.READ);
		subject.authorize(concept, Ability.READ);

		final List<String> codes = conceptCodes.getConcepts().stream().map(String::trim).collect(Collectors.toList());

		if (concept instanceof TreeConcept treeConcept && treeConcept.countElements() > 1) {
			return processor.resolveConceptElements((TreeConcept) concept, codes);
		}
		throw new WebApplicationException("can only resolved elements on tree concepts", Response.Status.BAD_REQUEST);
	}


	@Data
	@RequiredArgsConstructor(onConstructor_ = @JsonCreator)
	public static class ConceptCodeList {
		private final List<String> concepts;
	}
}
