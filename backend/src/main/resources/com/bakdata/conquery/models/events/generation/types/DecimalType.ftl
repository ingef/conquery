<#macro nullValue type>null</#macro>
<#macro kryoSerialization type>KryoHelper.writeBigDecimal(output, <#nested>)</#macro>
<#macro kryoDeserialization type>KryoHelper.readBigDecimal(input)</#macro>
<#macro nullCheck type><#nested/> == null</#macro>
<#macro majorTypeTransformation type><#nested></#macro>