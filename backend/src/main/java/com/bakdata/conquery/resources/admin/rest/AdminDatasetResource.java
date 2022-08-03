package com.bakdata.conquery.resources.admin.rest;

import static com.bakdata.conquery.resources.ResourceConstants.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.StructureNode;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.identifiable.mapping.EntityIdMap;
import com.bakdata.conquery.models.index.InternToExternMapper;
import com.bakdata.conquery.models.index.search.SearchIndex;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.util.io.FileUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Produces({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@Consumes({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@Getter
@Setter
@Path("datasets/{" + DATASET + "}")
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class AdminDatasetResource {

	private final AdminDatasetProcessor processor;

	@PathParam(DATASET)
	private Dataset dataset;

	private Namespace namespace;

	@PostConstruct
	public void init() {
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
	@Path("secondaryId")
	public void addSecondaryId(SecondaryIdDescription secondaryId) {
		processor.addSecondaryId(namespace, secondaryId);
	}


	@POST
	@Path("internToExtern")
	public void addInternToExternMapping(InternToExternMapper internToExternMapper) {
		processor.addInternToExternMapping(namespace, internToExternMapper);
	}


	@POST
	@Path("searchIndex")
	public void addSearchIndex(SearchIndex searchIndex) {
		processor.addSearchIndex(namespace, searchIndex);
	}

	@POST
	@Path("tables")
	public void addTable(Table table) {
		processor.addTable(table, namespace);
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
			throw new WebApplicationException(String.format("Invalid file (`%s`) supplied.", importFile), err, Status.BAD_REQUEST);
		}
	}

	@POST
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	@Path("cqpp")
	public void uploadImport(@NotNull InputStream importStream) throws IOException {
		log.info("Importing from file upload");
		processor.addImport(namespace, new GZIPInputStream(importStream));
	}

	@POST
	@Path("imports")
	public void addImport(@QueryParam("file") File importFile) throws WebApplicationException, JSONException {
		try {
			processor.addImport(namespace, new GZIPInputStream(FileUtil.cqppFileToInputstream(importFile)));
		}
		catch (IOException err) {
			log.warn("Unable to process import", err);
			throw new WebApplicationException(String.format("Invalid file (`%s`) supplied.", importFile), err, Status.BAD_REQUEST);
		}
	}


	@POST
	@Path("concepts")
	public void addConcept(Concept concept) {
		processor.addConcept(namespace.getDataset(), concept);
	}

	@PUT
	@Path("concepts")
	public void updateConcept(Concept concept) {
		processor.updateConcept(namespace.getDataset(), concept);
	}

	@DELETE
	@Path("secondaryId/{" + SECONDARY_ID + "}")
	public void deleteSecondaryId(@PathParam(SECONDARY_ID) SecondaryIdDescription secondaryId) {
		processor.deleteSecondaryId(secondaryId);
	}

	@DELETE
	@Path("searchIndex/{" + SEARCH_INDEX_ID + "}")
	public void deleteSearchIndex(@PathParam(SEARCH_INDEX_ID) SearchIndex searchIndex, @QueryParam("force") @DefaultValue("false") boolean force) {
		processor.deleteSearchIndex(searchIndex, force);
	}

	@DELETE
	@Path("internToExtern/{" + INTERN_TO_EXTERN_ID + "}")
	public void deleteInternToExternMapping(@PathParam(INTERN_TO_EXTERN_ID) InternToExternMapper internToExternMapper, @QueryParam("force") @DefaultValue("false") boolean force) {
		processor.deleteInternToExternMapping(internToExternMapper, force);
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
	public void updateMatchingStats(@PathParam(DATASET) Dataset dataset) {
		processor.updateMatchingStats(dataset);
	}

	@POST
	@Path("clear-index-cache")
	public void clearIndexCache() {
		processor.clearIndexCache(namespace);
	}

}
