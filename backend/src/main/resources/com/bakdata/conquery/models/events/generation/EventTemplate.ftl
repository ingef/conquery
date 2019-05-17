package com.bakdata.conquery.models.events.generation;

import java.lang.Integer;
import com.bakdata.conquery.models.common.CQuarter;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import java.time.LocalDate;
import com.google.common.primitives.Ints;
import com.google.common.collect.Range;
import java.math.BigDecimal;
import java.math.BigDecimal;

public class Event_${suffix} {
	<#list imp.columns as column>
	<#if column.type.lines != column.type.nullLines>
	private ${column.type.primitiveType.name} ${safeName(column.name)};
	</#if>
	</#list>
	
	<#list imp.columns as column>
	<#if column.type.lines != column.type.nullLines>
	<#import "/com/bakdata/conquery/models/events/generation/types/${column.type.class.simpleName}.ftl" as t/>
	public ${column.type.primitiveType.name} get${safeName(column.name)?cap_first}() {
		return ${safeName(column.name)};
	}
	
	public ${column.type.typeId.createType().primitiveType.name} get${safeName(column.name)?cap_first}AsMajor() {
		return <@t.majorTypeTransformation type=column.type>${safeName(column.name)}</@t.majorTypeTransformation>;
	}
	
	public void set${safeName(column.name)?cap_first}(${column.type.primitiveType.name} value) {
		this.${safeName(column.name)} = value;
	}
	</#if>
	</#list>
}