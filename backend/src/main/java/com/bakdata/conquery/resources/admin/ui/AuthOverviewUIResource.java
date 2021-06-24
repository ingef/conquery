package com.bakdata.conquery.resources.admin.ui;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.bakdata.conquery.resources.admin.rest.AdminProcessor;
import com.bakdata.conquery.resources.admin.rest.UIProcessor;
import com.bakdata.conquery.resources.admin.ui.model.UIView;
import com.bakdata.conquery.resources.hierarchies.HAuthOverview;
import io.dropwizard.views.View;

@Produces(MediaType.TEXT_HTML)
public class AuthOverviewUIResource extends HAuthOverview {

	@Inject
	protected AdminProcessor processor;
	@Inject
	protected UIProcessor uiProcessor;

	@GET
	public View getOverview() {
		return new UIView<>("authOverview.html.ftl", uiProcessor.getUIContext(), processor.getAuthOverview());
	}

}
