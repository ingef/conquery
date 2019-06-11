<#import "/com/bakdata/conquery/models/events/generation/Helper.ftl" as f/>
package com.bakdata.conquery.models.events.generation;

import java.io.InputStream;
import java.io.IOException;

import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.events.generation.BlockFactory;
import com.bakdata.conquery.io.DeserHelper;
import java.math.BigDecimal;
import com.google.common.collect.Range;

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

public class BlockFactory_${suffix} extends BlockFactory {

	public Bucket_${suffix} createBlock(Import imp, List<Object[]> events) {
		BitStore nullBits = Bits.store(${imp.nullWidth}*events.size());
		Bucket_${suffix} block = new Bucket_${suffix}(0, imp, events.size(), new int[]{0});
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
	
	public Bucket_${suffix} construct(int bucketNumber, Import imp, int numberOfEvents, int[] offsets) {
		return new Bucket_${suffix}(int bucketNumber, Import imp, int numberOfEvents, int[] offsets);
	}
}