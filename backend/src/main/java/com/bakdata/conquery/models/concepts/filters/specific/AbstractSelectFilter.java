package com.bakdata.conquery.models.concepts.filters.specific;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.api.description.FEFilter;
import com.bakdata.conquery.models.api.description.FEFilterType;
import com.bakdata.conquery.models.api.description.FEValue;
import com.bakdata.conquery.models.concepts.filters.SingleColumnFilter;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter @Setter @Slf4j @RequiredArgsConstructor
public abstract class AbstractSelectFilter<FE_TYPE extends FilterValue<?>> extends SingleColumnFilter<FE_TYPE> implements ISelectFilter {

	

	protected Map<String, String> labels = Collections.emptyMap();
	protected boolean matchLabels = false;
	protected boolean allowDropFile = false;
	@JsonIgnore
	protected Map<String, String> realLabels;
	@JsonIgnore
	protected Map<String, String> labelsToRealLabels;
	
	@JsonIgnore
	private final int maximumSize;
	@JsonIgnore
	private final FEFilterType filterType;

	private Dictionary dictionary;
	
	@Override
	public EnumSet<MajorTypeId> getAcceptedColumnTypes() {
		return EnumSet.of(MajorTypeId.STRING);
	}
	
	@Override
	public void configureFrontend(FEFilter f) {
		f.setType(filterType);
		//see #160
		/*
		Column c = getColumn();
		if(!c.getType().equals(ColumnType.STRING))
			throw new ConceptConfigurationException(getConnector(), filterType+" filter is incompatible with columns of type "+c.getType());
		
		DictionaryStatistics stats = (DictionaryStatistics)c.getStatistics();
		
		if(maximumSize!=-1 && stats.getDict().size()>maximumSize)
			throw new ConceptConfigurationException(getConnector(), "Too many possible values ("+stats.getDict().size()+" of "+maximumSize+") in filter "+this.getId()+".");
		*/
		Dictionary dict = null;//stats.getDict();
		realLabels = dict
			.values()
			.stream()
			.collect(Collectors.toMap(Function.identity(), e-> {
				String r = labels.get(e);
				if(r == null) {
					if(!labels.isEmpty()) {
						log.warn("The value '{}' could not be mapped with the given labels of {}.{}",e,this.getConnector().getLabel(),this.getLabel());
					}
					return e;
				}
				else {
					return r;
				}
			}));

		if (matchLabels) {
			// Build the inverse of realLabels to speed up value resolution later
			labelsToRealLabels = realLabels.entrySet()
				.stream()
				.collect(Collectors.toMap(Entry::getValue, Entry::getKey));
		}
		else {
			labelsToRealLabels = new HashMap<>();
		}

		if(maximumSize>0) {
			f.setOptions(FEValue.fromLabels(realLabels));
		}
	}

	public String resolveValueToRealValue(String value) {
		if (realLabels.containsKey(value)) {
			return value;
		}
		if (labelsToRealLabels.containsKey(value)) {
			return labelsToRealLabels.get(value);
		}
		return null;
	}
}
