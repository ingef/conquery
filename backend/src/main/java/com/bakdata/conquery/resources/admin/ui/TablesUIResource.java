package com.bakdata.conquery.resources.admin.ui;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.bakdata.conquery.models.types.specific.AStringType;
import com.bakdata.conquery.resources.admin.ui.model.TableStatistics;
import com.bakdata.conquery.resources.admin.ui.model.UIView;
import com.bakdata.conquery.resources.hierarchies.HTables;

import io.dropwizard.views.View;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import static com.bakdata.conquery.resources.ResourceConstants.IMPORT_ID;

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
				imports.stream().mapToLong(Import::getNumberOfEntries).sum(),
				//total size of dictionaries
				imports
					.stream()
					.flatMap(i->Arrays.stream(i.getColumns()))
					.filter(c->c.getType().getTypeId()==MajorTypeId.STRING)
					.map(c->(AStringType)c.getType())
					.filter(c->c.getUnderlyingDictionary() != null)
					.collect(Collectors.groupingBy(t->t.getUnderlyingDictionary().getId()))
					.values()
					.stream()
					.mapToLong(l->l.get(0).estimateTypeSize())
					.sum(),
				//total size of entries
				imports
					.stream()
					.mapToLong(Import::estimateMemoryConsumption)
					.sum(),
					namespace
					.getStorage()
					.getAllImports()
					.stream()
					.filter(imp -> imp.getTable().equals(table.getId()))
					.map(imp -> imp.getName())
					.collect(Collectors.toList())
			)
		);
	}
	
	@GET
	@Path("import/{"+IMPORT_ID+"}")
	public View getImportView(@PathParam(IMPORT_ID)ImportId importId) {
		Import imp = namespace
			.getStorage()
			.getImport(importId);

		return new UIView<>(
			"import.html.ftl",
			processor.getUIContext(),
			imp
		);
	}
}