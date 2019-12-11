package com.bakdata.conquery.resources.hierarchies;

import static com.bakdata.conquery.resources.ResourceConstants.USERS_PATH_ELEMENT;

import javax.inject.Inject;
import javax.ws.rs.Path;

import com.bakdata.conquery.resources.admin.rest.AdminProcessor;

import lombok.Setter;

@Setter
@Path(USERS_PATH_ELEMENT)
public abstract class HUsers extends HAdmin {

	@Inject
	protected AdminProcessor processor;
}
