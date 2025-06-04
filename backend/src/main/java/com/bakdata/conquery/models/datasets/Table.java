package com.bakdata.conquery.models.datasets;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.bakdata.conquery.io.jackson.Initializing;
import com.bakdata.conquery.io.storage.NamespacedStorage;
import com.bakdata.conquery.mode.ValidationMode;
import com.bakdata.conquery.models.config.DatabaseConfig;
import com.bakdata.conquery.models.identifiable.Labeled;
import com.bakdata.conquery.models.identifiable.ids.NamespacedIdentifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.util.validation.ValidSqlTable;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.dropwizard.validation.ValidationMethod;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Slf4j
@JsonDeserialize(converter = Table.Initializer.class)
@ValidSqlTable(groups = {ValidationMode.Local.class})
public class Table extends Labeled<TableId> implements NamespacedIdentifiable<TableId>, Initializing {

	private DatasetId dataset;

	@JacksonInject(useInput = OptBoolean.FALSE)
	@JsonIgnore
	private Namespace namespace;

	@NotNull
	@Valid
	@JsonManagedReference
	private Column[] columns = new Column[0];
	/**
	 * Defines the primary key/column of this table. Only required for SQL mode.
	 * If unset {@link DatabaseConfig#getPrimaryColumn()} is assumed.
	 */
	@Nullable
	@JsonManagedReference
	private Column primaryColumn;

	@ValidationMethod(message = "More than one column map to the same secondaryId")
	@JsonIgnore
	public boolean isDistinctSecondaryIds() {
		final Set<SecondaryIdDescriptionId> secondaryIds = new HashSet<>();
		for (Column column : columns) {
			final SecondaryIdDescriptionId secondaryId = column.getSecondaryId();
			if (secondaryId != null && !secondaryIds.add(secondaryId)) {
				log.error("{} is duplicated", secondaryId);
				return false;
			}
		}
		return true;
	}

	@ValidationMethod(message = "Column labels must be unique.")
	@JsonIgnore
	public boolean isDistinctLabels() {
		final Set<String> labels = new HashSet<>();

		for (Column column : columns) {
			if (!labels.add(column.getLabel())) {
				log.error("Label `{}` for `{}`  is duplicated", column.getLabel(), column.getId());
				return false;
			}
		}
		return true;
	}


	@Override
	public TableId createId() {
		return new TableId(dataset, getName());
	}

	public Stream<Import> findImports(NamespacedStorage storage) {
		TableId thisId = this.getId();
		return storage.getAllImports().filter(imp -> imp.getTable().equals(thisId));
	}

	public Column getColumnByName(@NotNull String columnName) {
		return Arrays.stream(columns)
					 .filter(column -> column.getName().equals(columnName))
					 .findFirst()
					 .orElseThrow(() -> new IllegalStateException(String.format("Column %s not found", columnName)));
	}

	/**
	 * selects the right column for the given secondaryId from this table
	 */
	@CheckForNull
	public Column findSecondaryIdColumn(SecondaryIdDescriptionId secondaryId) {

		for (Column col : columns) {
			if (col.getSecondaryId() == null || !secondaryId.equals(col.getSecondaryId())) {
				continue;
			}

			return col;
		}

		return null;
	}


	@Override
	public void init() {
		if (dataset == null) {
			dataset = namespace.getDataset().getId();
		}
		else if (namespace != null && !dataset.equals(namespace.getDataset().getId())) {
			throw new IllegalStateException("Datasets don't match. Namespace: %s  Table: %s".formatted(namespace.getDataset().getId(), dataset));
		}

		for (Column column : columns) {
			column.init();
		}
	}

	public static class Initializer extends Converter<Table> {
	}
}
