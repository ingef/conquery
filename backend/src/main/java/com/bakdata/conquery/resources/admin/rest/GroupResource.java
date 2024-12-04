package com.bakdata.conquery.resources.admin.rest;

import static com.bakdata.conquery.resources.ResourceConstants.*;

import java.util.Collection;
import java.util.List;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotEmpty;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.identifiable.ids.specific.RoleId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import lombok.RequiredArgsConstructor;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path(GROUPS_PATH_ELEMENT)
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class GroupResource {

	private final AdminProcessor processor;

	@GET
	public Collection<Group> getGroups() {
		return processor.getAllGroups();
	}


	@Path("{" + GROUP_ID + "}")
	@GET
	public Response getGroup(@PathParam(GROUP_ID) Group group) {
		return Response.ok(group).build();
	}

	@Path("{" + GROUP_ID + "}")
	@DELETE
	public void deleteGroup(@PathParam(GROUP_ID) Group group) {
		processor.deleteGroup(group);
	}

	@POST
	public void postGroups(@NotEmpty List<Group> groups) {
		processor.addGroups(groups);
	}

	@Path("{" + GROUP_ID + "}/" + USERS_PATH_ELEMENT + "/{" + USER_ID + "}")
	@POST
	public void addUserToGroup(@PathParam(GROUP_ID) Group group, @PathParam(USER_ID) User user) {
		processor.addUserToGroup(group, user);
	}

	@Path("{" + GROUP_ID + "}/" + USERS_PATH_ELEMENT + "/{" + USER_ID + "}")
	@DELETE
	public void deleteUserFromGroup(@PathParam(GROUP_ID) Group group, @PathParam(USER_ID) UserId user) {
		processor.deleteUserFromGroup(group, user);
	}

	@Path("{" + GROUP_ID + "}/" + ROLES_PATH_ELEMENT + "/{" + ROLE_ID + "}")
	@DELETE
	public void deleteRoleFromUser(@PathParam(GROUP_ID) Group group, @PathParam(ROLE_ID) RoleId role) {
		processor.deleteRoleFrom(group, role);
	}

	@Path("{" + GROUP_ID + "}/" + ROLES_PATH_ELEMENT + "/{" + ROLE_ID + "}")
	@POST
	public void addRoleToUser(@PathParam(GROUP_ID) Group group, @PathParam(ROLE_ID) Role role) {
		processor.addRoleTo(group, role);
	}
}
