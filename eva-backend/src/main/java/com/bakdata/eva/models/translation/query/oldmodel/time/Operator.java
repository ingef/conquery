package com.bakdata.eva.models.translation.query.oldmodel.time;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter @AllArgsConstructor
public enum Operator {
	BEFORE					(false),
	BEFORE_OR_SAME			(true),
	SAME					(true),
	DAYS_BEFORE				(false),
	DAYS_OR_NO_EVENT_BEFORE	(false);
	
	private boolean trimIncludingEquals;
}
