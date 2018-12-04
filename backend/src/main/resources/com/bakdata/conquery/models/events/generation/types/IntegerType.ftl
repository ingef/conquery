<#macro nullValue type><#stop "can't store null"/></#macro>
<#macro kryoSerialization type>
	<#if type.minValue gte 0>
		output.writeLong(<#nested/>, true)
	<#else>
		output.writeLong(<#nested/>, false)
	</#if>
</#macro>
<#macro kryoDeserialization type>
	<#if type.minValue gte 0>
		input.readLong(true)
	<#else>
		input.readLong(false)
	</#if>
</#macro>
<#macro nullCheck type><#stop "Tried to generate a null check that is not generateable"/></#macro>
<#macro majorTypeTransformation type><#nested></#macro>

<#macro unboxValue> ((Long)<#nested>).longValue() </#macro>