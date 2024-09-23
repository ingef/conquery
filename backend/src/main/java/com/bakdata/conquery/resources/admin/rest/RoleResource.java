package com.bakdata.conquery.resources.admin.rest;

import static com.bakdata.conquery.resources.ResourceConstants.ROLES_PATH_ELEMENT;
import static com.bakdata.conquery.resources.ResourceConstants.ROLE_ID;

import java.util.Collection;

import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.exceptions.JSONException;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
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
