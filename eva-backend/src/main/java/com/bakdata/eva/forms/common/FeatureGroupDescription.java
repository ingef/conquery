package com.bakdata.eva.forms.common;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.bakdata.conquery.models.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.query.concept.specific.CQOr;
import com.bakdata.eva.forms.common.ColumnDescriptor.MatchingType;
import com.bakdata.eva.models.forms.FeatureGroup;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter @AllArgsConstructor
public abstract class FeatureGroupDescription<T extends CQOr> {

	private List<T> features;
	private FeatureGroup groupType;
	private BiConsumer<ColumnDescriptor, CQOr> columnManipulator;
	
	
	public static class MatchedFD extends FeatureGroupDescription<Matched> {
		public MatchedFD(List<Matched> features, FeatureGroup group) {
			super(features, group, null);
		}
		
		public MatchedFD(List<Matched> features, FeatureGroup group, BiConsumer<ColumnDescriptor, CQOr> columnManipulator) {
			super(features, group, columnManipulator);
		}

		@Override
		public void addFixedFeature(CQConcept c) {
			Matched m = new Matched();
			m.setMatchingType(MatchingType.FIXED);
			m.setChildren(Collections.singletonList(c));
			getFeatures().add(0,m);
		}
	}
	
	public static class GroupFD extends FeatureGroupDescription<CQOr> {
		public GroupFD(List<CQOr> features, FeatureGroup group) {
			super(features, group, null);
		}
		
		public GroupFD(List<CQOr> features, FeatureGroup group, BiConsumer<ColumnDescriptor, CQOr> columnManipulator) {
			super(features, group, columnManipulator);
		}

		@Override
		public void addFixedFeature(CQConcept c) {
			CQOr g = new CQOr();
			g.setChildren(Arrays.asList(c));
			getFeatures().add(0,g);
		}
	}
	
	public abstract void addFixedFeature(CQConcept c);

	public Stream<FeatureDescription<T>> streamFeatureDescriptions() {
		return IntStream.range(0, features.size())
			.mapToObj(id->new FeatureDescription<T>(
				groupType.getPrefix()+id, 
				features.get(id), 
				this
			));
	}
}
