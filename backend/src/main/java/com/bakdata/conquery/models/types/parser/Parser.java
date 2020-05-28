package com.bakdata.conquery.models.types.parser;

import javax.annotation.Nonnull;

import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.types.CType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@RequiredArgsConstructor
@ToString
public abstract class Parser<MAJOR_JAVA_TYPE> {
	private long lines = 0;
	private long nullLines = 0;
	
	public MAJOR_JAVA_TYPE parse(String v) throws ParsingException {
		if(v==null) {
			return null;
		}
		else {
			try {
				return parseValue(v);
			}
			catch(Exception e) {
				throw new ParsingException("Failed to parse '"+v+"' with "+this.getClass().getSimpleName(), e);
			}
		}
	}
	
	protected abstract MAJOR_JAVA_TYPE parseValue(@Nonnull String value) throws ParsingException;

	protected void registerValue(MAJOR_JAVA_TYPE v) {};
	
	protected abstract Decision<MAJOR_JAVA_TYPE, ?, ? extends CType<MAJOR_JAVA_TYPE, ?>> decideType();
	
	public Decision<MAJOR_JAVA_TYPE, ?, ? extends CType<MAJOR_JAVA_TYPE, ?>> findBestType() {
		Decision<MAJOR_JAVA_TYPE, ?, ? extends CType<MAJOR_JAVA_TYPE, ?>> dec = decideType();
		setLineCounts(dec.getType());
		return dec;
	}
	
	public MAJOR_JAVA_TYPE addLine(MAJOR_JAVA_TYPE v) {
		lines++;
		if(v == null) {
			nullLines++;
		}
		else {
			registerValue(v);
		}
		return v;
	}
	
	public void setLineCounts(CType<?, ?> type) {
		type.setLines(lines);
		type.setNullLines(nullLines);
	}
}
