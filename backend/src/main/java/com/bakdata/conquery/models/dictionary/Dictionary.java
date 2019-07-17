package com.bakdata.conquery.models.dictionary;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.identifiable.NamedImpl;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JsonTypeInfo(use=JsonTypeInfo.Id.CUSTOM, property="type")
@CPSBase
@NoArgsConstructor
public abstract class Dictionary extends NamedImpl<DictionaryId> implements Iterable<DictionaryEntry> {

	@Getter @Setter
	private DatasetId dataset = new DatasetId("null");
	
	public Dictionary(DictionaryId dictionaryId) {
		this.setName(dictionaryId.getDictionary());
		this.dataset = dictionaryId.getDataset();
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
		MapDictionary newDict = new MapDictionary(dict.getId());
		for(DictionaryEntry e:dict) {
			newDict.add(e.getValue());
		}
		return newDict;
	}

	public abstract long estimateMemoryConsumption();

}
