package com.bakdata.conquery.models.preproc.outputs;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import org.hibernate.validator.constraints.NotEmpty;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.preproc.ColumnDescription;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.Data;

@Data
@JsonTypeInfo(use=JsonTypeInfo.Id.CUSTOM, property="operation")
@CPSBase
public abstract class Output implements Serializable {
	
	private static final long serialVersionUID = 1L;
	public static final List<Object> NULL = Collections.singletonList(null);
	
	
	@NotEmpty
	private String name;
	private boolean required = false;

	public abstract List<Object> createOutput(CType<?,?> type, String[] row, int source, long sourceLine) throws ParsingException;

	@JsonIgnore
	public abstract MajorTypeId getResultType();

	@JsonIgnore
	public ColumnDescription getColumnDescription() {
		return new ColumnDescription(name, getResultType());
	}
}
