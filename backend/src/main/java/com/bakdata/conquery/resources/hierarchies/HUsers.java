package com.bakdata.conquery.resources.hierarchies;

import static com.bakdata.conquery.resources.ResourceConstants.USERS_PATH_ELEMENT;

import javax.ws.rs.Path;

import lombok.Setter;

@Setter
@Path(USERS_PATH_ELEMENT)
public abstract class HUsers extends HAdmin {

}
