<#macro nullValue type>${(type.maxValue+1)?c}</#macro>
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
<#macro nullCheck type><#nested/> == <@nullValue type=type/></#macro>
<#macro majorTypeTransformation type>(long)<#nested></#macro>

<#macro unboxValue> ((Integer)<#nested>).intValue() </#macro>