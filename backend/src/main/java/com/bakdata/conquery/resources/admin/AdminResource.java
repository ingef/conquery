package com.bakdata.conquery.resources.admin;

import static com.bakdata.conquery.resources.ResourceConstants.CONCEPT_NAME;
import static com.bakdata.conquery.resources.ResourceConstants.TABLE_NAME;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.security.PermitAll;
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
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.media.multipart.BodyPart;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.hibernate.validator.constraints.NotEmpty;

import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.jersey.AuthCookie;
import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.concepts.StructureNode;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.exceptions.ConfigurationException;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.identifiable.mapping.CsvEntityId;
import com.bakdata.conquery.models.identifiable.mapping.ExternalEntityId;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.messages.namespaces.specific.UpdateDataset;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.conquery.models.worker.WorkerInformation;
import com.bakdata.conquery.resources.admin.rest.AdminProcessor;
import com.bakdata.conquery.resources.admin.ui.FileView;
import com.bakdata.conquery.resources.admin.ui.TableStatistics;
import com.bakdata.conquery.resources.admin.ui.UIContext;
import com.bakdata.conquery.resources.admin.ui.UIView;
import com.bakdata.conquery.util.io.FileTreeReduction;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.Uninterruptibles;

import io.dropwizard.views.View;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
/*
@Produces({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@Consumes({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@PermitAll
@Slf4j
@Getter
@Path("/")
@AuthCookie
public class AdminResource {

	private final ObjectMapper mapper;
	private final UIContext ctx;
	private final AdminProcessor processor;
	private final Namespaces namespaces;

	public AdminResource(ConqueryConfig config, MasterMetaStorage storage, Namespaces namespaces, JobManager jobManager, ScheduledExecutorService maintenanceService) {
		this.ctx = new UIContext(namespaces);
		this.mapper = namespaces.injectInto(Jackson.MAPPER);
		this.namespaces = namespaces;
		this.processor = new AdminProcessor(config, storage, namespaces, jobManager, maintenanceService);
	}

	

	


	@GET @Produces(MediaType.TEXT_HTML)
	@Path("datasets/{" + DATASET_NAME + "}/tables/{" + TABLE_NAME + "}")
	public View getTable(@PathParam(DATASET_NAME) DatasetId datasetId, @PathParam(TABLE_NAME) TableId tableParam) {
		Namespace ns = namespaces.get(datasetId);
		Dataset dataset = ns.getStorage().getDataset();
		Table table = dataset
			.getTables()
			.getOrFail(tableParam);

		List<Import> imports = ns
			.getStorage()
			.getAllImports()
			.stream()
			.filter(imp -> imp.getTable().equals(table.getId()))
			.collect(Collectors.toList());

		return new UIView<>(
			"table.html.ftl",
			ctx,
			new TableStatistics(
				table,
				imports.stream().mapToLong(Import::getNumberOfBlocks).sum(),
				imports.stream().mapToLong(Import::getNumberOfEntries).sum()
			)
		);
	}

	@GET @Produces(MediaType.TEXT_HTML)
	@Path("datasets/{" + DATASET_NAME + "}/concepts/{" + CONCEPT_NAME + "}")
	public View getConcept(@PathParam(DATASET_NAME) DatasetId datasetId, @PathParam(CONCEPT_NAME) ConceptId conceptParam) {
		Namespace ns = namespaces.get(datasetId);
		Concept<?> concept = ns
			.getStorage()
			.getConcept(conceptParam);

		return new UIView<>(
			"concept.html.ftl",
			ctx,
			concept
		);
	}

	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Path("datasets")
	public Response addDataset(@NotEmpty @FormDataParam("dataset_name") String name) throws JSONException {
		processor.addDataset(name);
		return Response
			.seeOther(UriBuilder.fromPath("/admin/").path(AdminResource.class).build())
			.build();
	}

	

	@DELETE
	@Path("datasets/{" + DATASET_NAME + "}/tables/{" + TABLE_NAME + "}")
	public Response removeTable(@PathParam(DATASET_NAME) DatasetId datasetId, @PathParam(TABLE_NAME) TableId tableParam) throws IOException, JSONException {
		Namespace ns = ctx.getNamespaces().get(datasetId);
		Dataset dataset = ns.getStorage().getDataset();
		dataset.getTables().remove(tableParam);
		ns.getStorage().updateDataset(dataset);
		for (WorkerInformation w : ns.getWorkers()) {
			w.send(new UpdateDataset(dataset));
		}
		return Response.ok().build();
	}

	

	@DELETE
	@Path("datasets/{" + DATASET_NAME + "}/concepts/{" + CONCEPT_NAME + "}")
	public Response removeConcept(@PathParam(DATASET_NAME) DatasetId datasetId, @PathParam(CONCEPT_NAME) ConceptId conceptId) throws IOException, JSONException {
		Namespace ns = ctx.getNamespaces().get(datasetId);
		Dataset dataset = ns.getStorage().getDataset();
		dataset.getConcepts().removeIf(c -> c.getId().equals(conceptId));
		ns.getStorage().updateDataset(dataset);
		for (WorkerInformation w : ns.getWorkers()) {
			w.send(new UpdateDataset(dataset));
		}
		return Response.ok().build();
	}
}*/
