package com.bakdata.conquery.resources.api;

import static com.bakdata.conquery.resources.ResourceConstants.CONCEPT_NAME;
import static com.bakdata.conquery.resources.ResourceConstants.DATASET_NAME;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.api.description.FEList;
import com.bakdata.conquery.resources.hierarchies.HConcepts;

import lombok.Setter;

@Setter
@Produces({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@Consumes({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@Path("datasets/{" + DATASET_NAME + "}/concepts/{" + CONCEPT_NAME + "}")
public class ConceptResource extends HConcepts {
	
	@Inject
	protected ConceptsProcessor processor;

	@GET
	public Response getNode() {
		FEList result = processor.getNode(concept);
		
		//check if browser still has this version cached
		if (request.getHeader(HttpHeaders.IF_NONE_MATCH) != null 
			&& result.getCacheId().equals(EntityTag.valueOf(request.getHeader(HttpHeaders.IF_NONE_MATCH)))
		) {
			return Response.status(HttpServletResponse.SC_NOT_MODIFIED).build();
		}
		else {
			return Response
				.ok(result)
				.tag(result.getCacheId())
				.build();
		}
	}
}
