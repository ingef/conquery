package com.bakdata.eva.forms.common;

import com.bakdata.conquery.models.query.concept.specific.CQOr;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter @AllArgsConstructor
public class FeatureDescription<T extends CQOr> {

	private String name;
	private T feature;
	private FeatureGroupDescription<T> group;
}
