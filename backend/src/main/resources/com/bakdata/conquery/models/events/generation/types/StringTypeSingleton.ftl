<#macro nullValue type>false</#macro>
<#macro kryoSerialization type>output.writeBoolean(<#nested>)</#macro>
<#macro kryoDeserialization type>input.readBoolean()</#macro>
<#macro nullCheck type><#nested>==false</#macro>
<#macro majorTypeTransformation type>0</#macro>