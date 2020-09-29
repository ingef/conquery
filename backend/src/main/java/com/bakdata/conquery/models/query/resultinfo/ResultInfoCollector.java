package com.bakdata.conquery.models.query.resultinfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ResultInfoCollector {
	
	/**
	 * Is used to track possible name duplicates for column names and provide an index to enumerate these.
	 * This lowers the risk of duplicate column names in the result.
	 * 
	 */
	@Getter
	private final HashMap<String, Integer> ocurrenceCounter = new HashMap<>();
	@Getter
	private final List<ResultInfo> infos = new ArrayList<>();
	
	public void add(ResultInfo info) {
		info.setOcurrenceCounter(ocurrenceCounter);
		infos.add(info);
	}

	public int size() {
		return infos.size();
	}

	public boolean isEmpty() {
		return infos.isEmpty();
	}

	public void addAll(List<ResultInfo> newInfos) {
		newInfos.forEach(this::add);
	}
}