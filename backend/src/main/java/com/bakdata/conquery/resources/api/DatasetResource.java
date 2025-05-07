package com.bakdata.conquery.resources.api;

import static com.bakdata.conquery.resources.ResourceConstants.DATASET;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

import com.bakdata.conquery.apiv1.frontend.FrontendPreviewConfig;
import com.bakdata.conquery.apiv1.frontend.FrontendRoot;
import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.identifiable.ids.specific.ConnectorId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.resources.hierarchies.HAuthorized;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Setter
@Produces({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@Consumes({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@Path("datasets/{" + DATASET + "}")
@RequiredArgsConstructor(onConstructor_ = {@Inject})
@Getter
public class DatasetResource extends HAuthorized {

	private final ConceptsProcessor processor;

	@Inject
	private DatasetRegistry<?> datasetRegistry;

	@PathParam(DATASET)
	private DatasetId dataset;
	private Namespace namespace;

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

	@PostConstruct
	@Override
	public void init() {
		super.init();
		this.namespace = datasetRegistry.get(dataset);
		subject.authorize(dataset, Ability.READ);
	}
}
