package com.bakdata.conquery.resources.api;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.core.Response;

import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.resources.hierarchies.HPermissions;

@Consumes({ExtraMimeTypes.JSON_STRING})
public class PermissionResource extends HPermissions{

	
	@POST
	public Response createPermission(ConqueryPermission permission) throws JSONException {
		processor.createPermission(ownerId, permission);
		return Response.ok().build();
	}
	
	@DELETE
	public Response deletePermission(ConqueryPermission permission) throws JSONException {
		processor.deletePermission(ownerId, permission);
		return Response.ok().build();
	}
}
