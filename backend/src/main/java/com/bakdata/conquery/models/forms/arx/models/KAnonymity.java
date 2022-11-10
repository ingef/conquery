package com.bakdata.conquery.models.forms.arx.models;

import javax.validation.constraints.Min;

import com.bakdata.conquery.io.cps.CPSType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.deidentifier.arx.criteria.PrivacyCriterion;

@CPSType(id = "K_ANONYMITY", base = PrivacyModel.class)
@Getter
@Setter(AccessLevel.PRIVATE)
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class KAnonymity extends PrivacyModel {

	@Min(1)
	private int k;

	@Override
	public PrivacyCriterion getPrivacyCriterion() {
		return new org.deidentifier.arx.criteria.KAnonymity(k);

	}
}
