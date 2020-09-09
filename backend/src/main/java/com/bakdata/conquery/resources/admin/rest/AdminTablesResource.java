package com.bakdata.conquery.resources.admin.rest;

import static com.bakdata.conquery.resources.ResourceConstants.DATASET;
import static com.bakdata.conquery.resources.ResourceConstants.IMPORT_ID;
import static com.bakdata.conquery.resources.ResourceConstants.TABLE;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import com.bakdata.conquery.apiv1.AdditionalMediaTypes;
import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.resources.hierarchies.HAdmin;
import lombok.Getter;
import lombok.Setter;

@Produces({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@Consumes({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})

@Getter @Setter
@Path("datasets/{" + DATASET + "}/tables/{" + TABLE + "}")
public class AdminTablesResource extends HAdmin {
	
	@PathParam(DATASET)
	protected DatasetId datasetId;
	protected Namespace namespace;
	@PathParam(TABLE)
	protected TableId tableId;
	protected Table table;

	@PostConstruct
	@Override
	public void init() {
		super.init();
		this.namespace = processor.getDatasetRegistry().get(datasetId);
		this.table = namespace.getDataset().getTables().getOrFail(tableId);
		if (this.table == null) {
			throw new WebApplicationException("Could not find table " + tableId, Status.NOT_FOUND);
		}
	}

	@DELETE
	public void remove() {
		processor.deleteTable(tableId);
	}

	@GET
	@Path("/imports")
	@Produces(AdditionalMediaTypes.JSON)
	public List<ImportId> listImports() {
		return namespace.getStorage()
						.getAllImports()
						.stream()
						.filter(imp -> imp.getTable().equals(table.getId()))
						.map(Import::getId)
						.collect(Collectors.toList());
	}

	@DELETE
	@Path("imports/{"+IMPORT_ID+"}")
	public void deleteImportView(@PathParam(IMPORT_ID) ImportId importId) {
		processor.deleteImport(importId);
	}




}