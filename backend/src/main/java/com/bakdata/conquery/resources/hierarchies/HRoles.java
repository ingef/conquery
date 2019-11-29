package com.bakdata.conquery.resources.hierarchies;


import javax.inject.Inject;
import javax.ws.rs.Path;

import com.bakdata.conquery.resources.admin.rest.AdminProcessor;

import lombok.Setter;

@Setter
@Path("roles")
public abstract class HRoles extends HAdmin{

	@Inject
	protected AdminProcessor processor;
}
