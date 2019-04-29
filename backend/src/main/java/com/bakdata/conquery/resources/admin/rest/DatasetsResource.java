package com.bakdata.conquery.resources.admin.rest;

import static com.bakdata.conquery.models.auth.AuthorizationHelper.authorize;
import static com.bakdata.conquery.resources.ResourceConstants.DATASET_NAME;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.media.multipart.BodyPart;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataParam;

import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.jersey.AuthCookie;
import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.concepts.StructureNode;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.exceptions.ConfigurationException;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.worker.Namespace;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.dropwizard.auth.Auth;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Produces({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@Consumes({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@PermitAll @AuthCookie
@Getter @Setter @Slf4j
@Path("datasets/{" + DATASET_NAME + "}")
public class DatasetsResource {
	
	private final AdminProcessor processor;
	private final Namespace namespace;
	private final ObjectMapper mapper;
	
	@Inject
	public DatasetsResource(
		AdminProcessor processor,
		//@Auth User user,
		@PathParam(DATASET_NAME) DatasetId datasetId
	) {
		this.processor = processor;
		this.mapper = processor.getNamespaces().injectInto(Jackson.MAPPER);
		this.namespace = processor.getNamespaces().get(datasetId);
		if(namespace == null) {
			throw new WebApplicationException("Could not find dataset "+datasetId, Status.NOT_FOUND);
		}
		//authorize(user, datasetId, Ability.READ);
	}





	@POST
	@Consumes(MediaType.WILDCARD)
	@Path("mapping")
	public void setIdMapping(@FormDataParam("data_csv") InputStream data) throws IOException, JSONException {
		processor.setIdMapping(namespace, data);
	}

	@POST
	@Path("tables")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public void addTable(@FormDataParam("table_schema") FormDataBodyPart schemas) throws IOException, JSONException {
		for (BodyPart part : schemas.getParent().getBodyParts()) {
			try (InputStream is = part.getEntityAs(InputStream.class)) {
				// ContentDisposition meta = part.getContentDisposition();
				Table t = mapper.readValue(is, Table.class);
				processor.addTable(namespace.getDataset(), t);
			}
		}
	}

	@POST
	@Path("imports")
	public void addImport(@QueryParam("file") File file) throws IOException, JSONException {
		File selectedFile = new File(processor.getConfig().getStorage().getPreprocessedRoot(), file.toString());
		if (!selectedFile.exists()) {
			throw new WebApplicationException("Could not find file " + selectedFile, Status.NOT_FOUND);
		}
		processor.addImport(namespace.getStorage().getDataset(), selectedFile);
	}
	
	@POST
	@Path("concepts")
	public void addConcept(Concept<?> concept) throws IOException, JSONException, ConfigurationException {
		processor.addConcept(namespace.getDataset(), concept);
	}
	
	@POST
	@Path("structure")
	public void setStructure(@NotNull@Valid StructureNode[] structure) throws JSONException {
		processor.setStructure(namespace.getDataset(), structure);
	}
}
