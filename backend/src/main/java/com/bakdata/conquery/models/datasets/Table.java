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
import com.bakdata.conquery.io.jackson.View;
import com.bakdata.conquery.io.storage.NamespacedStorage;
import com.bakdata.conquery.mode.ValidationMode;
import com.bakdata.conquery.models.config.DatabaseConfig;
import com.bakdata.conquery.models.identifiable.LabeledNamespaceIdentifiable;
import com.bakdata.conquery.models.identifiable.NamespacedStorageProvider;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.util.validation.ValidSqlTable;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.annotations.VisibleForTesting;
import io.dropwizard.validation.ValidationMethod;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Slf4j
@JsonDeserialize(converter = Table.Initializer.class)
@ValidSqlTable(groups = {ValidationMode.Local.class})
@EqualsAndHashCode(callSuper=false)
public class Table extends LabeledNamespaceIdentifiable<TableId> implements Initializing {

	/**
	 * Needed for SQL-Validation
	 */
	@JacksonInject(useInput = OptBoolean.FALSE)
	@JsonIgnore
	@Setter(AccessLevel.PRIVATE)
	@EqualsAndHashCode.Exclude
	private Namespace namespace;

	@JacksonInject(useInput = OptBoolean.FALSE)
	@JsonIgnore
	@Getter(AccessLevel.PRIVATE)
	@EqualsAndHashCode.Exclude
	private NamespacedStorageProvider namespacedStorageProvider;

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

	@JsonView(View.InternalCommunication.class)
	@Setter(value = AccessLevel.PUBLIC, onMethod_ = @VisibleForTesting)
	private DatasetId dataset;


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
		return new TableId(getDataset(), getName());
	}

	public Stream<Import> findImports(NamespacedStorage storage) {
		final TableId thisId = getId();
		return storage.getAllImports()
					  .filter(imp -> imp.getTable().equals(thisId))
					  .map(ImportId::resolve);
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
			if (secondaryId.equals(col.getSecondaryId())) {
				return col;
			}
		}

		return null;
	}


	@Override
	public void init() {

		if (this.dataset == null && namespacedStorageProvider != null) {
			this.dataset = namespacedStorageProvider.getStorage(null).getDataset().getId();
		}

		for (Column column : columns) {
			column.init();
		}
	}

	public static class Initializer extends Converter<Table> {
	}
}
