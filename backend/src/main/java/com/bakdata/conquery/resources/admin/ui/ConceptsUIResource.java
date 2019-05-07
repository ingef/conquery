package com.bakdata.conquery.resources.admin.ui;

import static com.bakdata.conquery.resources.ResourceConstants.CONCEPT_NAME;
import static com.bakdata.conquery.resources.ResourceConstants.DATASET_NAME;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import com.bakdata.conquery.io.jersey.AuthCookie;
import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.resources.admin.rest.AdminProcessor;

import io.dropwizard.views.View;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Produces(MediaType.TEXT_HTML)
@Consumes({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@PermitAll @AuthCookie
@Getter @Setter @Slf4j
@Path("datasets/{" + DATASET_NAME + "}/concepts/{" + CONCEPT_NAME + "}")
public class ConceptsUIResource {
	
	private AdminProcessor processor;
	private Namespace namespace;
	private Concept<?> concept;
	
	@Inject
	public ConceptsUIResource(
		//@Auth User user,
		AdminProcessor processor,
		@PathParam(DATASET_NAME) DatasetId datasetId,
		@PathParam(CONCEPT_NAME) ConceptId conceptId
	) {
		this.processor = processor;
		this.namespace = processor.getNamespaces().get(datasetId);
		if(namespace == null) {
			throw new WebApplicationException("Could not find dataset "+datasetId, Status.NOT_FOUND);
		}
		//authorize(user, datasetId, Ability.READ);
		this.concept = namespace.getStorage().getConcept(conceptId);
		if(this.concept == null) {
			throw new WebApplicationException("Could not find concept "+conceptId, Status.NOT_FOUND);
		}
	}
	
	@GET
	public View getConcept() {
		return new UIView<>(
			"concept.html.ftl",
			processor.getUIContext(),
			concept
		);
	}
}