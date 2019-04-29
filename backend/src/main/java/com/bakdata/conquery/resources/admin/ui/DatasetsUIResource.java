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

import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.jersey.AuthCookie;
import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.conquery.resources.admin.rest.AdminProcessor;
import com.bakdata.conquery.util.io.FileTreeReduction;

import io.dropwizard.auth.Auth;
import io.dropwizard.views.View;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Produces(MediaType.TEXT_HTML)
@Consumes({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@PermitAll @AuthCookie
@Getter @Setter @Slf4j
@Path("datasets/{" + DATASET_NAME + "}")
public class DatasetsUIResource {
	
	private AdminProcessor processor;
	private Namespace namespace;
	private UIContext context;
	
	@Inject
	public DatasetsUIResource(
		//@Auth User user,
		AdminProcessor processor,
		@PathParam(DATASET_NAME) DatasetId datasetId
	) {
		this.processor = processor;
		this.context = new UIContext(processor.getNamespaces());
		this.namespace = processor.getNamespaces().get(datasetId);
		if(namespace == null) {
			throw new WebApplicationException("Could not find dataset "+datasetId, Status.NOT_FOUND);
		}
		//authorize(user, datasetId, Ability.READ);
	}
	
	@GET
	public View getDataset(@PathParam(DATASET_NAME) DatasetId dataset) {
		return new FileView<>(
			"dataset.html.ftl",
			context,
			namespace.getDataset(),
			FileTreeReduction.reduceByExtension(processor.getConfig().getStorage().getPreprocessedRoot(), ".cqpp")
		);
	}
}