

package com.bakdata.conquery.models.query.resultinfo;

import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.PrintSettings;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.MutableClassToInstanceMap;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString
public abstract class ResultInfo {
	/**
	 * Calculated same name index for this column. If 0, the postfix can be omitted.
	 */
	private int postfix;
	private ClassToInstanceMap<Object> appendices = MutableClassToInstanceMap.create();
	
	@JsonIgnore
	public final String getUniqueName(PrintSettings settings) {
		return (postfix > 0) ? getName(settings) + "_" + postfix : getName(settings);
	}
	
	public abstract String getName(PrintSettings settings);
	
	@ToString.Include
	public abstract ResultType getType();
	
	public <T> void addAppendix(Class<T> cl, T obj) {
		appendices.put(cl, obj);
	}
}
