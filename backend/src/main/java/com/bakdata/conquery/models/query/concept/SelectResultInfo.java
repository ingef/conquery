package com.bakdata.conquery.models.query.concept;

import java.util.concurrent.atomic.AtomicInteger;

import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.externalservice.ResultType;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class SelectResultInfo extends ResultInfo {
	private final Select select;

	public SelectResultInfo(String name, ResultType type, AtomicInteger sameNameOcurrences, int postfix, Select select) {
		super(name, type, sameNameOcurrences, postfix);
		this.select = select;
	}
	
	@Override
	public SelectResultInfo withName(@NonNull String name) {
		return this.getName() == name ? this : new SelectResultInfo(name, this.getType(), this.getSameNameOcurrences(), this.getPostfix(), this.getSelect());
	}
}
