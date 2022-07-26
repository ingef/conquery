package com.bakdata.conquery.resources.admin.rest;

import static com.bakdata.conquery.resources.ResourceConstants.*;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
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

import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.User;
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
	public Response deleteGroup(@PathParam(GROUP_ID) Group group) {
		processor.deleteGroup(group);
		return Response.ok().build();
	}

	@POST
	public Response postGroups(@NotEmpty List<Group> groups) {
		processor.addGroups(groups);
		return Response.ok().build();
	}

	@Path("{" + GROUP_ID + "}/" + USERS_PATH_ELEMENT + "/{" + USER_ID + "}")
	@POST
	public Response addUserToGroup(@PathParam(GROUP_ID) Group group, @PathParam(USER_ID) User user) {
		processor.addUserToGroup(group, user);
		return Response.ok().build();
	}

	@Path("{" + GROUP_ID + "}/" + USERS_PATH_ELEMENT + "/{" + USER_ID + "}")
	@DELETE
	public Response deleteUserFromGroup(@PathParam(GROUP_ID) Group group, @PathParam(USER_ID) User user) {
		processor.deleteUserFromGroup(group, user);
		return Response.ok().build();
	}

	@Path("{" + GROUP_ID + "}/" + ROLES_PATH_ELEMENT + "/{" + ROLE_ID + "}")
	@DELETE
	public Response deleteRoleFromUser(@PathParam(GROUP_ID) Group group, @PathParam(ROLE_ID) Role role) {
		processor.deleteRoleFrom(group, role);
		return Response.ok().build();
	}

	@Path("{" + GROUP_ID + "}/" + ROLES_PATH_ELEMENT + "/{" + ROLE_ID + "}")
	@POST
	public Response addRoleToUser(@PathParam(GROUP_ID) Group group, @PathParam(ROLE_ID) Role role) {
		processor.addRoleTo(group, role);
		return Response.ok().build();
	}
}
