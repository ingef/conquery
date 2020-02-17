package com.bakdata.conquery.resources.hierarchies;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response.Status;

import com.bakdata.conquery.models.auth.entities.User;
import com.codahale.metrics.MetricRegistry;
import io.dropwizard.auth.Auth;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class HAuthorized {

	@Auth
	protected User user;
	@Context
	protected HttpServletRequest request;

	@Inject
	protected MetricRegistry metricRegistry;

	@PostConstruct
	public void init() {
		if(user == null) {
			throw new WebApplicationException(Status.UNAUTHORIZED);
		}
	}
}
