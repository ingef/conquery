package com.bakdata.conquery.resources.admin.ui;

import static com.bakdata.conquery.resources.ResourceConstants.*;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

import com.bakdata.conquery.models.auth.web.csrf.CsrfTokenSetFilter;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.resources.admin.rest.UIProcessor;
import com.bakdata.conquery.resources.admin.ui.model.UIView;
import io.dropwizard.views.common.View;

@Produces(MediaType.TEXT_HTML)
@Path("datasets/{" + DATASET + "}/tables/{" + TABLE + "}")
public class TablesUIResource {

	private final UIProcessor uiProcessor;
	@Context
	private ContainerRequestContext requestContext;


	@Inject
	public TablesUIResource(UIProcessor uiProcessor) {
		this.uiProcessor = uiProcessor;
	}

	@GET
	public View getTableView(@PathParam(TABLE) TableId tableId) {
		return new UIView("table.html.ftl", uiProcessor.getUIContext(CsrfTokenSetFilter.getCsrfTokenProperty(requestContext)), uiProcessor.getTableStatistics(tableId));
	}

	@GET
	@Path("import/{" + IMPORT_ID + "}")
	public View getImportView(@PathParam(IMPORT_ID) ImportId imp) {

		return new UIView("import.html.ftl", uiProcessor.getUIContext(CsrfTokenSetFilter.getCsrfTokenProperty(requestContext)), uiProcessor.getImportStatistics(imp));
	}
}