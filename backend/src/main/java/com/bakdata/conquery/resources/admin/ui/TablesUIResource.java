package com.bakdata.conquery.resources.admin.ui;

import static com.bakdata.conquery.resources.ResourceConstants.*;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeNode;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.resources.admin.rest.AdminProcessor;
import com.bakdata.conquery.resources.admin.rest.UIProcessor;
import com.bakdata.conquery.resources.admin.ui.model.ImportStatistics;
import com.bakdata.conquery.resources.admin.ui.model.TableStatistics;
import com.bakdata.conquery.resources.admin.ui.model.UIView;
import com.bakdata.conquery.resources.hierarchies.HAdmin;
import io.dropwizard.views.View;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Produces(MediaType.TEXT_HTML)
@Consumes({ExtraMimeTypes.JSON_STRING, ExtraMimeTypes.SMILE_STRING})
@Path("datasets/{" + DATASET + "}/tables/{" + TABLE + "}")
@Getter
@Setter
@Slf4j
public class TablesUIResource extends HAdmin {

	@PathParam(DATASET)
	protected Dataset dataset;
	protected Namespace namespace;
	@PathParam(TABLE)
	protected Table table;

	@Inject
	protected AdminProcessor processor;
	@Inject
	protected UIProcessor uiProcessor;

	@SneakyThrows({NotFoundException.class})
	@PostConstruct
	@Override
	public void init() {
		super.init();
		this.namespace = processor.getDatasetRegistry().get(dataset.getId());
	}

	@GET
	public View getTableView() {
		List<Import> imports = table.findImports(namespace.getStorage()).collect(Collectors.toList());

		final long entries = imports.stream().mapToLong(Import::getNumberOfEntries).sum();

		return new UIView<>(
				"table.html.ftl",
				uiProcessor.getUIContext(),
				new TableStatistics(
						table,
						entries,
						//total size of dictionaries
						imports.stream()
							   .flatMap(imp -> imp.getDictionaries().stream())
							   .filter(Objects::nonNull)
							   .map(namespace.getStorage()::getDictionary)
							   .mapToLong(Dictionary::estimateMemoryConsumption)
							   .sum(),
						//total size of entries
						imports.stream()
							   .mapToLong(Import::estimateMemoryConsumption)
							   .sum(),
						// Total size of CBlocks
						imports.stream()
							   .mapToLong(imp -> calculateCBlocksSizeBytes(imp, namespace.getStorage().getAllConcepts()))
							   .sum(),
						imports
				)
		);
	}

	public static long calculateCBlocksSizeBytes(Import imp, Collection<? extends Concept<?>> concepts) {

		// CBlocks are created per (per Bucket) Import per Connector targeting this table
		// Since the overhead of a single CBlock is minor, we gloss over the fact, that there are multiple and assume it is only a single very large one.
		return concepts.stream()
					   .filter(TreeConcept.class::isInstance)
					   .flatMap(concept -> ((TreeConcept) concept).getConnectors().stream())
					   .filter(con -> con.getTable().equals(imp.getTable()))
					   .mapToLong(con -> {
						   // Per event an int array is stored marking the path to the concept child.
						   final double avgDepth = con.getConcept()
													  .getAllChildren().stream()
													  .mapToInt(ConceptTreeNode::getDepth)
													  .average()
													  .orElse(1d);

						   return CBlock.estimateMemoryBytes(imp.getNumberOfEntities(), imp.getNumberOfEntries(), avgDepth);
					   })
					   .sum();
	}

	@GET
	@Path("import/{" + IMPORT_ID + "}")
	public View getImportView(@PathParam(IMPORT_ID) Import imp) {
		final long cBlockSize = calculateCBlocksSizeBytes(imp, namespace.getStorage().getAllConcepts());

		return new UIView<>(
				"import.html.ftl",
				uiProcessor.getUIContext(),
				new ImportStatistics(imp, cBlockSize)
		);
	}
}