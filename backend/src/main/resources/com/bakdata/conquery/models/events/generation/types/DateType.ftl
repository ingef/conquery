<#macro kryoSerialization type>output.writeInt(<#nested>, true)</#macro>
<#macro kryoDeserialization type>input.readInt(true)</#macro>
<#macro majorTypeTransformation type><#nested></#macro>

<#macro nullValue type><#stop "can't store null"/></#macro>
<#macro nullCheck type><#stop "Tried to generate a null check that is not generateable"/></#macro>
