package com.bakdata.conquery.apiv1.query.concept.specific.external;

import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.With;

public class IdColumn extends FormatColumn {

	public static String HANDLE = "ID";

	public String[] read(String[] row, int position) {
		return new String[]{row[position]};
	}
}
