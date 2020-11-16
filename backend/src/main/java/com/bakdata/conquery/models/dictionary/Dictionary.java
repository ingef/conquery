package com.bakdata.conquery.models.dictionary;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.identifiable.NamedImpl;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;

@JsonTypeInfo(use=JsonTypeInfo.Id.CUSTOM, property="type")
@CPSBase
public abstract class Dictionary extends NamedImpl<DictionaryId> implements Iterable<DictionaryEntry> {

	@Getter @Setter
	private DatasetId dataset;
	
	public Dictionary(@NotNull DatasetId dataset, @NotNull String name) {
		this.setName(name);
		this.dataset = dataset;
	}
	
	@Override
	public DictionaryId createId() {
		return new DictionaryId(dataset, getName());
	}

	public abstract int add(byte[] bytes);
	
	public abstract int put(byte[] bytes);

	public abstract int getId(byte[] bytes);

	public abstract byte[] getElement(int id);

	public abstract int size();

	@Override
	public String toString() {
		return this.getClass().getSimpleName()+"[size=" + size() + "]";
	}

	public static MapDictionary copyUncompressed(Dictionary dict) {
		MapDictionary newDict = new MapDictionary(dict.getId().getDataset(), dict.getId().getDictionary());
		for(DictionaryEntry e:dict) {
			newDict.add(e.getValue());
		}
		return newDict;
	}

	public abstract long estimateMemoryConsumption();

}
