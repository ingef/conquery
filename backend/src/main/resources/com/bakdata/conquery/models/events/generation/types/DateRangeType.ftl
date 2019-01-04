<#macro nullValue type>null</#macro>
<#macro kryoSerialization type>{
	CDateRange v = <#nested>;
	output.writeBoolean(v == null);
	if(v != null) {
		output.writeInt(v.getMinValue(), true);
		output.writeInt(v.getMaxValue(), true);
	}
}
</#macro>
<#macro kryoDeserialization type>input.readBoolean()?null:new CDateRange(input.readInt(true), input.readInt(true))</#macro>
<#macro nullCheck type><#nested/> == null</#macro>
<#macro majorTypeTransformation type><#nested></#macro>