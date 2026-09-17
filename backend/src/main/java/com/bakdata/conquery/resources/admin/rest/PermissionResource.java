package com.bakdata.conquery.resources.admin.rest;

import static com.bakdata.conquery.resources.ResourceConstants.OWNER_ID;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import com.bakdata.conquery.models.auth.permissions.WildcardPermission;
import com.bakdata.conquery.models.identifiable.ids.specific.PermissionOwnerId;
import com.bakdata.conquery.util.validation.ValidConqueryPermission;
import lombok.RequiredArgsConstructor;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
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
	public void createPermission(@ValidConqueryPermission String permission) {
		processor.createPermission(owner, new WildcardPermission(permission));
	}

	@DELETE
	public void deletePermission(@ValidConqueryPermission String permission) {
		processor.deletePermission(owner, new WildcardPermission(permission));
	}
}
