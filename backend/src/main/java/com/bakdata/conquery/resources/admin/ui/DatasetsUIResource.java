package com.bakdata.conquery.resources.admin.ui;

import static com.bakdata.conquery.resources.ResourceConstants.DATASET;
import static com.bakdata.conquery.resources.admin.rest.UIProcessor.calculateCBlocksSizeBytes;

import java.util.Collection;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

import com.bakdata.conquery.models.auth.web.csrf.CsrfTokenSetFilter;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.identifiable.mapping.EntityIdMap;
import com.bakdata.conquery.models.index.InternToExternMapper;
import com.bakdata.conquery.models.index.search.SearchIndex;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.resources.admin.rest.UIProcessor;
import com.bakdata.conquery.resources.admin.ui.model.UIContext;
import com.bakdata.conquery.resources.admin.ui.model.UIView;
import io.dropwizard.views.common.View;
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

	@Context
	private ContainerRequestContext requestContext;


	@GET
	@Produces(MediaType.TEXT_HTML)
	public View listDatasetsUI() {
		return new UIView<>(
				"datasets.html.ftl",
				uiProcessor.getUIContext(CsrfTokenSetFilter.getCsrfTokenProperty(requestContext)),
				uiProcessor.getDatasetRegistry().getAllDatasets().map(DatasetId::resolve).toList()
		);
	}


	@GET
	@Path("{" + DATASET + "}")
	public View getDataset(@PathParam(DATASET) DatasetId dataset) {
		final Namespace namespace = uiProcessor.getDatasetRegistry().get(dataset);
		return new UIView<>(
				"dataset.html.ftl",
				uiProcessor.getUIContext(CsrfTokenSetFilter.getCsrfTokenProperty(requestContext)),
				new DatasetInfos(
						namespace.getDataset(),
						namespace.getStorage().getSecondaryIds().toList(),
						namespace.getStorage().getInternToExternMappers().toList(),
						namespace.getStorage().getSearchIndices().toList(),
						namespace.getStorage().getTables()
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
						namespace.getStorage().getAllConcepts().toList(),
						// Total size of CBlocks
						namespace
								.getStorage().getTables()
								.flatMap(table -> table.findImports(namespace.getStorage()))
								.mapToLong(imp -> calculateCBlocksSizeBytes(
										imp, namespace.getStorage().getAllConcepts()
								))
								.sum(),
						// total size of entries
						namespace.getStorage().getAllImports().map(Id::resolve).mapToLong(Import::estimateMemoryConsumption).sum()
				)
		);
	}


	@GET
	@Path("{" + DATASET + "}/mapping")
	public View getIdMapping(@PathParam(DATASET) DatasetId dataset) {
		final Namespace namespace = uiProcessor.getDatasetRegistry().get(dataset);
		final EntityIdMap mapping = namespace.getStorage().getIdMapping();
		final UIContext uiContext = uiProcessor.getUIContext(CsrfTokenSetFilter.getCsrfTokenProperty(requestContext));

		if (mapping != null && mapping.getInternalToPrint() != null) {
			return new UIView<>("idmapping.html.ftl", uiContext, mapping.getInternalToPrint());
		}
		return new UIView<>("add_idmapping.html.ftl", uiContext, namespace.getDataset().getId());
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