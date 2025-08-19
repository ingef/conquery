package com.bakdata.conquery.resources.admin.rest;

import static com.bakdata.conquery.resources.ResourceConstants.USER_ID;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import com.bakdata.conquery.apiv1.auth.ProtoUser;
import com.bakdata.conquery.models.auth.basic.UserAuthenticationManagementProcessor;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.resources.hierarchies.HAuthorized;
import lombok.RequiredArgsConstructor;

@Path("local-authentiaction")
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class UserAuthenticationManagementResource extends HAuthorized {

	private final UserAuthenticationManagementProcessor processor;

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public Response addUser(ProtoUser pUser) {

		if (processor.tryRegister(pUser)) {
			return Response.status(Status.CREATED).build();
		}
		return Response.serverError().status(Status.CONFLICT).build();
	}

	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updateUser(ProtoUser pUser) {
		
		if(processor.updateUser(pUser)) {			
			return Response.status(Status.CREATED).build();
		}
		return Response.serverError().status(Status.CONFLICT).build();
	}

	@Path("{" + USER_ID + "}")
	@DELETE
	public void removeUser(@PathParam(USER_ID) UserId user) {
		processor.remove(user);
	}

}
