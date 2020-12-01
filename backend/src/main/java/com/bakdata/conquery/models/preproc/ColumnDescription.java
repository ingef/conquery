package com.bakdata.conquery.models.preproc;

import com.bakdata.conquery.models.events.parser.MajorTypeId;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ColumnDescription {

	private final String name;
	private final MajorTypeId type;

}
