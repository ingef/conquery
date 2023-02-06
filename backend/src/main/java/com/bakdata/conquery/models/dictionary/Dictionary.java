package com.bakdata.conquery.models.dictionary;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.identifiable.NamedImpl;
import com.bakdata.conquery.models.identifiable.ids.NamespacedIdentifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
@CPSBase
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
public abstract class Dictionary extends NamedImpl<DictionaryId> implements NamespacedIdentifiable<DictionaryId>, Iterable<DictionaryEntry> {

	@Getter
	@Setter
	@NsIdRef
	private Dataset dataset;

	public Dictionary(Dataset dataset, @NotNull String name) {
		this.setName(name);
		this.dataset = dataset;
	}

	@Override
	public DictionaryId createId() {
		return new DictionaryId(dataset.getId(), getName());
	}

	public abstract int add(byte[] bytes);

	public abstract int put(byte[] bytes);

	public abstract int getId(byte[] bytes);

	public abstract byte[] getElement(int id);

	@ToString.Include
	public abstract int size();

	public static MapDictionary copyUncompressed(Dictionary dict) {
		MapDictionary newDict = new MapDictionary(dict.getDataset(), dict.getName());
		for (DictionaryEntry e : dict) {
			newDict.add(e.getValue());
		}
		return newDict;
	}

	public abstract long estimateMemoryConsumption();

}
