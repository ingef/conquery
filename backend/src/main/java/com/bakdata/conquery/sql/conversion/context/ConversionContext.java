package com.bakdata.conquery.sql.conversion.context;

import java.time.LocalDate;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.sql.conversion.SqlConverterService;
import lombok.Builder;
import lombok.Value;
import lombok.With;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Select;

@Value
@With
@Builder
public class ConversionContext {

	DSLContext dslContext;
	SqlConverterService sqlConverterService;
	Select<Record> query;
	Range<LocalDate> dateRestricionRange;

}
