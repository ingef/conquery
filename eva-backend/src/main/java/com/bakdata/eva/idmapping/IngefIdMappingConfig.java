package com.bakdata.eva.idmapping;

import org.apache.commons.lang3.StringUtils;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.identifiable.mapping.CsvEntityId;
import com.bakdata.conquery.models.identifiable.mapping.DefaultIdMappingAccessor;
import com.bakdata.conquery.models.identifiable.mapping.ExternalEntityId;
import com.bakdata.conquery.models.identifiable.mapping.IdMappingAccessor;
import com.bakdata.conquery.models.identifiable.mapping.IdMappingConfig;
import com.bakdata.conquery.models.identifiable.mapping.PersistentIdMap;
import com.bakdata.conquery.models.worker.Namespace;

@CPSType(base= IdMappingConfig.class, id="INGEF_ID_MAPPING")
public class IngefIdMappingConfig extends IdMappingConfig {
	@Override
	public IdMappingAccessor[] getIdAccessors() {
		return new IdMappingAccessor[]{
			new DefaultIdMappingAccessor(this, new int[]{0,3}) {
				@Override
				public CsvEntityId getFallbackCsvId(String[] reorderedCsvLine) {
					return null;
				}
			},
			new DefaultIdMappingAccessor(this, new int[]{1,3}) {
				@Override
				public CsvEntityId getFallbackCsvId(String[] reorderedCsvLine) {
					return null;
				}
			},
			new DefaultIdMappingAccessor(this, new int[]{2,3}) {
				@Override
				public CsvEntityId getFallbackCsvId(String[] reorderedCsvLine) {
					return new CsvEntityId(reorderedCsvLine[1]+"|"+reorderedCsvLine[0]);
				}
			},
		};
	}

	@Override
	public String[] getPrintIdFields() {
		return new String[]{"eGK_Nr","KV_Nummer","PID","H2IK"};
	}

	@Override
	public String[] getHeader() {
		return new String[]{"id", "eGK_Nr","KV_Nummer","PID","H2IK"};
	}
	
	@Override
	public ExternalEntityId toExternal(CsvEntityId csvEntityId, Namespace namespace) {
		PersistentIdMap mapping = namespace.getStorage().getIdMapping();
		if (mapping != null){
			ExternalEntityId result = mapping.getCsvIdToExternalIdMap().get(csvEntityId);
			if(result != null) {
				return result;
			}
		}
		String[] parts = StringUtils.split(csvEntityId.getCsvId(), "|");
		if(parts.length == 2) {
			return new ExternalEntityId(new String[] {null,null,parts[1],parts[0]});
		}
		else {
			return new ExternalEntityId(new String[] {null,null,csvEntityId.getCsvId(),""});
		}
	}
}
