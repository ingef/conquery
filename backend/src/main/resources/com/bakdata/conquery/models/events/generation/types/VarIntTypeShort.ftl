<#macro nullValue type>((short)${type.maxValue+1})</#macro>
<#macro kryoSerialization type>output.writeShort(<#nested/>)</#macro>
<#macro kryoDeserialization type>input.readShort()</#macro>
<#macro nullCheck type><#nested/> == <@nullValue type=type/></#macro>
<#macro majorTypeTransformation type>(int)<#nested></#macro>

<#macro unboxValue type> ((Short)<#nested>).shortValue() </#macro>