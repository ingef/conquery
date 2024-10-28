package com.bakdata.conquery.resources.unprotected;

import java.net.URI;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

import com.bakdata.conquery.models.auth.web.RedirectingAuthFilter;
import com.bakdata.conquery.resources.admin.ui.model.UIView;
import io.dropwizard.views.common.View;

@Path("/login")
@Produces(MediaType.TEXT_HTML)
public class LoginResource {

	@Context
	private ContainerRequestContext request;
	
	@GET
	public View getLoginPage(@QueryParam(RedirectingAuthFilter.REDIRECT_URI) URI redirectUri) {
		final String requestAuthority = request.getUriInfo().getBaseUri().getAuthority();
		final String redirectAuthority = redirectUri.getAuthority();
		if (!requestAuthority.equals(redirectAuthority)){
			throw new BadRequestException(String.format(
					"The authorities of request uri (%s) and the redirect uri (%s) differ",
					requestAuthority,
					redirectAuthority));
		}
		return new UIView("login.html.ftl", null, redirectUri);
	}
}
