package com.bakdata.conquery.models.forms.arx.models;

import java.math.BigDecimal;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.deidentifier.arx.ARXPopulationModel;
import org.deidentifier.arx.criteria.PrivacyCriterion;
import org.deidentifier.arx.risk.RiskModelPopulationUniqueness;

@CPSType(id = "POPULATION_UNIQUENESS", base = PrivacyModel.class)
@Getter
@Setter(AccessLevel.PRIVATE)
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PopulationUniqueness extends PrivacyModel {

	@DecimalMin("0")
	private BigDecimal riskThreshold = BigDecimal.valueOf(0.001);

	@NotNull
	private RiskModelPopulationUniqueness.PopulationUniquenessModel populationUniquenessModel;

	private ARXPopulationModel.Region region = ARXPopulationModel.Region.EUROPEAN_UNION;

	@Override
	public PrivacyCriterion getPrivacyCriterion() {
		return new org.deidentifier.arx.criteria.PopulationUniqueness(riskThreshold.doubleValue(), populationUniquenessModel, ARXPopulationModel.create(region));
	}
}
