package com.bakdata.conquery.resources.api;

import static com.bakdata.conquery.resources.ResourceConstants.ROLE_NAME;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.bakdata.conquery.models.auth.subjects.Role;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.RoleId;
import com.bakdata.conquery.resources.hierarchies.HRoles;

public class RoleResource extends HRoles{


	@POST
	public Response postRole(Role role) throws JSONException {
		processor.addRole(role);
		return Response.ok().build();
	}
	

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public Response postRoles(List<Role> roles) throws JSONException {
		processor.addRoles(roles);
		return Response.ok().build();
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<Role> getRoles() throws JSONException {
		return processor.getAllRoles();
	}
	
	
	@Path("{" + ROLE_NAME + "}")
	@DELETE
	public Response deleteRole(@PathParam(ROLE_NAME) RoleId roleId) throws JSONException {
		processor.deleteRole(roleId);
		return Response.ok().build();
	}
}
