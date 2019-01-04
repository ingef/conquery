package com.bakdata.conquery.models.preproc.outputs;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter @CPSType(id="UNPIVOT", base=Output.class)
public class UnpivotOutput extends Output {
	
	private static final long serialVersionUID = 1L;
	

	@NotEmpty
	private int[] inputColumns;
	@NotNull
	private MajorTypeId inputType;
	private boolean includeNulls=false;
	
	@Override
	public List<Object> createOutput(CType<?,?> type, String[] row, int source, long sourceLine) throws ParsingException {
		List<Object> parsedRows = new ArrayList<>();
		for(int i=0;i<inputColumns.length;i++) {
			String value = row[inputColumns[i]];
			if(value!=null) {
				parsedRows.add(type.parse(value));
			}
			else if(includeNulls) {
				parsedRows.add(null);
			}
		}
		//to prevent an empty result if each column is null
		if(parsedRows.isEmpty()) {
			return NULL;
		}
		else {
			return parsedRows;
		}
	}

	@Override
	public MajorTypeId getResultType() {
		return inputType;
	}
}