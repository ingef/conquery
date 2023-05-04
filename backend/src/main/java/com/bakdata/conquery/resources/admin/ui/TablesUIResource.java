package com.bakdata.conquery.resources.admin.ui;

import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.resources.admin.rest.UIProcessor;
import com.bakdata.conquery.resources.admin.ui.model.UIView;
import io.dropwizard.views.View;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import static com.bakdata.conquery.resources.ResourceConstants.*;

@Produces(MediaType.TEXT_HTML)
@Path("datasets/{" + DATASET + "}/tables/{" + TABLE + "}")
@Getter
@Setter
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class TablesUIResource {

	private final UIProcessor uiProcessor;

	@PathParam(DATASET)
	private Dataset dataset;
	@PathParam(TABLE)
	private Table table;


	@GET
	public View getTableView() {
		return new UIView<>(
				"table.html.ftl",
				uiProcessor.getTableStatistics(table)
		);
	}

	@GET
	@Path("import/{" + IMPORT_ID + "}")
	public View getImportView(@PathParam(IMPORT_ID) Import imp) {

		return new UIView<>(
				"import.html.ftl",
				uiProcessor.getImportStatistics(imp)
		);
	}
}