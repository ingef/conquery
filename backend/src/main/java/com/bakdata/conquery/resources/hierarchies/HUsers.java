package com.bakdata.conquery.resources.hierarchies;

import static com.bakdata.conquery.resources.ResourceConstants.USER_ID;

import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import com.bakdata.conquery.models.identifiable.ids.specific.PermissionOwnerId;
import com.bakdata.conquery.resources.admin.rest.AdminProcessor;

import lombok.Setter;

@Setter
@Path("users")
public class HUsers extends HAuthorized {

	@Inject
	protected AdminProcessor processor;
}
