package com.bakdata.conquery.resources.admin.rest;

import static com.bakdata.conquery.resources.ResourceConstants.DATASET;
import static com.bakdata.conquery.resources.ResourceConstants.SECONDARY_ID;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.concepts.StructureNode;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.resources.hierarchies.HAdmin;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.media.multipart.FormDataParam;

@Slf4j
@Produces({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@Consumes({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@Getter
@Setter
@Path("datasets/{" + DATASET + "}")
public class AdminDatasetResource extends HAdmin {

	@PathParam(DATASET)
	protected Dataset dataset;
	protected Namespace namespace;

	@PostConstruct
	@Override
	public void init() {
		super.init();
		this.namespace = processor.getDatasetRegistry().get(dataset.getId());
	}

	@POST
	@Consumes(MediaType.WILDCARD)
	@Path("mapping")
	public void setIdMapping(@FormDataParam("data_csv") InputStream data) throws IOException, JSONException {
		processor.setIdMapping(data, namespace);
	}

	@POST
	@Path("label")
	public void setlabel(String label) throws IOException, JSONException {
		Dataset ds = namespace.getDataset();
		ds.setLabel(label);
		namespace.getStorage().updateDataset(ds);
	}

	@POST
	@Path("tables")
	public void addTable(Table table) throws IOException, JSONException {
		processor.addTable(table, namespace);
	}

	@POST
	@Path("imports")
	public void addImport(@QueryParam("file") File selectedFile) throws IOException, JSONException {

		StringJoiner errors = new StringJoiner("\n");

		if (!selectedFile.canRead()) {
			errors.add("Cannot read.");
		}

		if (!selectedFile.exists()) {
			errors.add("Does not exist.");
		}

		if (!selectedFile.isAbsolute()) {
			errors.add("Is not absolute.");
		}

		if (!selectedFile.getPath().endsWith(ConqueryConstants.EXTENSION_PREPROCESSED)) {
			errors.add(String.format("Does not end with `%s`.", ConqueryConstants.EXTENSION_PREPROCESSED));
		}

		if (errors.length() > 0) {
			throw new WebApplicationException(String.format("Invalid file (`%s`) supplied:\n%s.", selectedFile, errors.toString()), Status.BAD_REQUEST);
		}


		processor.addImport(namespace, selectedFile);
	}


	@POST
	@Path("concepts")
	public void addConcept(Concept<?> concept) throws JSONException {
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

	@POST
	@Path("structure")
	public void setStructure(@NotNull @Valid StructureNode[] structure) throws JSONException {
		processor.setStructure(namespace.getDataset(), structure);
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

}
