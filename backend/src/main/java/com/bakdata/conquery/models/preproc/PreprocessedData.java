package com.bakdata.conquery.models.preproc;

import java.util.Map;
import java.util.Set;

import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor(onConstructor_ = @JsonCreator)
public class PreprocessedData {

	private final Map<String, Integer> starts;
	private final Map<String, Integer> lengths;

	private final Map<String, ColumnStore> stores;

	@JsonIgnore
	public boolean isEmpty() {
		return getStarts() == null;
	}

	public int size() {
		return starts.size();
	}

	public Set<String> entities() {
		return starts.keySet();
	}
}
