package com.bakdata.conquery.models.types;

import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.xodus.NamespacedStorage;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonParser;
import com.google.common.primitives.Primitives;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@JsonTypeInfo(use=JsonTypeInfo.Id.CUSTOM, property="type")
@CPSBase
@Getter @Setter @RequiredArgsConstructor
public abstract class CType<JAVA_TYPE, MAJOR_TYPE extends CType<?,?>> implements MajorTypeIdHolder {

	@JsonIgnore
	private transient final MajorTypeId typeId;
	@JsonIgnore
	private transient final Class<?> primitiveType;

	private long lines = 0;
	private long nullLines = 0;

	public void init(DatasetId dataset) {}
	public JAVA_TYPE parse(String v) throws ParsingException {
		if(v==null) {
			return null;
		}
		else {
			try {
				return parseValue(v);
			}
			catch(Exception e) {
				throw new ParsingException("Failed to parse '"+v+"' as "+this.getClass().getSimpleName(), e);
			}
		}
	}
	
	public JAVA_TYPE addLine(JAVA_TYPE v) {
		lines++;
		if(v == null) {
			nullLines++;
		}
		else {
			registerValue(v);
		}
		return v;
	}

	protected abstract JAVA_TYPE parseValue(@Nonnull String value) throws ParsingException;

	protected void registerValue(JAVA_TYPE v) {};

	public CType<?,MAJOR_TYPE> bestSubType() {
		return this;
	}
	
	public JAVA_TYPE transformFromMajorType(MAJOR_TYPE majorType, Object value) {
		return (JAVA_TYPE)value;
	}
	
	public Object transformToMajorType(JAVA_TYPE value, MAJOR_TYPE majorType) {
		return value;
	}

	public Object createScriptValue(JAVA_TYPE value) {
		return value;
	}

	public Object createPrintValue(JAVA_TYPE value) { return value != null ? createScriptValue(value) : ""; }

	public void writeHeader(OutputStream out) throws IOException {}
	public void readHeader(JsonParser input) throws IOException {}
	public void storeExternalInfos(NamespacedStorage storage, Consumer<Dictionary> dictionaryConsumer) {}
	public void loadExternalInfos(NamespacedStorage storage) {}

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}

	public boolean canStoreNull() {
		return !primitiveType.isPrimitive();
	}

	public boolean requiresExternalNullStore() {
		return !canStoreNull() && getNullLines()>0;
	}

	@JsonIgnore
	public Class<?> getBoxedType(){
		return Primitives.wrap(getPrimitiveType());
	}
}
