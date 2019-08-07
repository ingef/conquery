package com.bakdata.conquery.resources.admin.ui;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.resources.admin.rest.AdminProcessor;
import com.bakdata.conquery.resources.admin.ui.model.UIView;
import com.bakdata.conquery.resources.hierarchies.HAuthorized;

import io.dropwizard.views.View;
import lombok.Getter;
import lombok.Setter;

@Produces(MediaType.TEXT_HTML)
@Consumes({ ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING })
@Getter
@Setter
@Path("users/")
public class UsersUIResource extends HAuthorized {
	@Inject
	protected AdminProcessor processor;
	@GET
	public View getUsers() {
		return new UIView<>("users.html.ftl", processor.getUIContext(), processor.getAllMandators());
	}
}
