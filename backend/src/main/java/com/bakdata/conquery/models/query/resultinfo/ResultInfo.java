

package com.bakdata.conquery.models.query.resultinfo;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.PrintSettings;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.MutableClassToInstanceMap;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString @EqualsAndHashCode
@RequiredArgsConstructor
public abstract class ResultInfo {
	private final static int UNSET_PREFIX = -1;
	/**
	 * Calculated same name index for this column. If 0, the postfix can be omitted.
	 */
	@EqualsAndHashCode.Exclude
	private int postfix = UNSET_PREFIX;
	
	/**
	 * Is injected by the {@link ResultInfoCollector} for {@link SelectResultInfo}s.
	 */
	@EqualsAndHashCode.Exclude
	private HashMap<String, AtomicInteger> ocurrenceCounter;
	private ClassToInstanceMap<Object> appendices = MutableClassToInstanceMap.create();
	
	@NonNull
	@JsonIgnore
	public final String getUniqueName(PrintSettings settings) {
		String name = getName(settings);
		if(ocurrenceCounter == null) {
			return name;
		}
		synchronized (ocurrenceCounter) {
			if(postfix == UNSET_PREFIX) {
				AtomicInteger occurence = ocurrenceCounter.computeIfAbsent(name, str -> new AtomicInteger(0));
				postfix = occurence.getAndIncrement();
			}
		}
		return (postfix > 0) ? name + "_" + postfix : name;
	}
	
	public abstract String getName(PrintSettings settings);
	
	@ToString.Include
	public abstract ResultType getType();
	
	public <T> void addAppendix(Class<T> cl, T obj) {
		appendices.putInstance(cl, obj);
	}
}
