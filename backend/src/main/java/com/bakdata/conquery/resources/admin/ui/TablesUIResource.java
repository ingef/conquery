package com.bakdata.conquery.resources.admin.ui;

import static com.bakdata.conquery.resources.ResourceConstants.*;

import java.util.List;
import java.util.Objects;

import javax.annotation.PostConstruct;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.bakdata.conquery.io.jersey.ExtraMimeTypes;
import com.bakdata.conquery.models.concepts.tree.ConceptTreeNode;
import com.bakdata.conquery.models.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.resources.admin.ui.model.TableStatistics;
import com.bakdata.conquery.resources.admin.ui.model.UIView;
import com.bakdata.conquery.resources.hierarchies.HAdmin;
import io.dropwizard.util.DataSize;
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
	protected DatasetId datasetId;
	protected Namespace namespace;
	@PathParam(TABLE)
	protected TableId tableId;
	protected Table table;

	@SneakyThrows({NotFoundException.class})
	@PostConstruct
	@Override
	public void init() {
		super.init();
		this.namespace = processor.getDatasetRegistry().get(datasetId);
		this.table = namespace
							 .getStorage()
							 .getTable(tableId);

		if (table == null) {
			throw new NotFoundException("Could not find Table " + tableId.toString());
		}
	}

	@GET
	public View getTableView() {
		List<Import> imports = table.findImports(namespace.getStorage());

		// Estimate CBlock usage
		{
			// We now have all Connectors for this Table
		}


		return new UIView<>(
				"table.html.ftl",
				processor.getUIContext(),
				new TableStatistics(
						table,
						imports.stream().mapToLong(Import::getNumberOfEntries).sum(),
						//total size of dictionaries
						imports
								.stream()
								.flatMap(imp -> imp.getDictionaries().stream())
								.filter(Objects::nonNull)
								.map(namespace.getStorage()::getDictionary)
								.mapToLong(Dictionary::estimateMemoryConsumption)
								.sum(),
						//total size of entries
						imports
								.stream()
								.mapToLong(Import::estimateMemoryConsumption)
								.sum(),
						imports
				)
		);
	}

	@GET
	@Path("import/{" + IMPORT_ID + "}")
	public View getImportView(@PathParam(IMPORT_ID) ImportId importId) {
		Import imp = namespace.getStorage()
							  .getImport(importId);

		final long entries = imp.getNumberOfEntries();

		{
			/* long  + long + int + int
			 *
			 */

			final long cblockSize = namespace.getStorage().getAllConcepts().stream()
											 .filter(TreeConcept.class::isInstance)
											 .flatMap(concept -> ((TreeConcept) concept).getConnectors().stream())
											 .filter(con -> con.getTable().equals(table))
											 .mapToLong(con -> {
												 final double avgDepth = con.getConcept()
																			.getAllChildren().stream()
																			.mapToInt(ConceptTreeNode::getDepth)
																			.average()
																			.orElse(1d);

												 return CBlock.estimateMemoryBytes(1000L, entries, Math.round(avgDepth));
											 })
											 .sum();
			;

			log.info("CBlocks = {}", DataSize.bytes(cblockSize));
		}

		return new UIView<>(
				"import.html.ftl",
				processor.getUIContext(),
				imp
		);
	}
}