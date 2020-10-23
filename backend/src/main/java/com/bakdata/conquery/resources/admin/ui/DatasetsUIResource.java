package com.bakdata.conquery.resources.admin.ui;

import static com.bakdata.conquery.resources.ResourceConstants.DATASET;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.identifiable.mapping.PersistentIdMap;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.bakdata.conquery.models.types.specific.string.StringType;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.resources.admin.ui.model.UIView;
import com.bakdata.conquery.resources.hierarchies.HAdmin;
import io.dropwizard.views.View;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

@Produces(MediaType.TEXT_HTML)
@Consumes({ ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING })
@Getter
@Setter
@Path("datasets/{" + DATASET + "}")
public class DatasetsUIResource extends HAdmin {


	public static final int MAX_IMPORTS_TEXT_LENGTH = 100;
	private static final String ABBREVIATION_MARKER = "\u2026";


	@PathParam(DATASET)
	protected DatasetId datasetId;
	protected Namespace namespace;

	@PostConstruct
	@Override
	public void init() {
		super.init();
		this.namespace = processor.getDatasetRegistry().get(datasetId);
		if (namespace == null) {
			throw new WebApplicationException("Could not find dataset " + datasetId, Status.NOT_FOUND);
		}
	}

	@GET
	public View getDataset() {
		return new UIView<>(
				"dataset.html.ftl",
				processor.getUIContext(),
				new DatasetInfos(
						namespace.getDataset(),
						namespace.getDataset().getTables().stream()
								 .map(table -> new TableInfos(
								 		table.getId(),
										table.getName(),
										table.getLabel(),
										StringUtils.abbreviate(table.findImports(namespace.getStorage()).stream().map(Import::getName).collect(Collectors.joining(", ")), ABBREVIATION_MARKER, MAX_IMPORTS_TEXT_LENGTH),
										table.findImports(namespace.getStorage()).stream().mapToLong(Import::getNumberOfEntries).sum()
								 ))
								 .collect(Collectors.toList()),
						namespace.getStorage().getAllConcepts(),
						// total size of dictionaries
						namespace
								.getStorage()
								.getAllImports()
								.stream()
								.flatMap(i -> Arrays.stream(i.getColumns()))
								.filter(c -> c.getType().getTypeId() == MajorTypeId.STRING)
								.map(c -> (StringType) c.getType())
								.filter(c -> c.getUnderlyingDictionary() != null)
								.collect(Collectors.groupingBy(t -> t.getUnderlyingDictionary().getId()))
								.values()
								.stream()
								.mapToLong(l -> l.get(0).estimateTypeSize())
								.sum(),
						// total size of entries
						namespace.getStorage().getAllImports().stream().mapToLong(Import::estimateMemoryConsumption).sum()
				)
		);
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
		private Collection<TableInfos> tables;
		private Collection<? extends Concept<?>> concepts;
		private long dictionariesSize;
		private long size;
	}

	@GET
	@Path("mapping")
	public View getIdMapping() {
		PersistentIdMap mapping = namespace.getStorage().getIdMapping();
		if (mapping != null && mapping.getCsvIdToExternalIdMap() != null) {
			return new UIView<>("idmapping.html.ftl", processor.getUIContext(), mapping.getCsvIdToExternalIdMap());
		}
		return new UIView<>("add_idmapping.html.ftl", processor.getUIContext(), namespace.getDataset().getId());
	}
}