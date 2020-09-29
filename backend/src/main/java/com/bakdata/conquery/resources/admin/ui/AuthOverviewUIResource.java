package com.bakdata.conquery.resources.admin.ui;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.bakdata.conquery.resources.admin.ui.model.UIView;
import com.bakdata.conquery.resources.hierarchies.HAuthOverview;
import io.dropwizard.views.View;

@Produces(MediaType.TEXT_HTML)
public class AuthOverviewUIResource extends HAuthOverview {
	
	@GET
	public View getOverview() {
		return new UIView<>("authOverview.html.ftl", processor.getUIContext(), processor.getAuthOverview());
	}

}
