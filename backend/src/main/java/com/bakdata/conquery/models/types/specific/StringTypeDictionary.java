package com.bakdata.conquery.models.types.specific;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.xodus.NamespacedStorage;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.dictionary.DictionaryEntry;
import com.bakdata.conquery.models.dictionary.MapDictionary;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.CTypeVarInt;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonParser;

import jersey.repackaged.com.google.common.collect.Iterators;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter @Setter @Slf4j
@CPSType(base = CType.class, id = "STRING_DICTIONARY")
public class StringTypeDictionary extends CTypeVarInt<Integer> {

	@NotNull @Nonnull
	private DictionaryId dictionaryId = new DictionaryId(new DatasetId("null"), UUID.randomUUID().toString());
	@JsonIgnore
	private transient Dictionary dictionary = new MapDictionary(dictionaryId);
	
	@JsonCreator
	public StringTypeDictionary(VarIntType numberType) {
		super(MajorTypeId.STRING, numberType);
	}
	
	@Override
	public void init(DatasetId dataset) {
		dictionaryId = new DictionaryId(dataset, dictionaryId.getDictionary());
		dictionary.setDataset(dataset);
	}

	@Override
	public byte[] createScriptValue(Number value) {
		return getElement(value);
	}
	
	public byte[] getElement(int value) {
		return dictionary.getElement(value);
	}
	
	public byte[] getElement(Number value) {
		return getElement(numberType.toInt(value));
	}
	
	@Override
	public void writeHeader(OutputStream out) throws IOException {
		Jackson.BINARY_MAPPER.writeValue(out, dictionary);
	}

	@Override
	public void readHeader(JsonParser input) throws IOException {
		dictionary = Jackson.BINARY_MAPPER.readValue(input, Dictionary.class);
	}
	
	@Override
	public void storeExternalInfos(NamespacedStorage storage, Consumer<Dictionary> dictionaryConsumer) {
		dictionary.setName(dictionaryId.getDictionary());
		dictionary.setDataset(dictionaryId.getDataset());
		dictionaryConsumer.accept(dictionary);
	}

	@Override
	public void loadExternalInfos(NamespacedStorage storage) {
		dictionary = Objects.requireNonNull(storage.getDictionary(dictionaryId));
	}
	
	public int size() {
		return dictionary.size();
	}
	
	public int getId(byte[] value) {
		return dictionary.getId(value);
	}

	public Iterator<byte[]> iterator() {
		return Iterators.transform(dictionary.iterator(), DictionaryEntry::getValue);
	}
	
	@Override
	public String toString() {
		return "StringTypeDictionary[dictionary=" + dictionary + ", numberType=" + numberType + "]";
	}
	
	@Override
	public long estimateTypeSize() {
		return dictionary.estimateMemoryConsumption();
	}

	public void adaptUnderlyingDictionary(Dictionary newDict, VarIntType newNumberType) {
		dictionaryId = newDict.getId();
		dictionary = newDict;
		if(newNumberType.estimateMemoryBitWidth() > numberType.estimateMemoryBitWidth()) {
			log.warn(
				"The width of a column is increased from {} to {} bits because of the shared dictionary {}",
				numberType.estimateMemoryBitWidth(),
				newNumberType.estimateMemoryBitWidth(),
				newDict.getId()
			);
		}
		numberType = newNumberType;
		this.setPrimitiveType(newNumberType.getPrimitiveType());
	}
}
