package com.bakdata.conquery.resources.api;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.resources.hierarchies.HUser;

import lombok.Getter;
import lombok.Setter;

@Produces(MediaType.TEXT_HTML)
@Consumes({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@Getter @Setter
public class UserResource extends HUser{

	public Response deleteUser() {
		processor.deleteUser(addressedUserId);
		return Response.ok().build();
	}
}
