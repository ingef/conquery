package com.bakdata.conquery.models.events.generation;

import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.util.io.SmallOut;
import com.bakdata.conquery.io.DeserHelper;


import java.time.LocalDate;
import java.io.IOException;

import java.lang.Integer;
import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.CDateRange;
import com.bakdata.conquery.models.common.Range;

import com.google.common.collect.ImmutableMap;
import java.math.BigDecimal;

import java.util.Collections;
import java.util.List;

import org.apache.commons.io.output.ByteArrayOutputStream;
import java.time.LocalDate;

import com.tomgibara.bits.BitStore;
import com.tomgibara.bits.Bits;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.util.Map;
import java.math.BigDecimal;

public class Block_${suffix} extends Block {

	private BitStore nullBits;
	private Event_${suffix}[] events;
	
	public Block_${suffix}(int entity, Import imp) {
		super(entity, imp);
	}
	
	public void setNullBits(BitStore nullBits){
		this.nullBits = nullBits;
	}
	
	public void setEvents(Event_${suffix}[] events){
		this.events = events;
	}
	
	@Override
	public void serialize(JsonGenerator gen, SerializerProvider serializers) throws IOException {
		gen.writeStartObject();
		gen.writeStringField("import","${imp.id}");
		gen.writeNumberField("entity",getEntity());
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		try (SmallOut output = new SmallOut(baos)){
			writeContent(output);
		}
		byte[] content = baos.toByteArray();
		
		gen.writeBinaryField("content",content);
		gen.writeEndObject();
	}
	
	@Override
	public void writeContent(SmallOut output) throws IOException {
		output.writeInt(this.events.length, true);
		
		byte[] nullBitsAsBytes = Bits.asStore(nullBits.toByteArray()).toByteArray();
		output.writeInt(nullBitsAsBytes.length, true);
		output.write(nullBitsAsBytes);
		
		for (int event = 0; event < events.length; event++) {					
		<#list imp.columns as column>
			<#import "/com/bakdata/conquery/models/events/generation/types/${column.type.class.simpleName}.ftl" as t/>
			<#if column.type.lines == column.type.nullLines>
			<#--no values, column is all null-->
			<#elseif column.type.requiresExternalNullStore()>
			if(this.has(event, ${column_index})) {
				<@t.kryoSerialization type=column.type>events[event].get${safeName(column.name)?cap_first}()</@t.kryoSerialization>;
			}
			<#else>
			<@t.kryoSerialization type=column.type>events[event].get${safeName(column.name)?cap_first}()</@t.kryoSerialization>;
			</#if>
		</#list>
		}
	}
	
	@Override
	public int size() {
		return events.length;
	}
	
	@Override
	public boolean has(int event, Column column) {
		return has(event, column.getPosition());
	}
	
	public boolean has(int event, int columnPosition) {
		switch(columnPosition) {
	<#list imp.columns as col>
	<#import "/com/bakdata/conquery/models/events/generation/types/${col.type.class.simpleName}.ftl" as t/>
			case ${col.position}:
		<#if col.type.nullLines == 0>
				return true;
		<#elseif col.type.nullLines == col.type.lines >
				return false;
		<#elseif !col.type.requiresExternalNullStore()>
				return !(<@t.nullCheck type=col.type>events[event].get${safeName(col.name)?cap_first}()</@t.nullCheck>);
		<#else>
				return !nullBits.getBit(${imp.nullWidth}*event+${col.nullPosition});
		</#if>
	</#list>
			default:
				throw new IllegalArgumentException("Column "+columnPosition+" is not a valid Column for this block");
		}
	}
	
	<#list types as type>
	@Override
	public ${type.createType().primitiveType.name} get${type.label}(int event, Column column) {
		switch(column.getPosition()) {
	<#list imp.columns as col>
		<#if col.type.typeId == type && col.type.nullLines != col.type.lines>
			case ${col.position}:
				return events[event].get${safeName(col.name)?cap_first}AsMajor();
		</#if>
	</#list>
			default:
				throw new IllegalArgumentException("Column "+column+" is not of Type ${type} or does not have values");
		}
	}
	</#list>
	
	@Override
	public Object getAsObject(int event, Column column) {
		switch(column.getPosition()) {
	<#list types as type>
	<#list imp.columns as col>
		<#-- there are no getters for null only columns-->
		<#if col.type.typeId == type && col.type.nullLines != col.type.lines>
			case ${col.position}:
				return events[event].get${safeName(col.name)?cap_first}AsMajor();
		</#if>
	</#list>
	</#list>
			default:
				throw new IllegalArgumentException("Column "+column+" is not valid");
		}
	}

	@Override
	public boolean eventIsContainedIn(int event, Column column, CDateRange dateRange) {
		if(!this.has(event, column)) {
			return false;
		}
		switch(column.getPosition()) {
		<#list imp.columns as col>
		    <#if col.type.lines != col.type.nullLines>
                <#if col.type.typeId == "DATE">
                case ${col.position}:
                    return dateRange.contains(events[event].get${safeName(col.name)?cap_first}AsMajor());
                <#elseif col.type.typeId == "DATE_RANGE">
                case ${col.position}:
                    return dateRange.intersects(events[event].get${safeName(col.name)?cap_first}AsMajor());
                </#if>
            </#if>
		</#list>
			default:
				throw new IllegalArgumentException("Column "+column+" is not a date type");
		}
	}

	@Override
	public boolean eventIsContainedIn(int event, Column column, CDateSet dateRanges) {
		if(!this.has(event, column)) {
			return false;
		}
		switch(column.getPosition()) {
		<#list imp.columns as col>
            <#if col.type.lines != col.type.nullLines>
                <#if col.type.typeId == "DATE">
                    case ${col.position}:
                    return dateRanges.contains(events[event].get${safeName(col.name)?cap_first}AsMajor());
                <#elseif col.type.typeId == "DATE_RANGE">
                    case ${col.position}:
                    return dateRanges.intersects(events[event].get${safeName(col.name)?cap_first}AsMajor());
                </#if>
            </#if>
		</#list>
		default:
		throw new IllegalArgumentException("Column "+column+" is not a date type");
		}
	}

	@Override
	public CDateRange getAsDateRange(int event, Column column) {
		if(!this.has(event, column)) {
			return null;
		}
		switch(column.getPosition()) {
		<#list imp.columns as col>
            <#if col.type.lines != col.type.nullLines>
                <#if col.type.typeId == "DATE">
                case ${col.position}:
                    return new CDateRange(events[event].get${safeName(col.name)?cap_first}(), events[event].get${safeName(col.name)?cap_first}());
                <#elseif col.type.typeId == "DATE_RANGE">
                case ${col.position}:
                    return events[event].get${safeName(col.name)?cap_first}();
                </#if>
            </#if>
		</#list>
			default:
				throw new IllegalArgumentException("Column "+column+" is not a date type");
		}
	}
	
	@Override
	public Map<String, Object> calculateMap(int event, Import imp) {
		 ImmutableMap.Builder<String, Object> builder = ImmutableMap.<String, Object>builder();

		<#list imp.columns as col>
			<#if col.type.lines == col.type.nullLines>
			//ImmutableMap does not allow for null values: builder.put("${safeJavaString(col.name)}", null)
			<#else>
			if(has(event, ${col_index})) {
				builder.put("${safeJavaString(col.name)}", imp.getColumns()[${col_index}].getType().createScriptValue(events[event].get${safeName(col.name)?cap_first}()));
			}

			</#if>
		</#list>
		return builder.build();
	}
}
