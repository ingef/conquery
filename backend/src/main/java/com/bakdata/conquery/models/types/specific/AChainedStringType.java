package com.bakdata.conquery.models.types.specific;

import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import com.bakdata.conquery.io.xodus.NamespacedStorage;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.fasterxml.jackson.core.JsonParser;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter
@ToString
public abstract class AChainedStringType extends AStringType<Number> {

	@Nonnull
	protected AStringType<Number> subType;
	
	public AChainedStringType(AStringType<Number> subType) {
		super(subType.getPrimitiveType());
		this.subType = subType;
	}
	
	@Override
	public void init(DatasetId dataset) {
		subType.init(dataset);
	}
	
	@Override
	public void loadExternalInfos(NamespacedStorage storage) {
		subType.loadExternalInfos(storage);
	}
	
	@Override
	public void storeExternalInfos(NamespacedStorage storage, Consumer<Dictionary> dictionaryConsumer) {
		subType.storeExternalInfos(storage, dictionaryConsumer);
	}
	
	@Override
	public void readHeader(JsonParser input) throws IOException {
		subType.readHeader(input);
	}
	
	@Override
	public void writeHeader(OutputStream out) throws IOException {
		subType.writeHeader(out);
	}
	
	@Override
	public boolean canStoreNull() {
		return subType.canStoreNull();
	}
	
	@Override
	public int size() {
		return subType.size();
	}
	
	@Override
	public long estimateMemoryBitWidth() {
		return subType.estimateMemoryBitWidth();
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
	public void adaptUnderlyingDictionary(Dictionary newDict, VarIntType newNumberType) {
		subType.adaptUnderlyingDictionary(newDict, newNumberType);
		this.setPrimitiveType(newNumberType.getPrimitiveType());
	}
}
