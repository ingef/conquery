package com.bakdata.conquery.resources.admin.rest;

import static com.bakdata.conquery.resources.ResourceConstants.*;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
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
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.resources.hierarchies.HAdmin;

@Path(USERS_PATH_ELEMENT)
public class UserResource extends HAdmin {

	@Inject
	protected AdminProcessor processor;

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Collection<User> getUsers() {
		return processor.getAllUsers();
	}

	@POST
	public Response postUser(@Valid User user) {
		processor.addUser(user);
		return Response.ok().build();
	}

	@POST
	@Path("upload")
	public Response postUsers(@NotEmpty List<User> users) {
		processor.addUsers(users);
		return Response.ok().build();
	}

	@Path("{" + USER_ID + "}")
	@GET
	public Response getUser(@PathParam(USER_ID) User user) {
		return Response.ok(user).build();
	}

	@Path("{" + USER_ID + "}")
	@DELETE
	public Response deleteUser(@PathParam(USER_ID) User user) {
		processor.deleteUser(user);
		return Response.ok().build();
	}

	@Path("{" + USER_ID + "}/" + ROLES_PATH_ELEMENT + "/{" + ROLE_ID + "}")
	@DELETE
	public Response deleteRoleFromUser(@PathParam(USER_ID) User user, @PathParam(ROLE_ID) Role role) {
		processor.deleteRoleFrom(user, role);
		return Response.ok().build();
	}

	@Path("{" + USER_ID + "}/" + ROLES_PATH_ELEMENT + "/{" + ROLE_ID + "}")
	@POST
	public Response addRoleToUser(@PathParam(USER_ID) User user, @PathParam(ROLE_ID) Role role) {
		processor.addRoleTo(user, role);
		return Response.ok().build();
	}
}
