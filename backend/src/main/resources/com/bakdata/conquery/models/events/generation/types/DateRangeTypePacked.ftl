<#macro nullValue type><#stop "can't store null"/></#macro>
<#macro kryoSerialization type>
	<#if type.minValue gte 0>
		output.writeInt(<#nested/>, true)
	<#else>
		output.writeInt(<#nested/>, false)
	</#if>
</#macro>
<#macro kryoDeserialization type>
	<#if type.minValue gte 0>
		input.readInt(true)
	<#else>
		input.readInt(false)
	</#if>
</#macro>
<#macro nullCheck type><#stop "can't store null"/></#macro>
<#macro majorTypeTransformation type>CDateRange.of(PackedUnsigned1616.getLeft(<#nested>)+${type.minValue}, PackedUnsigned1616.getRight(<#nested>)+${type.minValue?c})</#macro>

<#macro unboxValue type> ((Integer)<#nested>).intValue() </#macro>