package com.bakdata.conquery.resources.admin.ui;

import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.resources.admin.ui.model.TableStatistics;
import com.bakdata.conquery.resources.admin.ui.model.UIView;
import com.bakdata.conquery.resources.hierarchies.HTables;

import io.dropwizard.views.View;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Produces(MediaType.TEXT_HTML)
@Consumes({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})

@Getter @Setter @Slf4j
public class TablesUIResource extends HTables {
	
	@GET
	public View getTableView() {
		List<Import> imports = namespace
			.getStorage()
			.getAllImports()
			.stream()
			.filter(imp -> imp.getTable().equals(table.getId()))
			.collect(Collectors.toList());

		return new UIView<>(
			"table.html.ftl",
			processor.getUIContext(),
			new TableStatistics(
				table,
				imports.stream().mapToLong(Import::getNumberOfEntries).sum()
			)
		);
	}
}