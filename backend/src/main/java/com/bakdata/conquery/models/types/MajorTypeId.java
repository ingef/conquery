package com.bakdata.conquery.models.types;

import java.util.function.Supplier;

import com.bakdata.conquery.models.types.specific.BooleanType;
import com.bakdata.conquery.models.types.specific.DateRangeType;
import com.bakdata.conquery.models.types.specific.DateType;
import com.bakdata.conquery.models.types.specific.DecimalType;
import com.bakdata.conquery.models.types.specific.IntegerType;
import com.bakdata.conquery.models.types.specific.MoneyType;
import com.bakdata.conquery.models.types.specific.RealType;
import com.bakdata.conquery.models.types.specific.StringType;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum MajorTypeId implements MajorTypeIdHolder {

	STRING(false, "String", StringType::new),
	INTEGER(false, "Integer", IntegerType::new),
	BOOLEAN(false, "Boolean", BooleanType::new),
	REAL(false, "Real", RealType::new),
	DECIMAL(false, "Decimal", DecimalType::new),
	MONEY(false, "Money", MoneyType::new),
	DATE(true, "Date", DateType::new),
	DATE_RANGE(true, "DateRange", DateRangeType::new);
	
	@Getter
	private final boolean dateCompatible;
	@Getter
	private final String label;
	private final Supplier<CType<?,?>> supplier;
	
	public CType<?,?> createType() {
		return supplier.get();
	}

	@Override
	public MajorTypeId getTypeId() {
		return this;
	}
}
