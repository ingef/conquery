package com.bakdata.conquery.models.concepts.filters;

import java.util.EnumSet;

import javax.validation.ConstraintValidatorContext;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.exceptions.validators.DetailedValid;
import com.bakdata.conquery.models.exceptions.validators.DetailedValid.ValidationMethod2;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.fasterxml.jackson.annotation.JsonIgnore;

@DetailedValid
public interface ISingleColumnFilter {

	public void setColumn(Column c);
	public Column getColumn();
	
	@ValidationMethod2
	public default boolean validateColumnType(ConstraintValidatorContext ctx) {
		if(!getAcceptedColumnTypes().contains(getColumn().getType())) {
			ctx
				.buildConstraintViolationWithTemplate("The column needs to have one of the types "+getAcceptedColumnTypes()+" (was "+getColumn().getType()+")")
				.addConstraintViolation();
			return false;
		}
		return true;
	}
	
	@JsonIgnore
	public default EnumSet<MajorTypeId> getAcceptedColumnTypes() {
		return EnumSet.allOf(MajorTypeId.class);
	}
}
