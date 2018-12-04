<#macro nullValue type><#stop "can't store null"/></#macro>
<#macro kryoSerialization type>output.writeBoolean(<#nested>)</#macro>
<#macro kryoDeserialization type>input.readBoolean()</#macro>
<#macro nullCheck type><#stop "Tried to generate a null check that is not generateable"/></#macro>
<#macro majorTypeTransformation type><#nested></#macro>