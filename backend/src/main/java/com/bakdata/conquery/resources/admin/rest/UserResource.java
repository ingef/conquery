package com.bakdata.conquery.resources.admin.rest;

import static com.bakdata.conquery.resources.ResourceConstants.*;

import java.util.Collection;
import java.util.List;
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

import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.identifiable.ids.specific.RoleId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;

import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.identifiable.ids.specific.RoleId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
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
	public void postUser(@Valid User user) {
		processor.addUser(user);
	}

	@POST
	@Path("upload")
	public void postUsers(@NotEmpty List<User> users) {
		processor.addUsers(users);
	}

	@Path("{" + USER_ID + "}")
	@GET
	public User getUser(@PathParam(USER_ID) UserId user) {
		return user.resolve();
	}

	@Path("{" + USER_ID + "}")
	@DELETE
	public void deleteUser(@PathParam(USER_ID) UserId user) {
		processor.deleteUser(user);
	}

	@Path("{" + USER_ID + "}/" + ROLES_PATH_ELEMENT + "/{" + ROLE_ID + "}")
	@DELETE
	public void deleteRoleFromUser(@PathParam(USER_ID) UserId user, @PathParam(ROLE_ID) RoleId role) {
		processor.deleteRoleFrom(user.resolve(), role);
	}

	@Path("{" + USER_ID + "}/" + ROLES_PATH_ELEMENT + "/{" + ROLE_ID + "}")
	@POST
	public void addRoleToUser(@PathParam(USER_ID) UserId user, @PathParam(ROLE_ID) RoleId role) {
		processor.addRoleTo(user.resolve(), role);
	}
}
