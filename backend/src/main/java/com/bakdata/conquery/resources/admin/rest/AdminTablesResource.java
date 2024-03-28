package com.bakdata.conquery.resources.admin.rest;

import static com.bakdata.conquery.resources.ResourceConstants.*;

import java.util.List;
import java.util.stream.Collectors;

import com.bakdata.conquery.apiv1.AdditionalMediaTypes;
import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.worker.Namespace;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Produces({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@Consumes({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})

@Getter
@Setter
@Path("datasets/{" + DATASET + "}/tables/{" + TABLE + "}")
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class AdminTablesResource {

	private final AdminDatasetProcessor processor;

	@PathParam(DATASET)
	protected Dataset dataset;
	protected Namespace namespace;
	@PathParam(TABLE)
	protected Table table;

	@PostConstruct
	public void init() {
		this.namespace = processor.getDatasetRegistry().get(dataset.getId());
	}

	@GET
	public Table getTable() {
		return table;
	}


	/**
	 * Try to delete a table and all it's imports. Fails if it still has dependencies (unless force is used).
	 *
	 * @param force Force deletion of dependent concepts.
	 * @return List of dependent concepts.
	 */
	@DELETE
	public Response remove(@QueryParam("force") @DefaultValue("false") boolean force) {
		final List<ConceptId> dependents = processor.deleteTable(table, force);

		if (!force && !dependents.isEmpty()) {
			return Response.status(Status.CONFLICT)
						   .entity(dependents)
						   .build();
		}

		return Response.ok()
					   .entity(dependents)
					   .build();
	}

	@GET
	@Path("/imports")
	@Produces(AdditionalMediaTypes.JSON)
	public List<ImportId> listImports() {
		return namespace.getStorage()
						.getAllImports()
						.stream()
						.filter(imp -> imp.getTable().equals(table))
						.map(Import::getId)
						.collect(Collectors.toList());
	}

	@DELETE
	@Path("imports/{" + IMPORT_ID + "}")
	public void deleteImport(@PathParam(IMPORT_ID) Import imp) {
		processor.deleteImport(imp);
	}


	@GET
	@Path("imports/{" + IMPORT_ID + "}")
	public Import getImport(@PathParam(IMPORT_ID) Import imp) {
		return imp;
	}




}