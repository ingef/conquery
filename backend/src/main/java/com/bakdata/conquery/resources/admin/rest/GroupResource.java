package com.bakdata.conquery.resources.admin.rest;

import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.resources.hierarchies.HGroups;

import javax.inject.Inject;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.List;

import static com.bakdata.conquery.resources.ResourceConstants.*;

public class GroupResource extends HGroups {

	@Inject
	protected AdminProcessor processor;

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Collection<Group> getGroups() {
		return processor.getAllGroups();
	}

	@Path("{" + GROUP_ID + "}")
	@DELETE
	public Response deleteGroup(@PathParam(GROUP_ID) Group group) {
		processor.deleteGroup(group);
		return Response.ok().build();
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public Response postGroups(@NotEmpty List<Group> groups) {
		processor.addGroups(groups);
		return Response.ok().build();
	}

	@Path("{" + GROUP_ID + "}/" + USER_PATH_ELEMENT + "/{" + USER_ID + "}")
	@POST
	public Response addUserToGroup(@PathParam(GROUP_ID) Group group, @PathParam(USER_ID) User user) {
		processor.addUserToGroup(group, user);
		return Response.ok().build();
	}

	@Path("{" + GROUP_ID + "}/" + USER_PATH_ELEMENT + "/{" + USER_ID + "}")
	@DELETE
	public Response deleteUserFromGroup(@PathParam(GROUP_ID) Group group, @PathParam(USER_ID) User user) {
		processor.deleteUserFromGroup(group, user);
		return Response.ok().build();
	}

	@Path("{" + GROUP_ID + "}/" + ROLE_PATH_ELEMENT + "/{" + ROLE_ID + "}")
	@DELETE
	public Response deleteRoleFromUser(@PathParam(GROUP_ID) Group group, @PathParam(ROLE_ID) Role role) {
		processor.deleteRoleFrom(group, role);
		return Response.ok().build();
	}

	@Path("{" + GROUP_ID + "}/" + ROLE_PATH_ELEMENT + "/{" + ROLE_ID + "}")
	@POST
	public Response addRoleToUser(@PathParam(GROUP_ID) Group group, @PathParam(ROLE_ID) Role role) {
		processor.addRoleTo(group, role);
		return Response.ok().build();
	}
}
