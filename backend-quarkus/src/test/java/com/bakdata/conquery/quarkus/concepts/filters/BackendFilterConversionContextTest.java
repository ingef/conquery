package com.bakdata.conquery.quarkus.concepts.filters;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import com.bakdata.conquery.quarkus.ids.ColumnId;
import com.bakdata.conquery.quarkus.ids.ConceptId;
import com.bakdata.conquery.quarkus.ids.ConnectorId;
import com.bakdata.conquery.quarkus.ids.DatasetId;
import com.bakdata.conquery.quarkus.ids.TableId;
import com.bakdata.conquery.quarkus.plugin.api.datasets.ColumnDescriptor;
import com.bakdata.conquery.quarkus.plugin.api.datasets.ColumnType;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import org.junit.jupiter.api.Test;

class BackendFilterConversionContextTest {

	@Test
	void preservesDateRangeColumnTypeForPlugins() {
		DatasetId datasetId = new DatasetId("demo");
		TableId tableId = new TableId(datasetId, "events");
		ColumnId columnId = new ColumnId(tableId, "validity");
		DatasetCatalogRepository.TableRecord table = new DatasetCatalogRepository.TableRecord(
				tableId,
				"Events",
				List.of(new DatasetCatalogRepository.ColumnRecord(columnId, "Validity", ColumnType.DATE_RANGE, null)),
				columnId
		);
		ConnectorId connectorId = new ConnectorId(new ConceptId(datasetId, List.of("events")), "events");
		BackendFilterConversionContext context = new BackendFilterConversionContext(
				connectorId,
				tableId,
				table,
				(idType, fallbackContext, fallbackValue, sanitized) -> { }
		);

		assertEquals(new ColumnDescriptor("validity", ColumnType.DATE_RANGE), context.requireColumn("validity"));
	}
}
