package com.bakdata.conquery.models.types.specific;

import java.util.function.Consumer;

import javax.annotation.Nonnull;

import com.bakdata.conquery.io.xodus.NamespacedStorage;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.bakdata.conquery.models.types.specific.string.StringType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter
@ToString
public abstract class ChainedStringType extends StringType {

	@Nonnull
	protected StringType subType;
	
	public ChainedStringType(StringType subType) {
		super();
		this.subType = ((StringType) subType);
	}

	@Override
	public void loadDictionaries(NamespacedStorage storage) {
		subType.loadDictionaries(storage);
	}
	
	@Override
	public void storeExternalInfos(Consumer<Dictionary> dictionaryConsumer) {
		subType.storeExternalInfos(dictionaryConsumer);
	}

	@Override
	public int size() {
		return subType.size();
	}
	
	@Override
	public long estimateMemoryFieldSize() {
		return subType.estimateMemoryFieldSize();
	}
	
	@Override
	public long estimateMemoryConsumption() {
		return subType.estimateMemoryConsumption();
	}
	
	@Override
	public long estimateTypeSize() {
		return subType.estimateTypeSize();
	}
	
	@Override
	public Dictionary getUnderlyingDictionary() {
		return subType.getUnderlyingDictionary();
	}
	
	@Override
	public void setUnderlyingDictionary(DictionaryId newDict) {
		subType.setUnderlyingDictionary(newDict);
	}

	@Override
	public Integer get(int event) {
		return subType.get(event);
	}

	@Override
	public void set(int event, Integer value) {
		subType.set(event, value);
	}

	@Override
	public boolean has(int event) {
		return subType.has(event);
	}
}
