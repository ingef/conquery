package com.bakdata.conquery.resources.admin.ui;

import static com.bakdata.conquery.models.auth.AuthorizationHelper.authorize;
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

import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.mapping.PersistentIdMap;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.resources.admin.rest.AdminProcessor;
import com.bakdata.conquery.util.io.FileTreeReduction;

import io.dropwizard.auth.Auth;
import io.dropwizard.views.View;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Produces(MediaType.TEXT_HTML)
@Consumes({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})

@Getter @Setter @Slf4j
@Path("datasets/{" + DATASET_NAME + "}")
public class DatasetsUIResource {
	
	private AdminProcessor processor;
	private Namespace namespace;
	
	@Inject
	public DatasetsUIResource(
		@Auth User user,
		AdminProcessor processor,
		@PathParam(DATASET_NAME) DatasetId datasetId
	) {
		this.processor = processor;
		this.namespace = processor.getNamespaces().get(datasetId);
		if(namespace == null) {
			throw new WebApplicationException("Could not find dataset "+datasetId, Status.NOT_FOUND);
		}
		authorize(user, datasetId, Ability.READ);
	}
	
	@GET
	public View getDataset() {
		return new FileView<>(
			"dataset.html.ftl",
			processor.getUIContext(),
			namespace.getDataset(),
			FileTreeReduction.reduceByExtension(processor.getConfig().getStorage().getPreprocessedRoot(), ".cqpp")
		);
	}
	
	@GET
	@Path("mapping")
	public View getIdMapping() {
		PersistentIdMap mapping = namespace.getStorage().getIdMapping();
		if (mapping != null && mapping.getCsvIdToExternalIdMap() != null) {
			return new UIView<>(
				"idmapping.html.ftl",
				processor.getUIContext(),
				mapping.getCsvIdToExternalIdMap()
			);
		} else {
			return new UIView<>(
				"add_idmapping.html.ftl",
				processor.getUIContext(),
				namespace.getDataset().getId()
			);
		}
	}
}