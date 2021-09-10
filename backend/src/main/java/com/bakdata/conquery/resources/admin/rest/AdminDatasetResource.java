package com.bakdata.conquery.resources.admin.rest;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.StructureNode;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.identifiable.mapping.EntityIdMap;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.resources.hierarchies.HAdmin;
import io.dropwizard.auth.Auth;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
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

@Slf4j
@Produces({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@Consumes({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@Getter
@Setter
@Path("datasets/{" + DATASET + "}")
public class AdminDatasetResource extends HAdmin {


	@Inject
	protected AdminDatasetProcessor processor;


	@PathParam(DATASET)
	protected Dataset dataset;
	protected Namespace namespace;

	@PostConstruct
	@Override
	public void init() {
		super.init();
		this.namespace = processor.getDatasetRegistry().get(dataset.getId());
	}

	@GET
	@Consumes(MediaType.WILDCARD)
	@Path("mapping")
	public EntityIdMap getIdMapping() {
		return processor.getIdMapping(namespace);
	}

	@POST
	@Consumes(MediaType.WILDCARD)
	@Path("mapping")
	public void setIdMapping(InputStream data) {
		processor.setIdMapping(data, namespace);
	}

	@POST
	@Path("label")
	public void setLabel(String label) {
		Dataset ds = namespace.getDataset();
		ds.setLabel(label);
		namespace.getStorage().updateDataset(ds);
	}

	@POST
	@Path("weight")
	public void setWeight(@Min(0) int weight) {
		Dataset ds = namespace.getDataset();
		ds.setWeight(weight);
		namespace.getStorage().updateDataset(ds);
	}

	@POST
	@Path("tables")
	public void addTable(Table table) {
		processor.addTable(table, namespace);
	}


	@POST
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	@Path("cqpp")
	public void uploadImport(@NotNull InputStream importStream) throws IOException {
		log.info("Importing from file upload");
		processor.addImport(namespace, new GZIPInputStream(importStream));
	}

	@PUT
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	@Path("imports/{" + IMPORT_ID + "}")
	public void updateImport(@NotNull @PathParam(IMPORT_ID) Import imp, @NotNull InputStream importStream) throws IOException {

		log.info("Importing from file upload");
		processor.updateImport(imp, namespace, new GZIPInputStream(importStream));

	}


	@POST
	@Path("imports")
	public void addImport(@QueryParam("file") File importFile) throws IOException, JSONException {

		StringJoiner errors = new StringJoiner("\n");

		if (!importFile.canRead()) {
			errors.add("Cannot read.");
		}

		if (!importFile.exists()) {
			errors.add("Does not exist.");
		}

		if (!importFile.isAbsolute()) {
			errors.add("Is not absolute.");
		}

		if (!importFile.getPath().endsWith(ConqueryConstants.EXTENSION_PREPROCESSED)) {
			errors.add(String.format("Does not end with `%s`.", ConqueryConstants.EXTENSION_PREPROCESSED));
		}

		if (errors.length() > 0) {
			throw new WebApplicationException(String.format("Invalid file (`%s`) supplied:\n%s.", importFile, errors.toString()), Status.BAD_REQUEST);
		}


		log.info("Importing from local file {}", importFile.getAbsolutePath());
		processor.addImport(namespace, new GZIPInputStream(new FileInputStream(importFile)));
	}


	@POST
	@Path("concepts")
	public void addConcept(Concept<?> concept) {
		processor.addConcept(namespace.getDataset(), concept);
	}

	@POST
	@Path("secondaryId")
	public void addSecondaryId(SecondaryIdDescription secondaryId) {
		processor.addSecondaryId(namespace, secondaryId);
	}

	@DELETE
	@Path("secondaryId/{" + SECONDARY_ID + "}")
	public void deleteSecondaryId(@PathParam(SECONDARY_ID) SecondaryIdDescription secondaryId) {
		processor.deleteSecondaryId(secondaryId);
	}

	@GET
	public Dataset getDatasetInfos() {
		return dataset;
	}

	@POST
	@Path("structure")
	public void setStructure(@NotNull @Valid StructureNode[] structure) {
		processor.setStructure(namespace, structure);
	}


	@GET
	@Path("tables")
	public List<TableId> listTables() {
		return namespace.getStorage().getTables().stream().map(Table::getId).collect(Collectors.toList());
	}

	@GET
	@Path("concepts")
	public List<ConceptId> listConcepts() {
		return namespace.getStorage().getAllConcepts().stream().map(Concept::getId).collect(Collectors.toList());
	}

	@DELETE
	public void delete() {
		processor.deleteDataset(dataset);
	}

	@POST
	@Path("/update-matching-stats")
	@Consumes(MediaType.WILDCARD)
	public void updateMatchingStats(@Auth User user, @PathParam(DATASET) Dataset dataset) {
		processor.updateMatchingStats(dataset);
	}

}
