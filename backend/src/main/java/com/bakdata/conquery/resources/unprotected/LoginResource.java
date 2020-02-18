package com.bakdata.conquery.resources.unprotected;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.bakdata.conquery.resources.admin.ui.model.UIView;
import io.dropwizard.views.View;

@Path("/login")
@Produces(MediaType.TEXT_HTML)
public class LoginResource {
	
	@GET
	public View getLoginPage(){
		return new UIView<>("login.html.ftl", null);
	}
}
