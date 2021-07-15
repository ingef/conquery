package com.bakdata.conquery.resources.admin.ui;

import static com.bakdata.conquery.resources.ResourceConstants.DATASET;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.identifiable.mapping.PersistentIdMap;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.resources.admin.rest.AdminDatasetProcessor;
import com.bakdata.conquery.resources.admin.rest.UIProcessor;
import com.bakdata.conquery.resources.admin.ui.model.UIView;
import com.bakdata.conquery.resources.hierarchies.HAdmin;
import io.dropwizard.views.View;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Produces(MediaType.TEXT_HTML)
@Getter
@Setter
@Path("datasets")
@Slf4j
public class DatasetsUIResource {

	@Inject
	private UIProcessor uiProcessor;


	@GET
	@Produces(MediaType.TEXT_HTML)
	public View listDatasetsUI() {
		return new UIView<>("datasets.html.ftl", uiProcessor.getUIContext(), null);
	}


	@GET
	@Path("{" + DATASET + "}")
	public View getDataset(@PathParam(DATASET) Dataset dataset) {
		return new UIView<>(
				"dataset.html.ftl",
				uiProcessor.getUIContext(),
				null
		);
	}



	@GET
	@Path("{" + DATASET + "}/mapping")
	public View getIdMapping(@PathParam(DATASET) Dataset dataset) {
		return new UIView<>("add_idmapping.html.ftl", uiProcessor.getUIContext(), Collections.emptyMap());
	}

	@Data
	public static class TableInfos {
		private final TableId id;
		private final String name;
		private final String label;
		private final String imports;
		private final long entries;
	}

	@Data
	@AllArgsConstructor
	public static class DatasetInfos {

		private Dataset ds;
		private Collection<SecondaryIdDescription> secondaryIds;
		private Collection<TableInfos> tables;
		private Collection<? extends Concept<?>> concepts;
		private long dictionariesSize;
		private long cBlocksSize;
		private long size;
	}
}