package com.bakdata.conquery.resources.admin.ui;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.bakdata.conquery.resources.admin.rest.UIProcessor;
import com.bakdata.conquery.resources.admin.ui.model.UIView;
import io.dropwizard.views.View;
import lombok.RequiredArgsConstructor;

@Produces(MediaType.TEXT_HTML)
@Path("/")
@RequiredArgsConstructor(onConstructor_=@Inject)
public class AdminUIResource {

	private final UIProcessor uiProcessor;

	@GET
	public View getIndex() {
		return new UIView<>("index.html.ftl", uiProcessor.getUIContext());
	}

	@GET
	@Path("script")
	public View getScript() {
		return new UIView<>("script.html.ftl", uiProcessor.getUIContext());
	}

	@GET
	@Path("jobs")
	public View getJobs() {
		return new UIView<>("jobs.html.ftl", uiProcessor.getUIContext(), uiProcessor.getAdminProcessor().getJobs());
	}

	@GET
	@Path("queries")
	public View getQueries() {
		return new UIView<>("queries.html.ftl", uiProcessor.getUIContext());
	}

}
