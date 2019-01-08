package com.bakdata.conquery.models.concepts;

import com.bakdata.conquery.models.common.CDateRange;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class MatchingStats {
	
	private Multiset<Entry> entries = HashMultiset.create();
	@JsonIgnore
	private transient CDateRange span;
	@JsonIgnore
	private transient long numberOfEvents = -1;
	
	public synchronized long countEvents() {
		if(numberOfEvents == -1L) {
			numberOfEvents = entries.stream().mapToLong(Entry::getNumberOfEvents).sum();
		}
		return numberOfEvents;
	}
	
	public synchronized CDateRange spanEvents() {
		if(span == null) {
			span = entries.stream().map(Entry::getSpan).reduce(null, CDateRange::spanOf);
		}
		return span;
	}
	
	public void updateEntry(WorkerId source, Entry value) {
		// TODO Auto-generated method stub
		
	}
	
	public synchronized void addEntry(Entry entry) {
		entries.add(entry);
		numberOfEvents += entry.getNumberOfEvents();
		span = CDateRange.spanOf(span, entry.getSpan());
	}

	public synchronized void removeEntry(Entry entry) {
		entries.remove(entry);
		numberOfEvents = -1;
		span = null;
	}
	
	@Data
	public static class Entry {
		private final long numberOfEvents;
		private final CDateRange span;
	}

	
}
