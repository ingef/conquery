package com.bakdata.conquery.models.preproc;

import com.bakdata.conquery.models.events.MajorTypeId;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor(onConstructor_ = {@JsonCreator} )
public class PPColumn {
	@NonNull
	private final String name;

	private final MajorTypeId type;
}
