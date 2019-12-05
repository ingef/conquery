package com.bakdata.conquery.resources.admin.ui;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.bakdata.conquery.resources.admin.ui.model.UIView;
import com.bakdata.conquery.resources.hierarchies.HAdmin;

import io.dropwizard.views.View;

@Path("auth-overview")
@Produces(MediaType.TEXT_HTML)
public class AuthOverviewUIResource extends HAdmin {
	
	@GET
	public View getOverview() {
		return new UIView<>("authOverview.html.ftl", processor.getUIContext(), processor.getAuthOverview());
	}

}
