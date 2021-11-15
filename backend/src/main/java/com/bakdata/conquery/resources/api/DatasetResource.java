package com.bakdata.conquery.resources.api;

import static com.bakdata.conquery.resources.ResourceConstants.DATASET;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.apiv1.frontend.FERoot;
import com.bakdata.conquery.resources.hierarchies.HDatasets;
import lombok.Setter;

@Setter
@Produces({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@Consumes({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@Path("datasets/{" + DATASET + "}")
public class DatasetResource extends HDatasets {
	
	@Inject
	protected ConceptsProcessor processor;

	@GET
	@Path("concepts")
	public FERoot getRoot() {
		return processor.getRoot(getNamespace().getStorage(), user);
	}
}
