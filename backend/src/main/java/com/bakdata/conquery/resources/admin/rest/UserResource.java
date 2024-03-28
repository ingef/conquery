package com.bakdata.conquery.resources.admin.rest;

import static com.bakdata.conquery.resources.ResourceConstants.*;

import java.util.Collection;
import java.util.List;

import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.User;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;

@Path(USERS_PATH_ELEMENT)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class UserResource {

	protected final AdminProcessor processor;

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
