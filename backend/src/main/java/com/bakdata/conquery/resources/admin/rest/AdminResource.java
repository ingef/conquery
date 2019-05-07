package com.bakdata.conquery.resources.admin.rest;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.media.multipart.FormDataParam;
import org.hibernate.validator.constraints.NotEmpty;

import com.bakdata.conquery.io.jersey.AuthCookie;
import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.exceptions.JSONException;

import lombok.Getter;
import lombok.Setter;

@Produces({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@Consumes({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@PermitAll @AuthCookie
@Path("/")
@Getter @Setter
public class AdminResource {

	@Inject
	private AdminProcessor processor;
	
	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Path("datasets")
	public Response addDataset(@NotEmpty @FormDataParam("dataset_name") String name) throws JSONException {
		processor.addDataset(name);
		return Response
			.seeOther(UriBuilder.fromPath("/admin/").path(AdminResource.class).build())
			.build();
	}
}
