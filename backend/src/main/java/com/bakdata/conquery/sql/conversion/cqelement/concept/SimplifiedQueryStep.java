package com.bakdata.conquery.sql.conversion.cqelement.concept;

import java.util.List;

import com.bakdata.conquery.sql.conversion.model.SqlIdColumns;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SelectForStep;
import org.jooq.Table;
import org.jooq.TableLike;
import org.jooq.TableOnConditionStep;

@Data
@RequiredArgsConstructor
public class SimplifiedQueryStep {
	private final String name;
	private final Table<?> query;
	private final SqlIdColumns ids;
	private final List<Field<?>> fields;

//	public SimplifiedQueryStep or(SimplifiedQueryStep other) {
//		TableOnConditionStep<Record> joined = query.join(other.query).on(ids.join2(other.ids));
//
//	}
}
