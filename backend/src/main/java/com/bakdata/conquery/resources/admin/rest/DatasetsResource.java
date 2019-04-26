package com.bakdata.conquery.resources.admin.rest;

import static com.bakdata.conquery.resources.ResourceConstants.DATASET_NAME;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;

import org.eclipse.jetty.server.Response;

import com.bakdata.conquery.io.jersey.AuthCookie;
import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.worker.Namespace;

import io.dropwizard.auth.Auth;
import lombok.Getter;
import lombok.Setter;

@Produces({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@Consumes({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@PermitAll @AuthCookie
@Getter @Setter
@Path("datasets/{" + DATASET_NAME + "}")
public class DatasetsResource {
	
	private final AdminProcessor processor;
	private final Namespace namespace;
	
	@Inject
	public DatasetsResource(
		AdminProcessor processor,
		@Auth User user,
		@PathParam(DATASET_NAME) DatasetId datasetId
	) {
		this.processor = processor;
		this.namespace = processor.getNamespaces().get(datasetId);
		if(namespace == null || user.isPermitted(new DatasetPermission(user, Ability.READ.asSet(), datasetId))) {
			throw new WebApplicationException("Could not find dataset "+datasetId, Response.SC_NOT_FOUND);
		}
	}
}
