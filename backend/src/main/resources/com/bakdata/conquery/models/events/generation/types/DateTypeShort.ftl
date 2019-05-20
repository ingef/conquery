<#macro kryoSerialization type>output.writeShort(<#nested/>)</#macro>
<#macro kryoDeserialization type>input.readShort()</#macro>
<#macro majorTypeTransformation type>(int)<#nested></#macro>

<#macro nullValue type>((short) ${(type.maxValue+1)?c})</#macro>
<#macro nullCheck type><#nested/> == <@nullValue type=type/></#macro>
<#macro unboxValue> ((Short)<#nested>).shortValue() </#macro>