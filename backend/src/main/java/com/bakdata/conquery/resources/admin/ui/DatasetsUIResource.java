package com.bakdata.conquery.resources.admin.ui;

import static com.bakdata.conquery.resources.ResourceConstants.DATASET;

import java.util.Collection;
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
@Consumes({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@Getter
@Setter
@Path("datasets/{" + DATASET + "}")
@Slf4j
public class DatasetsUIResource extends HAdmin {

	@Inject
	private AdminDatasetProcessor processor;
	@Inject
	private UIProcessor uiProcessor;

	public static final int MAX_IMPORTS_TEXT_LENGTH = 100;
	private static final String ABBREVIATION_MARKER = "\u2026";


	@PathParam(DATASET)
	protected Dataset dataset;
	protected Namespace namespace;

	@PostConstruct
	@Override
	public void init() {
		super.init();
		this.namespace = processor.getDatasetRegistry().get(dataset.getId());
	}

	@GET
	public View getDataset() {
		return new UIView<>(
				"dataset.html.ftl",
				uiProcessor.getUIContext(),
				new DatasetInfos(
						namespace.getDataset(),
						namespace.getStorage().getSecondaryIds(),
						namespace.getStorage().getTables().stream()
								 .map(table -> new TableInfos(
										 table.getId(),
										 table.getName(),
										 table.getLabel(),
										 StringUtils.abbreviate(table.findImports(namespace.getStorage())
																	 .map(Import::getName)
																	 .collect(Collectors.joining(", ")), ABBREVIATION_MARKER, MAX_IMPORTS_TEXT_LENGTH),
										 table.findImports(namespace.getStorage()).mapToLong(Import::getNumberOfEntries).sum()
								 ))
								 .collect(Collectors.toList()),
						namespace.getStorage().getAllConcepts(),
						// total size of dictionaries
						namespace
								.getStorage()
								.getAllImports()
								.stream()
								.flatMap(i -> i.getDictionaries().stream())
								.filter(Objects::nonNull)
								.map(namespace.getStorage()::getDictionary)
								.distinct()
								.mapToLong(Dictionary::estimateMemoryConsumption)
								.sum(),
						// Total size of CBlocks
						namespace
								.getStorage().getTables()
								.stream()
								.flatMap(table -> table.findImports(namespace.getStorage()))
								.mapToLong(imp -> TablesUIResource.calculateCBlocksSizeBytes(
										imp, getNamespace().getStorage().getAllConcepts()
								))
								.sum(),
						// total size of entries
						namespace.getStorage().getAllImports().stream().mapToLong(Import::estimateMemoryConsumption).sum()
				)
		);
	}

	@GET
	@Path("mapping")
	public View getIdMapping() {
		PersistentIdMap mapping = namespace.getStorage().getIdMapping();
		if (mapping != null && mapping.getCsvIdToExternalIdMap() != null) {
			return new UIView<>("idmapping.html.ftl", uiProcessor.getUIContext(), mapping.getCsvIdToExternalIdMap());
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
		private Collection<TableInfos> tables;
		private Collection<? extends Concept<?>> concepts;
		private long dictionariesSize;
		private long cBlocksSize;
		private long size;
	}
}