package com.bakdata.conquery.models.preproc.outputs;

import java.util.List;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.preproc.ColumnDescription;
import com.bakdata.conquery.models.preproc.PPColumn;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.Data;

@JsonTypeInfo(use=JsonTypeInfo.Id.CUSTOM, property="type")
@CPSBase
public interface AutoOutput {

	List<OutRow> createOutput(int primaryId, String[] row, PPColumn[] columns, int inputSource, long lineId) throws ParsingException;

	@JsonIgnore
	int getWidth();

	ColumnDescription getColumnDescription(int i);

	List<OutRow> finish() throws ParsingException;

	@Data
	class OutRow {
		private final int primaryId;
		private final PPColumn[] types;
		private final Object[] data;
	}
}
