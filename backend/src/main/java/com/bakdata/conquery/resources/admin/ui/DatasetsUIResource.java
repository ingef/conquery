package com.bakdata.conquery.resources.admin.ui;

import static com.bakdata.conquery.resources.ResourceConstants.DATASET_NAME;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.identifiable.mapping.PersistentIdMap;
import com.bakdata.conquery.resources.admin.ui.model.FileView;
import com.bakdata.conquery.resources.admin.ui.model.UIView;
import com.bakdata.conquery.resources.hierarchies.HDatasets;
import com.bakdata.conquery.util.io.FileTreeReduction;

import io.dropwizard.views.View;
import lombok.Getter;
import lombok.Setter;

@Produces(MediaType.TEXT_HTML)
@Consumes({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@Getter @Setter
@Path("datasets/{" + DATASET_NAME + "}")
public class DatasetsUIResource extends HDatasets {
	
	@GET
	public View getDataset() {
		return new FileView<>(
			"dataset.html.ftl",
			processor.getUIContext(),
			namespace.getDataset(),
			FileTreeReduction.reduceByExtension(processor.getConfig().getStorage().getPreprocessedRoot(), ".cqpp")
		);
	}
	
	@GET
	@Path("mapping")
	public View getIdMapping() {
		PersistentIdMap mapping = namespace.getStorage().getIdMapping();
		if (mapping != null && mapping.getCsvIdToExternalIdMap() != null) {
			return new UIView<>(
				"idmapping.html.ftl",
				processor.getUIContext(),
				mapping.getCsvIdToExternalIdMap()
			);
		} else {
			return new UIView<>(
				"add_idmapping.html.ftl",
				processor.getUIContext(),
				namespace.getDataset().getId()
			);
		}
	}
}