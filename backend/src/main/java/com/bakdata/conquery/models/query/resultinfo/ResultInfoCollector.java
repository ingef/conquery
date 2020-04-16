package com.bakdata.conquery.models.query.resultinfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.bakdata.conquery.models.query.PrintSettings;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ResultInfoCollector {
	@Getter
	private final PrintSettings settings;
	private final HashMap<String, AtomicInteger> ocurrenceCounter = new HashMap<>();
	@Getter
	private final List<ResultInfo> infos = new ArrayList<>();
	
	public void add(SimpleResultInfo info) {
		infos.add(info);
	}
	
	public void add(SelectResultInfo info) {
		String name = info.getName();
		AtomicInteger occurence = ocurrenceCounter.computeIfAbsent(name, str -> new AtomicInteger(0));
		info.setPostfix(occurence.getAndIncrement());
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