package com.bakdata.conquery.resources.api;

import static com.bakdata.conquery.resources.ResourceConstants.DATASET;

import com.bakdata.conquery.apiv1.frontend.FrontendPreviewConfig;
import com.bakdata.conquery.apiv1.frontend.FrontendRoot;
import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;
import com.bakdata.conquery.resources.hierarchies.HDatasets;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Setter
@Produces({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@Consumes({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@Path("datasets/{" + DATASET + "}")
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class DatasetResource extends HDatasets {

	private final ConceptsProcessor processor;

	@GET
	@Path("concepts")
	public FrontendRoot getRoot() {
		return processor.getRoot(getNamespace().getStorage(), subject);
	}

	/**
	 * Provides list of default {@link ConnectorId}s to use for {@link QueryResource.EntityPreview#getSources()}.
	 */
	@GET
	@Path("entity-preview")
	public FrontendPreviewConfig getEntityPreviewDefaultConnectors() {
		return processor.getEntityPreviewFrontendConfig(getDataset());
	}
}
