

package com.bakdata.conquery.models.query.resultinfo;

import com.bakdata.conquery.models.externalservice.ResultType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.MutableClassToInstanceMap;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString
@RequiredArgsConstructor
public abstract class ResultInfo {

	/**
	 * Calculated same name index for this column. If 0, the postfix can be omitted.
	 */
	private int postfix;
	private ClassToInstanceMap<Object> appendices = MutableClassToInstanceMap.create();
	@NonNull
	
	@JsonIgnore
	public final String getUniqueName() {
		return (postfix > 0) ? getName() + "_" + postfix : getName();
	}
	
	public abstract String getName();
	
	@ToString.Include
	public abstract ResultType getType();
	
	public <T> void addAppendix(Class<T> cl, T obj) {
		appendices.putInstance(cl, obj);
	}
}
