<#import "/com/bakdata/conquery/models/events/generation/Helper.ftl" as f/>
package com.bakdata.conquery.models.events.generation;

import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.io.DeserHelper;


import java.time.LocalDate;
import java.io.IOException;

import java.lang.Integer;
import com.bakdata.conquery.models.common.CQuarter;
import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.ICDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.common.Range;

import com.google.common.collect.ImmutableMap;
import java.math.BigDecimal;

import java.util.Collections;
import java.util.List;

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

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.time.LocalDate;

import java.lang.Integer;
import com.bakdata.conquery.models.common.daterange.CDateRange;

import java.util.List;

import com.tomgibara.bits.BitStore;
import com.tomgibara.bits.Bits;
import com.bakdata.conquery.util.PackedUnsigned1616;
import com.bakdata.conquery.models.common.CQuarter;
import com.google.common.primitives.Ints;

public class Bucket_${suffix} extends Bucket {

	public Bucket_${suffix}(int bucketNumber, Import imp, int[] offsets) {
		super(bucketNumber, imp, offsets);
	}

	<#list imp.columns as column>
	<#if column.type.lines != column.type.nullLines>
	<#import "/com/bakdata/conquery/models/events/generation/types/${column.type.class.simpleName}.ftl" as t/>
	
	private ${column.type.primitiveType.name}[] <@f.field column/>;
	
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
	
	@Override
	public int getBucketSize() {
		return ${bucketSize};
	}
	
	@Override
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
	public Object getRaw(int event, int columnPosition) {
		switch(columnPosition) {
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
				throw new IllegalArgumentException("Column Position "+columnPosition+" is not valid");
		}
	}
	
	@Override
	public Object getAsObject(int event, int columnPosition) {
		switch(columnPosition) {
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
				throw new IllegalArgumentException("ColumnPosition "+columnPosition+" is not valid");
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
	public void initFields(int numberOfEvents) {
		this.setNumberOfEvents(numberOfEvents);
		<#list imp.columns as column>
		<#if column.type.lines != column.type.nullLines>
		<@f.field column/> = new ${column.type.primitiveType.name}[numberOfEvents];
		</#if>
		</#list>
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
	
	@Override
	public void read(Input input) throws IOException {
		int numberOfEvents = input.readInt(true);
		initFields(numberOfEvents);
		
		<#if imp.nullWidth gt 0>
		int nullBytesLength = input.readInt(true);
		byte [] nullBytes = input.readBytes(nullBytesLength);
		nullBits = Bits.asStore(nullBytes, 0, numberOfEvents*${imp.nullWidth});
		<#else>
		nullBits = Bits.noBits();
		</#if>
		for (int eventId = 0; eventId < numberOfEvents; eventId++) {
			<#list imp.columns as col>
			<#import "/com/bakdata/conquery/models/events/generation/types/${col.type.class.simpleName}.ftl" as t/>
			<#if col.type.nullLines == col.type.lines>
			//all values of ${col.name} are null
			<#elseif col.type.requiresExternalNullStore()>		
			if(this.has(eventId, ${col.position})) {
				this.<@f.set col/>(eventId, <@t.kryoDeserialization type=col.type/>);
			}
			<#else>
			this.<@f.set col/>(eventId, <@t.kryoDeserialization type=col.type/>);
			</#if>
			</#list>
		}
	}
	
	@Override
	public void writeContent(Output output) throws IOException {
		output.writeInt(getNumberOfEvents(), true);
		
		<#if imp.nullWidth gt 0>
		byte[] nullBitsAsBytes = Bits.asStore(nullBits.toByteArray()).toByteArray();
		output.writeInt(nullBitsAsBytes.length, true);
		output.write(nullBitsAsBytes);
		</#if>
		
		for (int event = 0; event < getNumberOfEvents(); event++) {					
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
}
