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

	public Block_${suffix} createBlock(Import imp, List<Object[]> events) {
		BitStore nullBits = Bits.store(${imp.nullWidth}*events.size());
		Block_${suffix} block = new Block_${suffix}();
		block.setSize(events.size());
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
	
	public Block_${suffix} readBlock(Import imp, InputStream inputStream) throws IOException {
		try (SmallIn input = new SmallIn(inputStream)){
			int eventLength = input.readInt(true);
			int nullBytesLength = input.readInt(true);
			byte [] nullBytes = input.readBytes(nullBytesLength);
			
			BitStore nullBits = Bits.asStore(nullBytes, 0, eventLength*${imp.nullWidth});
			Block_${suffix} block = new Block_${suffix}();
			block.setNullBits(nullBits);
			block.setSize(eventLength);
			for (int eventId = 0; eventId < eventLength; eventId++) {
				<#list imp.columns as col>
				<#import "/com/bakdata/conquery/models/events/generation/types/${col.type.class.simpleName}.ftl" as t/>
				<#if col.type.nullLines == col.type.lines>
				//all values of ${col.name} are null
				<#elseif col.type.requiresExternalNullStore()>		
				if(block.has(eventId, ${col.position})) {
					block.<@f.set col/>(eventId, <@t.kryoDeserialization type=col.type/>);
				}
				<#else>
				block.<@f.set col/>(eventId, <@t.kryoDeserialization type=col.type/>);
				</#if>
				</#list>
			}
			return block;
		}
	}
}