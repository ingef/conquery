package com.bakdata.conquery.resources.admin.rest;

import static com.bakdata.conquery.resources.ResourceConstants.*;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.PreviewConfig;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.StructureNode;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.InternToExternMapperId;
import com.bakdata.conquery.models.identifiable.ids.specific.SearchIndexId;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.identifiable.mapping.EntityIdMap;
import com.bakdata.conquery.models.index.InternToExternMapper;
import com.bakdata.conquery.models.index.search.SearchIndex;
import com.bakdata.conquery.models.worker.Namespace;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
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
	private DatasetId dataset;

	private Namespace namespace;

	@PostConstruct
	public void init() {
		namespace = processor.getDatasetRegistry().get(dataset);
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
		final Dataset ds = namespace.getDataset();
		ds.setLabel(label);
		namespace.getStorage().updateDataset(ds);
	}

	@POST
	@Path("weight")
	public void setWeight(@Min(0) int weight) {
		final Dataset ds = namespace.getDataset();
		ds.setWeight(weight);
		namespace.getStorage().updateDataset(ds);
	}

	@POST
	@Path("secondaryId")
	public void addSecondaryId(SecondaryIdDescription secondaryId) {
		processor.addSecondaryId(namespace, secondaryId);
	}

	@POST
	@Path("preview")
	public void setPreviewConfig(PreviewConfig previewConfig) {
		processor.setPreviewConfig(previewConfig, namespace);
	}

	@DELETE
	@Path("preview")
	public void deletePreviewConfig() {
		processor.deletePreviewConfig(namespace);
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
		processor.updateImport(namespace, new GZIPInputStream(new BufferedInputStream(importStream)));
	}

	@POST
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	@Path("cqpp")
	@SneakyThrows
	public void uploadImport(@NotNull InputStream importStream) {
		log.debug("Importing from file upload");
		processor.addImport(namespace, new GZIPInputStream(new BufferedInputStream(importStream)));
	}

	@POST
	@Path("concepts")
	public void addConcept(@QueryParam("force") @DefaultValue("false") boolean force, Concept<?> concept) {
		processor.addConcept(namespace, concept, force);
	}

	@PUT
	@Path("concepts")
	public void updateConcept(Concept<?> concept) {
		processor.updateConcept(namespace, concept);
	}

	@DELETE
	@Path("secondaryId/{" + SECONDARY_ID + "}")
	public void deleteSecondaryId(@PathParam(SECONDARY_ID) SecondaryIdDescriptionId secondaryId) {
		processor.deleteSecondaryId(secondaryId);
	}

	@DELETE
	@Path("searchIndex/{" + SEARCH_INDEX_ID + "}")
	public List<ConceptId> deleteSearchIndex(@PathParam(SEARCH_INDEX_ID) SearchIndexId searchIndex, @QueryParam("force") @DefaultValue("false") boolean force) {

		final List<ConceptId> conceptIds = processor.deleteSearchIndex(searchIndex, force);
		if (!conceptIds.isEmpty() && !force) {
			throw new BadRequestException(String.format("Cannot delete search index because it is used by these concepts: %s", conceptIds));
		}
		return conceptIds;
	}

	@DELETE
	@Path("internToExtern/{" + INTERN_TO_EXTERN_ID + "}")
	public List<ConceptId> deleteInternToExternMapping(
			@PathParam(INTERN_TO_EXTERN_ID) InternToExternMapperId internToExternMapper,
			@QueryParam("force") @DefaultValue("false") boolean force) {
		return processor.deleteInternToExternMapping(internToExternMapper, force);
	}

	@GET
	public Dataset getDatasetInfos() {
		return dataset.resolve();
	}

	@POST
	@Path("structure")
	public void setStructure(@NotNull @Valid StructureNode[] structure) {
		processor.setStructure(namespace, structure);
	}


	@GET
	@Path("tables")
	public List<TableId> listTables() {
		return namespace.getStorage().getTables().map(Table::getId).collect(Collectors.toList());
	}

	@GET
	@Path("concepts")
	public List<ConceptId> listConcepts() {
		return namespace.getStorage()
						.getAllConcepts()
						.map(Concept::getId)
						.collect(Collectors.toList());
	}

	@DELETE
	public void delete() {
		processor.deleteDataset(dataset);
	}

	/**
	 * @param dataset the namespace to postprocess
	 * @implNote The path mapping is historical named. Renaming the path requires some coordination.
	 */
	@POST
	@Path("/update-matching-stats")
	@Consumes(MediaType.WILDCARD)
	public void postprocessNamespace(@PathParam(DATASET) DatasetId dataset) {
		processor.postprocessNamespace(dataset);
	}

	@POST
	@Path("clear-index-cache")
	public void clearIndexCache() {
		processor.clearIndexCache();
	}

}
