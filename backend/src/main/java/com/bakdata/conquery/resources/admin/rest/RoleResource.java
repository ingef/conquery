package com.bakdata.conquery.resources.admin.rest;

import static com.bakdata.conquery.resources.ResourceConstants.ROLES_PATH_ELEMENT;
import static com.bakdata.conquery.resources.ResourceConstants.ROLE_ID;

import java.util.Collection;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.exceptions.JSONException;
import lombok.RequiredArgsConstructor;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path(ROLES_PATH_ELEMENT)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class RoleResource {

	private final AdminProcessor processor;

	@POST
	public Response postRole(Role role) throws JSONException {
		processor.addRole(role);
		return Response.ok().build();
	}

	@GET
	public Collection<Role> getRoles() {
		return processor.getAllRoles();
	}

	@Path("{" + ROLE_ID + "}")
	@GET
	public Response getRole(@PathParam(ROLE_ID) Role role) throws JSONException {
		return Response.ok(role).build();
	}

	@Path("{" + ROLE_ID + "}")
	@DELETE
	public Response deleteRole(@PathParam(ROLE_ID) Role role) throws JSONException {
		processor.deleteRole(role);
		return Response.ok().build();
	}
}
