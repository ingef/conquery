package com.bakdata.conquery.resources.admin.ui;

import static com.bakdata.conquery.resources.ResourceConstants.CONCEPT_NAME;
import static com.bakdata.conquery.resources.ResourceConstants.DATASET_NAME;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.messages.namespaces.specific.UpdateDataset;
import com.bakdata.conquery.models.worker.WorkerInformation;
import com.bakdata.conquery.resources.admin.ui.model.UIView;
import com.bakdata.conquery.resources.hierarchies.HConcepts;

import io.dropwizard.views.View;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;

@Produces(MediaType.TEXT_HTML)
@Consumes({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})

@Getter @Setter
@Path("datasets/{" + DATASET_NAME + "}/concepts/{" + CONCEPT_NAME + "}")
public class ConceptsUIResource extends HConcepts {
	
	@GET
	public View getConceptView() {
		return new UIView<>(
			"concept.html.ftl",
			processor.getUIContext(),
			concept
		);
	}

	@DELETE
	public void removeConcept() throws IOException, JSONException {
		namespace.getStorage().removeConcept(conceptId);
		namespace.getStorage().updateDataset(namespace.getDataset());

		for (WorkerInformation w : namespace.getWorkers()) {
			w.send(new UpdateDataset(namespace.getDataset()));
		}
	}
}