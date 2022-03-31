package com.bakdata.conquery.models.dictionary;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.identifiable.NamedImpl;
import com.bakdata.conquery.models.identifiable.ids.NamespacedIdentifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;

@JsonTypeInfo(use=JsonTypeInfo.Id.CUSTOM, property="type")
@CPSBase
public abstract class Dictionary extends NamedImpl<DictionaryId> implements NamespacedIdentifiable<DictionaryId>, Iterable<DictionaryEntry> {

	@Getter @Setter
	@NsIdRef
	private Dataset dataset;

	@Getter
	private final Encoding encoding;
	
	public Dictionary(Dataset dataset, String name, Encoding encoding) {
		this.encoding = encoding;
		this.setName(name);
		this.dataset = dataset;
	}
	
	@Override
	public DictionaryId createId() {
		return new DictionaryId(dataset.getId(), getName());
	}

	public abstract int add(String bytes);
	
	public abstract int put(String bytes);

	public abstract int getId(String bytes);

	public abstract String getElement(int id);

	public abstract int size();

	protected String decode(byte[] elements) {
		return getEncoding().decode(elements);
	}

	protected byte[] encode(String value) {
		return getEncoding().encode(value);
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName()+"[size=" + size() + "]";
	}

	public static MapDictionary copyUncompressed(Dictionary dict) {
		MapDictionary newDict = new MapDictionary(dict.getDataset(), dict.getName(), dict.getEncoding());
		for(DictionaryEntry e:dict) {
			newDict.add(e.getValue());
		}
		return newDict;
	}

	public abstract long estimateMemoryConsumption();

}
