<#import "/com/bakdata/conquery/models/events/generation/Helper.ftl" as f/>
package com.bakdata.conquery.models.events.generation;

import java.io.InputStream;
import java.io.IOException;

import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.events.generation.BlockFactory;
import com.bakdata.conquery.io.DeserHelper;
import com.bakdata.conquery.models.preproc.PreprocessedHeader;
import com.bakdata.conquery.models.preproc.PPColumn;
import com.bakdata.conquery.models.dictionary.DictionaryMapping;
import java.math.BigDecimal;
import com.google.common.collect.Range;

import java.time.LocalDate;

import java.lang.Integer;
import com.bakdata.conquery.models.common.daterange.CDateRange;

import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

import com.tomgibara.bits.BitStore;
import com.tomgibara.bits.BitWriter;
import com.tomgibara.bits.Bits;
import com.bakdata.conquery.util.PackedUnsigned1616;
import com.bakdata.conquery.models.common.CQuarter;
import com.google.common.primitives.Ints;

import java.util.Arrays;
import it.unimi.dsi.fastutil.ints.IntList;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.entity.Entity;

public class BlockFactory_${suffix} extends BlockFactory {

	@Override
	public Bucket_${suffix} create(Import imp, List<Object[]> events) {
		BitStore nullBits = Bits.store(${imp.nullWidth}*events.size());
		Bucket_${suffix} block = construct(0, imp, new int[]{0});
		block.initFields(events.size());
		for(int event = 0; event < events.size(); event++){
			<#list imp.columns as col>
			<#import "/com/bakdata/conquery/models/events/generation/types/${col.type.class.simpleName}.ftl" as t/>
			//${col.name} : ${col.type.class.simpleName}
			<#if col.type.lines == col.type.nullLines>
			<#-- do nothing, column is null everywhere-->
			<#else>
			if(events.get(event)[${col_index}]==null){
			<#if col.type.canStoreNull()> //TODO implement this with t.nullValue?has_content, else throw exception to consolidate concerns of parsing into file.
				block.<@f.set col/>(event, <@t.nullValue type=col.type/>);	
			<#else>
				nullBits.setBit(${imp.nullWidth}*event+${col.nullPosition}, true);
			</#if>
			}
			else{
				${col.type.primitiveType.name} value;
				<#if t.unboxValue??>
				value = <@t.unboxValue col.type>events.get(event)[${col_index}]</@t.unboxValue>;
				<#else>
				value = (${col.type.primitiveType.name}) events.get(event)[${col_index}];
				</#if>
				block.<@f.set col/>(event, value);
			}
			</#if>
			</#list>
		}
		block.setNullBits(nullBits);
		return block;
	}
	
	@Override
	public Bucket_${suffix} construct(int bucketNumber, Import imp, int[] offsets) {
		return new Bucket_${suffix}(bucketNumber, imp, offsets);
	}
	
	@Override
	public Bucket_${suffix} combine(IntList includedEntities, Bucket[] buckets) {
		int[] order = IntStream
			.range(0, includedEntities.size())
			.boxed()
			.sorted(Comparator.comparing(includedEntities::getInt))
			.mapToInt(Integer::intValue)
			.toArray();
		int[] offsets = new int[${bucketSize}];
		int bucketNumber = Entity.getBucket(includedEntities.getInt(0), ${bucketSize});
		Arrays.fill(offsets, -1);
		int offset = 0;
		for(int index : order) {
			offsets[includedEntities.getInt(index) - ${bucketSize}*bucketNumber]=offset;
			offset+=buckets[index].getNumberOfEvents();
		}
		
		Bucket_${suffix} result = construct(
			bucketNumber,
			buckets[0].getImp(),
			offsets
		);
		result.initFields(Arrays.stream(buckets).mapToInt(Bucket::getNumberOfEvents).sum());
		BitStore bits = Bits.store(${imp.nullWidth}*result.getNumberOfEvents());
		offset = 0;
		for(int index : order) {
			Bucket_${suffix} bucket = (Bucket_${suffix})buckets[index];
			bits.setStore(
				offset*${imp.nullWidth}, 
				bucket.getNullBits().rangeTo(bucket.getNumberOfEvents()*${imp.nullWidth})
			);
			for(int event =0;event<bucket.getNumberOfEvents();event++) {
				<#list imp.columns as column>
				<#if column.type.lines != column.type.nullLines>
				result.<@f.set column/>(offset, bucket.<@f.get column/>(event));
				</#if>
				</#list>
				offset++;
			}
		}
		result.setNullBits(bits);
		return result;
	}
	
	@Override
	public Bucket_${suffix} adaptValuesFrom(int bucketNumber, Import imp, Bucket source, PreprocessedHeader header) {
		Bucket_${suffix} result = construct(bucketNumber, imp, source.getOffsets());
		BitStore nullBits = Bits.store(${imp.nullWidth}*source.getNumberOfEvents());
		result.initFields(source.getNumberOfEvents());
		for(int event = 0; event < source.getNumberOfEvents(); event++){
			<#list imp.columns as col>
			<#import "/com/bakdata/conquery/models/events/generation/types/${col.type.class.simpleName}.ftl" as t/>
			//${col.name} : ${col.type.class.simpleName}
			<#if col.type.lines == col.type.nullLines>
			<#-- do nothing, column is null everywhere-->
			<#else>
			if(!source.has(event, ${col_index})) {
			<#if col.type.canStoreNull()> //TODO implement this with t.nullValue?has_content, else throw exception to consolidate concerns of parsing into file.
				result.<@f.set col/>(event, <@t.nullValue type=col.type/>);	
			<#else>
				nullBits.setBit(${imp.nullWidth}*event+${col.nullPosition}, true);
			</#if>
			}
			else {
			<#if col.type.typeId.name()=="STRING">
				PPColumn ppColumn = header.getColumns()[${col_index}];
				if(ppColumn.getTransformer() != null) {
					${col.type.typeId.primitiveType.name} majorValue = (${col.type.typeId.boxedType.name})source.getAsObject(event, ${col_index});
					
					majorValue = ppColumn.getValueMapping().getSource2TargetMap()[majorValue];
	
					<#if t.unboxValue??>
					${col.type.primitiveType.name} value = <@t.unboxValue col.type>ppColumn.getTransformer().transform(majorValue)</@t.unboxValue>;
					<#else>
					${col.type.primitiveType.name} value = (${col.type.primitiveType.name})ppColumn.getTransformer().transform(majorValue);
					</#if>
					
					result.<@f.set col/>(event, value);
				}
				else {
					result.<@f.set col/>(event, (${col.type.primitiveType.name})source.getRaw(event, ${col_index}));
				}
			<#else>
				result.<@f.set col/>(event, (${col.type.primitiveType.name})source.getRaw(event, ${col_index}));
			</#if>
			}
			</#if>
			</#list>
		}
		result.setNullBits(nullBits);
		return result;
	}
}