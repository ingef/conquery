package com.bakdata.conquery.resources.admin.ui;

import java.net.URI;
import java.util.Objects;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.auth.web.csrf.CsrfTokenSetFilter;
import com.bakdata.conquery.models.config.auth.AuthenticationConfig;
import com.bakdata.conquery.resources.ResourceConstants;
import com.bakdata.conquery.resources.admin.rest.UIProcessor;
import com.bakdata.conquery.resources.admin.ui.model.UIView;
import io.dropwizard.auth.Auth;
import io.dropwizard.views.common.View;
import lombok.RequiredArgsConstructor;

@Produces(MediaType.TEXT_HTML)
@Path("/")
@RequiredArgsConstructor(onConstructor_=@Inject)
public class AdminUIResource {

	private final UIProcessor uiProcessor;
	@Context
	private ContainerRequestContext requestContext;
	@GET
	public View getIndex() {
		return new UIView<>("index.html.ftl", uiProcessor.getUIContext(CsrfTokenSetFilter.getCsrfTokenProperty(requestContext)));
	}

	@GET
	@Path("script")
	public View getScript() {
		return new UIView<>("script.html.ftl", uiProcessor.getUIContext(CsrfTokenSetFilter.getCsrfTokenProperty(requestContext)));
	}

	@GET
	@Path("jobs")
	public View getJobs() {
		return new UIView<>("jobs.html.ftl", uiProcessor.getUIContext(CsrfTokenSetFilter.getCsrfTokenProperty(requestContext)), uiProcessor.getAdminProcessor()
																																		   .getJobs());
	}

	@GET
	@Path("queries")
	public View getQueries() {
		return new UIView<>("queries.html.ftl", uiProcessor.getUIContext(CsrfTokenSetFilter.getCsrfTokenProperty(requestContext)));
	}


	@GET
	@Path("logout")
	public Response logout(@Auth Subject user) {
		// Invalidate all cookies. At the moment the adminEnd uses cookies only for authentication, so this does not interfere with other things
		final NewCookie[] expiredCookies = requestContext.getCookies().keySet().stream().map(AuthenticationConfig::expireCookie).toArray(NewCookie[]::new);
		final URI logout = user.getAuthenticationInfo().getFrontChannelLogout();
		return Response.seeOther(Objects.requireNonNullElseGet(logout, () -> URI.create("/" + ResourceConstants.ADMIN_UI_SERVLET_PATH)))
					   .cookie(expiredCookies)
					   .build();
	}

}
