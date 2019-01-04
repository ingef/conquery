<#macro nullValue type><#stop "can't store null"/></#macro>
<#macro kryoSerialization type>output.writeShort(<#nested/>)</#macro>
<#macro kryoDeserialization type>input.readShort()</#macro>
<#macro nullCheck type><#stop "Tried to generate a null check that is not generateable"/></#macro>
<#macro majorTypeTransformation type>(long)<#nested></#macro>