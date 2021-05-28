package com.bakdata.conquery.resources.admin.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.core.Response;

import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.auth.permissions.WildcardPermission;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.resources.hierarchies.HPermissions;

@Consumes(ExtraMimeTypes.JSON_STRING)
public class PermissionResource extends HPermissions {

	
	/**
	 * We let SHIRO parse the permission from a string, instead of letting Jackson map it directly to an object.
	 * One reason is, that Jackson only support one JSON creator at the moment, which is already used for the 
	 * (de)serialization form the store.
	 * The other reason is, that we delegate the permission-string-checking to SHIRO, that gives useful exception messages.
	 */
	@POST
	public Response createPermission(String permission) throws JSONException {
		processor.createPermission(getOwner(), new WildcardPermission(permission));
		return Response.ok().build();
	}
	
	@DELETE
	public Response deletePermission(String permission) throws JSONException {
		processor.deletePermission(getOwner(), new WildcardPermission(permission));
		return Response.ok().build();
	}
}
