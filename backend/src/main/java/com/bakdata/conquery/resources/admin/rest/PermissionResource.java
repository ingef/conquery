package com.bakdata.conquery.resources.admin.rest;

import static com.bakdata.conquery.resources.ResourceConstants.OWNER_ID;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.auth.permissions.WildcardPermission;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.PermissionOwnerId;
import lombok.RequiredArgsConstructor;

@Consumes(ExtraMimeTypes.JSON_STRING)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
@Path("permissions/{" + OWNER_ID + "}")
public class PermissionResource {

	private final AdminProcessor processor;

	@PathParam(OWNER_ID)
	private PermissionOwnerId<?> owner;

	/**
	 * We let SHIRO parse the permission from a string, instead of letting Jackson map it directly to an object.
	 * One reason is, that Jackson only support one JSON creator at the moment, which is already used for the
	 * (de)serialization form the store.
	 * The other reason is, that we delegate the permission-string-checking to SHIRO, that gives useful exception messages.
	 */
	@POST
	public void createPermission(String permission) throws JSONException {
		processor.createPermission(owner, new WildcardPermission(permission));
	}
	
	@DELETE
	public void deletePermission(String permission) throws JSONException {
		processor.deletePermission(owner, new WildcardPermission(permission));
	}
}
