<#macro nullValue type>((byte)${(type.maxValue+1)?c})</#macro>
<#macro kryoSerialization type>output.writeByte(<#nested/>)</#macro>
<#macro kryoDeserialization type>input.readByte()</#macro>
<#macro nullCheck type><#nested/> == <@nullValue type=type/></#macro>
<#macro majorTypeTransformation type>(long)<#nested></#macro>

<#macro unboxValue> ((Byte)<#nested>).byteValue() </#macro>