<#macro kryoSerialization type>output.writeInt(<#nested>, true)</#macro>
<#macro kryoDeserialization type>input.readInt(true)</#macro>
<#macro majorTypeTransformation type><#nested></#macro>

<#macro nullValue type>${(type.maxValue+1)?c}</#macro>
<#macro nullCheck type><#nested/> == <@nullValue type=type/></#macro>
<#macro unboxValue> ((Integer)<#nested>).intValue() </#macro>