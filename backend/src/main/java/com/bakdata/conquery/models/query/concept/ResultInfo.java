package com.bakdata.conquery.models.query.concept;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.bakdata.conquery.models.concepts.select.Select;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.PrintSettings;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.experimental.Wither;

@AllArgsConstructor @Getter @Wither @ToString
public class ResultInfo {
	private final String name;
	private final ResultType type;
	/**
	 * Keeps track of the number of columns, that are generated with the same base name.
	 * Start with 0 for no other occurence.
	 */
	private final AtomicInteger sameNameOcurrences;
	/**
	 * Calculated index for this column. Should be {@code <= sameNameOcurrences}. If both are 0, the postfix can be omitted.
	 */
	private final int postfix;
	
	public String getUniqueName() {
		return (sameNameOcurrences != null && sameNameOcurrences.get() > 1) ? name + "_" + postfix : name;
	}
	
	@RequiredArgsConstructor
	public static class ResultInfoCollector {
		@Getter
		private final PrintSettings settings;
		private final HashMap<String, AtomicInteger> ocurrenceCounter = new HashMap<>();
		@Getter
		private final List<ResultInfo> infos = new ArrayList<>();
		
		public void add(ResultInfo constantInfo) {
			infos.add(constantInfo);
		}

		/*
		 * Column name is constructed from the most specific concept id the CQConcept
		 * has and the selector.
		 */
		public void add(SelectDescriptor selectDescr) {
			Select select = selectDescr.getSelect();
			String columnName = settings.getNameExtractor().apply(selectDescr);
			AtomicInteger occurence = ocurrenceCounter.computeIfAbsent(columnName, str -> new AtomicInteger(0));
			infos.add(new SelectResultInfo(columnName, select.getResultType(), occurence, occurence.getAndIncrement(), select));
		}
	}
}
