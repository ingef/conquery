package com.bakdata.conquery.models.preproc;

import com.bakdata.conquery.models.common.CDateRange;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PPHeader {
	private int validityHash;
	private String name;
	private String table;
	private long rows;
	private long groups;
	private CDateRange eventRange;
	private PPColumn primaryColumn;
	private PPColumn[] columns;
}
