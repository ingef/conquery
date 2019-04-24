<#macro nullValue type>null</#macro>
<#macro kryoSerialization type>DeserHelper.writeBigDecimal(output, <#nested>)</#macro>
<#macro kryoDeserialization type>DeserHelper.readBigDecimal(input)</#macro>
<#macro nullCheck type><#nested/> == null</#macro>
<#macro majorTypeTransformation type><#nested></#macro>