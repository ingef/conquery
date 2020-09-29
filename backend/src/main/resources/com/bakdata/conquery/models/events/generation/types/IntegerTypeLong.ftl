<#macro nullValue type>
<#if type.maxValue?? >
		${type.maxValue + 1}L
<#else>
		Long.MAX_VALUE
</#if>
</#macro>
<#macro kryoSerialization type>
		<#if type.minValue?? && type.minValue gte 0>
				output.writeLong(<#nested/>, true)
		<#else>
				output.writeLong(<#nested/>, false)
		</#if>
</#macro>
<#macro kryoDeserialization type>
		<#if type.minValue?? && type.minValue gte 0>
				input.readLong(true)
		<#else>
				input.readLong(false)
		</#if>
</#macro>
<#macro nullCheck type><#nested/> == <@nullValue type=type/></#macro>
<#macro majorTypeTransformation type><#nested></#macro>

<#macro unboxValue type> ((Long)<#nested>).longValue() </#macro>
