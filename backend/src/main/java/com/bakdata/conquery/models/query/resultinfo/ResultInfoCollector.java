package com.bakdata.conquery.models.query.resultinfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ResultInfoCollector {
	
	@Getter
	private final HashMap<String, AtomicInteger> ocurrenceCounter = new HashMap<>();
	@Getter
	private final List<ResultInfo> infos = new ArrayList<>();
	
	public void add(ResultInfo info) {
		infos.add(info);
	}
	
	public void add(SelectResultInfo info) {
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
		for(ResultInfo info : newInfos) {
			if(info instanceof SelectResultInfo) {
				add((SelectResultInfo)info);
			}
			else {
				add((SimpleResultInfo)info);
			}
		}
	}
}