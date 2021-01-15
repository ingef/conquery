package com.bakdata.conquery.models.events.parser;

import javax.annotation.Nonnull;

import com.bakdata.conquery.models.config.ParserConfig;
import com.bakdata.conquery.models.events.EmptyStore;
import com.bakdata.conquery.models.events.stores.ColumnStore;
import com.bakdata.conquery.models.exceptions.ParsingException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@RequiredArgsConstructor
@ToString
@Slf4j
public abstract class Parser<MAJOR_JAVA_TYPE> {
	private final ParserConfig config;

	private int lines = 0;
	private int nullLines = 0;

	
	public MAJOR_JAVA_TYPE parse(String v) throws ParsingException {
		if(v==null) {
			return null;
		}
		try {
			return parseValue(v);
		}
		catch(Exception e) {
			throw new ParsingException("Failed to parse '"+v+"' with "+this.getClass().getSimpleName(), e);
		}
	}
	
	protected abstract MAJOR_JAVA_TYPE parseValue(@Nonnull String value) throws ParsingException;

	protected void registerValue(MAJOR_JAVA_TYPE v) {};
	
	protected abstract ColumnStore<MAJOR_JAVA_TYPE> decideType();
	
	public final ColumnStore<MAJOR_JAVA_TYPE> findBestType() {
		if (getLines() == 0 || getLines() == getNullLines()) {
			return new EmptyStore<>();
		}

		ColumnStore<MAJOR_JAVA_TYPE> dec = decideType();
		copyLineCounts(dec);
		return dec;
	}
	
	public MAJOR_JAVA_TYPE addLine(MAJOR_JAVA_TYPE v) {
		lines++;
		log.trace("Registering `{}` in line {}",v, lines);

		if(v == null) {
			nullLines++;
		}
		else {
			registerValue(v);
		}
		return v;
	}
	
	public void copyLineCounts(ColumnStore<?> type) {
		type.setLines(lines);
		type.setNullLines(nullLines);
	}
}
