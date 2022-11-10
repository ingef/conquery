package com.bakdata.conquery.models.forms.arx.models;

import java.util.Map;

import javax.validation.constraints.NotEmpty;

import com.bakdata.conquery.io.cps.CPSBase;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.deidentifier.arx.criteria.PrivacyCriterion;

@JsonTypeInfo(property = "type", use = JsonTypeInfo.Id.CUSTOM)
@CPSBase
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class PrivacyModel {

	@NotEmpty
	private Map<String, String> localizedLabels;

	@JsonIgnore
	public abstract PrivacyCriterion getPrivacyCriterion();
}
