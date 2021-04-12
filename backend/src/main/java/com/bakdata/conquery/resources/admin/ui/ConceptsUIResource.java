package com.bakdata.conquery.resources.admin.ui;

import static com.bakdata.conquery.resources.ResourceConstants.CONCEPT;
import static com.bakdata.conquery.resources.ResourceConstants.DATASET;

import javax.annotation.PostConstruct;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.resources.admin.ui.model.UIView;
import com.bakdata.conquery.resources.hierarchies.HAdmin;
import com.bakdata.conquery.util.ResourceUtil;
import io.dropwizard.views.View;
import lombok.Getter;
import lombok.Setter;

@Produces(MediaType.TEXT_HTML)
@Consumes({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})

@Getter
@Setter
@Path("datasets/{" + DATASET + "}/concepts/{" + CONCEPT + "}")
public class ConceptsUIResource extends HAdmin {

	@PathParam(CONCEPT)
	protected ConceptId conceptId;
	protected Concept<?> concept;
	@PathParam(DATASET)
	protected DatasetId datasetId;
	protected Namespace namespace;

	@PostConstruct
	@Override
	public void init() {
		super.init();
		this.namespace = processor.getDatasetRegistry().get(datasetId);

		ResourceUtil.throwNotFoundIfNull(datasetId, namespace);

		this.concept = namespace.getStorage().getConcept(conceptId);

		ResourceUtil.throwNotFoundIfNull(conceptId, concept);
	}

	@GET
	public View getConceptView() {
		return new UIView<>(
				"concept.html.ftl",
				processor.getUIContext(),
				concept
		);
	}
}