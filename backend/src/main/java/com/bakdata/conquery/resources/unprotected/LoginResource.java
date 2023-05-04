package com.bakdata.conquery.resources.unprotected;

import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.bakdata.conquery.models.config.auth.LocalAuthenticationConfig;
import com.bakdata.conquery.resources.admin.ui.model.UIView;
import io.dropwizard.views.View;

import java.net.URI;

@Path("/login")
@Produces(MediaType.TEXT_HTML)
public class LoginResource {

	@Context
	private ContainerRequestContext request;
	
	@GET
	public View getLoginPage(@QueryParam(LocalAuthenticationConfig.REDIRECT_URI) URI redirectUri){
		final String requestAuthority = request.getUriInfo().getBaseUri().getAuthority();
		final String redirectAuthority = redirectUri.getAuthority();
		if (!requestAuthority.equals(redirectAuthority)){
			throw new BadRequestException(String.format(
					"The authorities of request uri (%s) and the redirect uri (%s) differ",
					requestAuthority,
					redirectAuthority));
		}
		return new UIView<>("login.html.ftl", redirectUri);
	}
}
