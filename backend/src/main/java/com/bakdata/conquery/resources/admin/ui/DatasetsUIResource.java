package com.bakdata.conquery.resources.admin.ui;

import static com.bakdata.conquery.resources.ResourceConstants.DATASET;
import static com.bakdata.conquery.resources.admin.rest.UIProcessor.calculateCBlocksSizeBytes;

import java.util.Collection;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.identifiable.mapping.EntityIdMap;
import com.bakdata.conquery.models.index.InternToExternMapper;
import com.bakdata.conquery.models.index.search.SearchIndex;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.resources.admin.rest.UIProcessor;
import com.bakdata.conquery.resources.admin.ui.model.UIView;
import io.dropwizard.views.common.View;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Produces(MediaType.TEXT_HTML)
@Getter
@Setter
@Path("datasets")
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DatasetsUIResource {

	public static final int MAX_IMPORTS_TEXT_LENGTH = 100;
	private static final String ABBREVIATION_MARKER = "\u2026";

	private final UIProcessor uiProcessor;


	@GET
	@Produces(MediaType.TEXT_HTML)
	public View listDatasetsUI() {
		return new UIView<>(
				"datasets.html.ftl",
				uiProcessor.getUIContext(),
				uiProcessor.getDatasetRegistry().getAllDatasets()
		);
	}


	@GET
	@Path("{" + DATASET + "}")
	public View getDataset(@PathParam(DATASET) Dataset dataset) {
		final Namespace namespace = uiProcessor.getDatasetRegistry().get(dataset.getId());
		return new UIView<>(
				"dataset.html.ftl",
				uiProcessor.getUIContext(),
				new DatasetInfos(
						namespace.getDataset(),
						namespace.getStorage().getSecondaryIds(),
						namespace.getStorage().getInternToExternMappers(),
						namespace.getStorage().getSearchIndices(),
						namespace.getStorage().getTables().stream()
								 .map(table -> new TableInfos(
										 table.getId(),
										 table.getName(),
										 table.getLabel(),
										 StringUtils.abbreviate(table.findImports(namespace.getStorage())
																	 .map(Import::getName)
																	 .sorted()
																	 .collect(Collectors.joining(", ")), ABBREVIATION_MARKER, MAX_IMPORTS_TEXT_LENGTH),
										 table.findImports(namespace.getStorage()).mapToLong(Import::getNumberOfEntries).sum()
								 ))
								 .collect(Collectors.toList()),
						namespace.getStorage().getAllConcepts(),
						// Total size of CBlocks
						namespace
								.getStorage().getTables()
								.stream()
								.flatMap(table -> table.findImports(namespace.getStorage()))
								.mapToLong(imp -> calculateCBlocksSizeBytes(
										imp, namespace.getStorage().getAllConcepts()
								))
								.sum(),
						// total size of entries
						namespace.getStorage().getAllImports().stream().mapToLong(Import::estimateMemoryConsumption).sum()
				)
		);
	}


	@GET
	@Path("{" + DATASET + "}/mapping")
	public View getIdMapping(@PathParam(DATASET) Dataset dataset) {
		final Namespace namespace = uiProcessor.getDatasetRegistry().get(dataset.getId());
		EntityIdMap mapping = namespace.getStorage().getIdMapping();
		if (mapping != null && mapping.getInternalToPrint() != null) {
			return new UIView<>("idmapping.html.ftl", uiProcessor.getUIContext(), mapping.getInternalToPrint());
		}
		return new UIView<>("add_idmapping.html.ftl", uiProcessor.getUIContext(), namespace.getDataset().getId());
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
		private Collection<InternToExternMapper> internToExternMappers;
		private Collection<SearchIndex> searchIndices;
		private Collection<TableInfos> tables;
		private Collection<? extends Concept<?>> concepts;
		private long cBlocksSize;
		private long size;
	}
}