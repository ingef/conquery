<#import "/com/bakdata/conquery/models/events/generation/Helper.ftl" as f/>
package com.bakdata.conquery.models.events.generation;

import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.util.io.SmallOut;
import com.bakdata.conquery.io.DeserHelper;


import java.time.LocalDate;
import java.io.IOException;

import java.lang.Integer;
import com.bakdata.conquery.models.common.CQuarter;
import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.common.Range;

import com.google.common.collect.ImmutableMap;
import java.math.BigDecimal;

import java.util.Collections;
import java.util.List;

import org.apache.commons.io.output.ByteArrayOutputStream;

import com.tomgibara.bits.BitStore;
import com.tomgibara.bits.Bits;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.util.Map;
import java.io.InputStream;
import java.io.IOException;

import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.events.generation.BlockFactory;
import com.bakdata.conquery.io.DeserHelper;
import java.math.BigDecimal;

import com.bakdata.conquery.util.io.SmallIn;
import com.bakdata.conquery.util.io.SmallOut;
import java.time.LocalDate;

import java.lang.Integer;
import com.bakdata.conquery.models.common.daterange.CDateRange;

import java.util.List;

import com.tomgibara.bits.BitStore;
import com.tomgibara.bits.Bits;
import com.bakdata.conquery.util.PackedUnsigned1616;
import com.bakdata.conquery.models.common.CQuarter;
import com.google.common.primitives.Ints;

public class Block_${suffix} extends Block {

	private BitStore nullBits;
	private int size;
	<#list imp.columns as column>
	<#if column.type.lines != column.type.nullLines>
	private ${column.type.primitiveType.name}[] <@f.field column/>;
	</#if>
	</#list>
	
	public void setSize(int size) {
		this.size = size;
		<#list imp.columns as column>
		<#if column.type.lines != column.type.nullLines>
		<@f.field column/> = new ${column.type.primitiveType.name}[size];
		</#if>
		</#list>
	}
	
	/* getter and setter for the fields */
	<#list imp.columns as column>
	<#if column.type.lines != column.type.nullLines>
	<#import "/com/bakdata/conquery/models/events/generation/types/${column.type.class.simpleName}.ftl" as t/>
	public ${column.type.primitiveType.name} <@f.get column/>(int event) {
		return <@f.field column/>[event];
	}
	
	public ${column.type.typeId.primitiveType.name} <@f.getMajor column/>(int event) {
		return <@t.majorTypeTransformation type=column.type><@f.field column/>[event]</@t.majorTypeTransformation>;
	}
	
	public void <@f.set column/>(int event, ${column.type.primitiveType.name} value) {
		this.<@f.field column/>[event] = value;
	}
	</#if>
	</#list>
	
	public void setNullBits(BitStore nullBits){
		this.nullBits = nullBits;
	}
	
	@Override
	public void serialize(JsonGenerator gen, SerializerProvider serializers) throws IOException {		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		try (SmallOut output = new SmallOut(baos)){
			writeContent(output);
		}
		byte[] content = baos.toByteArray();
		
		gen.writeBinary(content);
	}
	
	@Override
	public void writeContent(SmallOut output) throws IOException {
		output.writeInt(this.size, true);
		
		byte[] nullBitsAsBytes = Bits.asStore(nullBits.toByteArray()).toByteArray();
		output.writeInt(nullBitsAsBytes.length, true);
		output.write(nullBitsAsBytes);
		
		for (int event = 0; event < size; event++) {					
		<#list imp.columns as column>
			<#import "/com/bakdata/conquery/models/events/generation/types/${column.type.class.simpleName}.ftl" as t/>
			<#if column.type.lines == column.type.nullLines>
			<#--no values, column is all null-->
			<#elseif column.type.requiresExternalNullStore()>
			if(this.has(event, ${column_index})) {
				<@t.kryoSerialization type=column.type><@f.get column/>(event)</@t.kryoSerialization>;
			}
			<#else>
			<@t.kryoSerialization type=column.type><@f.get column/>(event)</@t.kryoSerialization>;
			</#if>
		</#list>
		}
	}
	
	@Override
	public int size() {
		return size;
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
				return !(<@t.nullCheck type=col.type><@f.get col/>(event)</@t.nullCheck>);
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
	public ${type.primitiveType.name} get${type.label}(int event, Column column) {
		switch(column.getPosition()) {
	<#list imp.columns as col>
		<#if col.type.typeId == type && col.type.nullLines != col.type.lines>
			case ${col.position}:
				return <@f.getMajor col/>(event);
		</#if>
	</#list>
			default:
				throw new IllegalArgumentException("Column "+column+" is not of Type ${type} or does not have values");
		}
	}
	</#list>
	
	@Override
	public Object getRaw(int event, Column column) {
		switch(column.getPosition()) {
	<#list types as type>
	<#list imp.columns as col>
		<#-- there are no getters for null only columns-->
		<#if col.type.typeId == type && col.type.nullLines != col.type.lines>
			case ${col.position}:
				return <@f.get col/>(event);
		</#if>
	</#list>
	</#list>
			default:
				throw new IllegalArgumentException("Column "+column+" is not valid");
		}
	}
	
	@Override
	public Object getAsObject(int event, Column column) {
		switch(column.getPosition()) {
	<#list types as type>
	<#list imp.columns as col>
		<#-- there are no getters for null only columns-->
		<#if col.type.typeId == type && col.type.nullLines != col.type.lines>
			case ${col.position}:
				return <@f.getMajor col/>(event);
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
                    return dateRange.contains(<@f.getMajor col/>(event));
                <#elseif col.type.typeId == "DATE_RANGE">
                case ${col.position}:
					return dateRange.intersects(<@f.getMajor col/>(event));
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
                    return dateRanges.contains(<@f.getMajor col/>(event));
                <#elseif col.type.typeId == "DATE_RANGE">
                    case ${col.position}:
                    return dateRanges.intersects(<@f.getMajor col/>(event));
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
                    return CDateRange.exactly(<@f.get col/>(event));
                <#elseif col.type.typeId == "DATE_RANGE">
                case ${col.position}:
                    return <@f.getMajor col/>(event);
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
			//"${safeJavaString(col.name)}" is always null
			<#else>
			if(has(event, ${col_index})) {
				builder.put(
					"${safeJavaString(col.name)}",
					imp.getColumns()[${col_index}].getType().createScriptValue(<@f.get col/>(event))
				);
			}
			</#if>
		</#list>
		return builder.build();
	}
}
