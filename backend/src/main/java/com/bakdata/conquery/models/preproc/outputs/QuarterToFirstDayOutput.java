package com.bakdata.conquery.models.preproc.outputs;

import javax.validation.constraints.NotNull;

import java.util.Collections;
import java.util.List;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.QuarterUtils;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.bakdata.conquery.models.types.parser.Parser;
import com.fasterxml.jackson.annotation.JsonIgnore;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import lombok.Data;

@Data
@CPSType(id = "QUARTER_TO_FIRST_DAY", base = Output.class)
public class QuarterToFirstDayOutput extends Output {

	private static final long serialVersionUID = 1L;

	@JsonIgnore
	private int yearIndex, quarterIndex;

	@NotNull
	private String yearColumn, quarterColumn;

	@Override
	public void setHeaders(Object2IntArrayMap<String> headers) {
		assertRequiredHeaders(headers, yearColumn, quarterColumn);

		yearIndex = headers.getInt(yearColumn);
		quarterIndex = headers.getInt(quarterColumn);
	}

	@Override
	public List<Object> createOutput(Parser<?> type, String[] row, int source, long sourceLine) {
		if (row[yearIndex] == null || row[quarterIndex] == null) {
			return NULL;
		}

		return Collections.singletonList(
				CDate.ofLocalDate(
						QuarterUtils.getFirstDayOfQuarter(Integer.parseInt(row[yearIndex]), Integer.parseInt(row[quarterIndex]))
				));
	}

	@Override
	public MajorTypeId getResultType() {
		return MajorTypeId.DATE;
	}
}
