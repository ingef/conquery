package com.bakdata.conquery.resources.api;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import com.bakdata.conquery.apiv1.IdLabel;
import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.resources.hierarchies.HAuthorized;
import lombok.Setter;

@Setter
@Produces({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@Consumes({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@Path("/")
public class APIResource extends HAuthorized {
	
	@Inject
	protected ConceptsProcessor processor;
	
	@GET
	@Path("datasets")
	public List<IdLabel<DatasetId>> getDatasets() {
		return processor.getDatasets(user);
	}
}
