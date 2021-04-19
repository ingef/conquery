package com.bakdata.conquery.resources.admin.rest;

import static com.bakdata.conquery.resources.ResourceConstants.*;

import java.util.Collection;
import java.util.List;

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

import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.RoleId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.resources.hierarchies.HUsers;

public class UserResource extends HUsers {

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Collection<User> getUsers() {
		return processor.getAllUsers();
	}

	@POST
	public Response postUser(User user) throws JSONException {
		processor.addUser(user);
		return Response.ok().build();
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public Response postUsers(@NotEmpty List<User> users) {
		processor.addUsers(users);
		return Response.ok().build();
	}

	@Path("{" + USER_ID + "}")
	@DELETE
	public Response deleteUser(@PathParam(USER_ID) UserId userId) {
		processor.deleteUser(userId);
		return Response.ok().build();
	}

	@Path("{" + USER_ID + "}/" + ROLE_PATH_ELEMENT + "/{" + ROLE_ID + "}")
	@DELETE
	public Response deleteRoleFromUser(@PathParam(USER_ID) UserId userId, @PathParam(ROLE_ID) RoleId roleId) {
		processor.deleteRoleFrom(userId, roleId);
		return Response.ok().build();
	}

	@Path("{" + USER_ID + "}/" + ROLE_PATH_ELEMENT + "/{" + ROLE_ID + "}")
	@POST
	public Response addRoleToUser(@PathParam(USER_ID) UserId userId, @PathParam(ROLE_ID) RoleId roleId) {
		processor.addRoleTo(userId, roleId);
		return Response.ok().build();
	}
}
