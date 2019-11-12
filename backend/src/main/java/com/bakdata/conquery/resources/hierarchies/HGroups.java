package com.bakdata.conquery.resources.hierarchies;

import javax.inject.Inject;
import javax.ws.rs.Path;

import com.bakdata.conquery.resources.admin.rest.AdminProcessor;

import lombok.Setter;

@Setter
@Path("groups")
public class HGroups extends HAuthorized {

	@Inject
	protected AdminProcessor processor;
}
