package com.bakdata.conquery.resources.admin.rest;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.apiv1.AdditionalMediaTypes;
import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.resources.hierarchies.HAdmin;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import static com.bakdata.conquery.resources.ResourceConstants.*;

import com.bakdata.conquery.util.io.*;

@Produces({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@Consumes({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})

@Getter
@Setter
@Path("datasets/{" + DATASET + "}/tables/{" + TABLE + "}")
public class AdminTablesResource extends HAdmin {

	@Inject
	private AdminDatasetProcessor processor;

	@PathParam(DATASET)
	protected Dataset dataset;
	protected Namespace namespace;
	@PathParam(TABLE)
	protected Table table;

	@PostConstruct
	@Override
	public void init() {
		super.init();
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


	@PUT
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	@Path("cqpp")
	public void updateCqppImport(@NotNull InputStream importStream) throws IOException {
		processor.updateImport(namespace, new GZIPInputStream(importStream));
	}

	@PUT
	@Path("imports")
	public void updateImport(@NotNull @QueryParam("file") File importFile) throws WebApplicationException {
		try {
			processor.updateImport(namespace, new GZIPInputStream(FileUtil.cqppFileToInputstream(importFile)));
		}
		catch (IOException err) {
			throw new WebApplicationException(String.format("Invalid file (`%s`) supplied:\n%s.", importFile, err.getMessage()), Status.BAD_REQUEST);
		}
	}

}