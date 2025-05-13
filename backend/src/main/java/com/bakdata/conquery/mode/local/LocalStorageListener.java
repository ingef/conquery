package com.bakdata.conquery.mode.local;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import com.bakdata.conquery.mode.StorageListener;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.LocalNamespace;
import lombok.Data;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Named;

@Data
public class LocalStorageListener implements StorageListener {

	private final DatasetRegistry<LocalNamespace> datasetRegistry;

	@Override
	public void onAddSecondaryId(SecondaryIdDescription secondaryId) {
	}

	@Override
	public void onDeleteSecondaryId(SecondaryIdDescription description) {
	}

	@Override
	public void onAddTable(Table table) {
		LocalNamespace namespace = datasetRegistry.get(table.getDataset());

		DSLContext dslContext = namespace.getDslContextWrapper().getDslContext();
		List<org.jooq.Table<?>> tables = dslContext.meta()
												   .getTables(table.getName());

		if (tables.isEmpty()) {
			throw new NotFoundException("The table %s does not exist.".formatted(table.getId()));
		}

		if (tables.size() > 1) {
			throw new BadRequestException("Found multiple tables matching %s.".formatted(table.getId()));
		}

		org.jooq.Table<?> sqlTable = tables.getFirst();

		Map<String, Field<?>> fieldMap = sqlTable.fieldStream()
												 .collect(Collectors.toMap(Named::getName, Function.identity()));

		List<String> violations = new ArrayList<>();

		List<Column> columns = new ArrayList<>(List.of(table.getColumns()));
		if (table.getPrimaryColumn() != null) {
			columns.add(table.getPrimaryColumn());
		}

		for (Column column : columns) {
			Field<?> field = fieldMap.get(column.getName());

			if (field == null) {
				violations.add("Missing Column %s".formatted(column.getName()));
				continue;
			}

			if (!isTypeCompatible(field, column.getType())) {
				violations.add("%s does not match provided type %s".formatted(column, field.getDataType()));
			}
		}

		if (violations.isEmpty()) {
			return;
		}

		String body = String.join("\n - ", violations);

		throw new BadRequestException("Failed to validate %s:\n%s".formatted(table.getId(), body));
	}

	private static boolean isTypeCompatible(Field<?> field, MajorTypeId type) {
		return switch (type) {
			case STRING -> field.getDataType().isString();
			case INTEGER -> field.getDataType().isInteger();
			case BOOLEAN -> field.getDataType().isBoolean();
			case REAL -> field.getDataType().isFloat();
			case DECIMAL -> field.getDataType().isDecimal();
			case MONEY -> field.getDataType().isDecimal(); //TODO how to verify this properly?
			case DATE -> field.getDataType().isDate();
			case DATE_RANGE -> throw new IllegalStateException("Not implemented for SQL.");
		};
	}

	@Override
	public void onRemoveTable(Table table) {
	}

	@Override
	public void onAddConcept(Concept<?> concept) {
	}

	@Override
	public void onDeleteConcept(ConceptId concept) {
	}
}
