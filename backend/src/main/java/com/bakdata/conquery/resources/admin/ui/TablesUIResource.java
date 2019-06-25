package com.bakdata.conquery.resources.admin.ui;

import static com.bakdata.conquery.resources.ResourceConstants.DATASET_NAME;
import static com.bakdata.conquery.resources.ResourceConstants.TABLE_NAME;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.resources.admin.rest.AdminProcessor;

import io.dropwizard.views.View;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Produces(MediaType.TEXT_HTML)
@Consumes({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@PermitAll
@Getter @Setter @Slf4j
@Path("datasets/{" + DATASET_NAME + "}/tables/{" + TABLE_NAME + "}")
public class TablesUIResource {
	
	private AdminProcessor processor;
	private Namespace namespace;
	private Table table;
	
	@Inject
	public TablesUIResource(
		//@Auth User user,
		AdminProcessor processor,
		@PathParam(DATASET_NAME) DatasetId datasetId,
		@PathParam(TABLE_NAME) TableId tableId
	) {
		this.processor = processor;
		this.namespace = processor.getNamespaces().get(datasetId);
		if(namespace == null) {
			throw new WebApplicationException("Could not find dataset "+datasetId, Status.NOT_FOUND);
		}
		//authorize(user, datasetId, Ability.READ);
		this.table = namespace.getStorage().getDataset().getTables().getOptional(tableId)
			.orElseThrow(() -> new WebApplicationException("Could not find table "+tableId, Status.NOT_FOUND));
	}
	
	@GET
	public View getTable() {
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