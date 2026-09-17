package com.bakdata.conquery.resources.api;

import java.util.stream.Stream;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import com.bakdata.conquery.apiv1.IdLabel;
import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.resources.hierarchies.HAuthorized;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Setter
@Produces({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@Consumes({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@Path("/datasets")
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class DatasetsResource extends HAuthorized {

	private final ConceptsProcessor processor;

	@GET
	public Stream<IdLabel<DatasetId>> getDatasets() {
		return processor.getDatasets(subject);
	}
}
