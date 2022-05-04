package com.bakdata.conquery.models.datasets;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.frontend.FEValue;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.config.CSVConfig;
import com.bakdata.conquery.models.datasets.concepts.Searchable;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.dictionary.MapDictionary;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.events.stores.root.StringStore;
import com.bakdata.conquery.models.identifiable.IdMutex;
import com.bakdata.conquery.models.identifiable.Labeled;
import com.bakdata.conquery.models.identifiable.ids.NamespacedIdentifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Preconditions;
import io.dropwizard.validation.ValidationMethod;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;

@Getter
@Setter
@Slf4j
public class Column extends Labeled<ColumnId> implements NamespacedIdentifiable<ColumnId>, Searchable {

	public static final int UNKNOWN_POSITION = -1;

	@JsonBackReference
	@NotNull
	@ToString.Exclude
	private Table table;
	@NotNull
	private MajorTypeId type;

	private int minSuffixLength = 3;
	private boolean generateSuffixes = true;

	@JsonIgnore
	@Getter(lazy = true)
	private final int position = ArrayUtils.indexOf(getTable().getColumns(), this);
	/**
	 * if set this column should use the given dictionary
	 * if it is of type string, instead of its own dictionary
	 */
	private String sharedDictionary;
	/**
	 * if this is set this column counts as the secondary id of the given name for this
	 * table
	 */
	@NsIdRef
	private SecondaryIdDescription secondaryId;


	@Override
	public ColumnId createId() {
		return new ColumnId(table.getId(), getName());
	}

	//TODO try to remove this method methods, they are quite leaky
	public ColumnStore getTypeFor(Import imp) {
		if (!imp.getTable().equals(getTable())) {
			throw new IllegalArgumentException(String.format("Import %s is not for same table as %s", imp.getTable().getId(), getTable().getId()));
		}

		return Objects.requireNonNull(
				imp.getColumns()[getPosition()].getTypeDescription(),
				() -> String.format("No description for Column/Import %s/%s", getId(), imp.getId())
		);
	}

	@Override
	public String toString() {
		return "Column(id = " + getId() + ", type = " + getType() + ")";
	}

	@ValidationMethod(message = "Only STRING columns can be part of shared Dictionaries.")
	@JsonIgnore
	public boolean isSharedString() {
		return sharedDictionary == null || type.equals(MajorTypeId.STRING);
	}

	@JsonIgnore
	@Override
	public Dataset getDataset() {
		return table.getDataset();
	}

	/**
	 * Creates an id-replacement mapping for shared dictionaries for an {@link Import}.
	 * Because Imports are bound to a {@link com.bakdata.conquery.models.worker.Namespace} but the {@link com.bakdata.conquery.models.preproc.Preprocessed} files are not
	 * they contain dummy-{@link NsIdRef}. These References are mapped to actual object with valid ids through this
	 * generated mapping.
	 * <p>
	 * In this method for shared dictionaries, it is ensured, that the shared dictionary exists in the storage and it is
	 * created if not.
	 *
	 * @param dicts                 The mapping of column names in the Import to dictionaries in the Import
	 * @param storage               The {@link NamespaceStorage} that backs the dictionaries
	 * @param out                   The collection for the generated replacement, that are needed during the deserialization of the next
	 *                              part of the {@link com.bakdata.conquery.models.preproc.Preprocessed}-file
	 * @param sharedDictionaryLocks A collection of locks used for the synchronized creation of shared dictionaries.
	 */
	public void createSharedDictionaryReplacement(Map<String, Dictionary> dicts, NamespaceStorage storage, Map<DictionaryId, Dictionary> out, IdMutex<DictionaryId> sharedDictionaryLocks) {
		Preconditions.checkArgument(type.equals(MajorTypeId.STRING), "Not a STRING Column.");
		Preconditions.checkArgument(sharedDictionary != null, "Can only be used for Shared Dictionary based Columns");
		// If the column is based on a shared dict. We reference a new empty dictionary or the existing one
		// but without updated entries. The entries are updated later on, see ImportJob#applyDictionaryMappings.

		Dictionary sharedDict = null;
		final DictionaryId sharedDictId = new DictionaryId(table.getDataset().getId(), getSharedDictionary());

		try (IdMutex.Locked lock = sharedDictionaryLocks.acquire(sharedDictId)) {
			sharedDict = storage.getDictionary(sharedDictId);
			// Create dictionary if not yet present
			if (sharedDict == null) {
				sharedDict = new MapDictionary(table.getDataset(), getSharedDictionary());
				storage.updateDictionary(sharedDict);
			}
		}
		out.put(new DictionaryId(Dataset.PLACEHOLDER.getId(), dicts.get(getName()).getName()), sharedDict);
	}


	/**
	 * See {@link Column#createSharedDictionaryReplacement(Map, NamespaceStorage, Map, IdMutex)}
	 */
	public void createSingleColumnDictionaryReplacement(Map<String, Dictionary> dicts, String importName, Map<DictionaryId, Dictionary> out) {
		Preconditions.checkArgument(type.equals(MajorTypeId.STRING), "Not a STRING Column.");
		Preconditions.checkArgument(sharedDictionary == null, "Cannot be used for Shared Dictionary based Columns.");

		final Dictionary dict = dicts.get(getName());
		final String name = computeDefaultDictionaryName(importName);

		out.put(new DictionaryId(Dataset.PLACEHOLDER.getId(), dict.getName()), dict);

		dict.setDataset(table.getDataset());
		dict.setName(name);
	}


	private String computeDefaultDictionaryName(String importName) {
		return String.format("%s#%s", importName, getId().toString());
	}

	@Override
	public Stream<FEValue> getSearchValues(CSVConfig config, NamespaceStorage storage) {
		return storage.getAllImports().stream()
					  .flatMap(imp -> StreamSupport.stream(((StringStore) getTypeFor(imp)).spliterator(), false))
					  .map(value -> new FEValue(value, value))
					  .onClose(() -> log.debug("DONE processing values for {}", getId()));
	}

	@Override
	public Searchable getSearchReference() {

		if (getSecondaryId() != null) {
			return getSecondaryId().getSearchReference();
		}

		return this;
	}

}
