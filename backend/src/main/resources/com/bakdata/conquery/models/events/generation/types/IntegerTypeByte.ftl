<#macro nullValue type><#stop "can't store null"/></#macro>
<#macro kryoSerialization type>output.writeByte(<#nested/>)</#macro>
<#macro kryoDeserialization type>input.readByte()</#macro>
<#macro nullCheck type><#stop "Tried to generate a null check that is not generateable"/></#macro>
<#macro majorTypeTransformation type>(long)<#nested></#macro>

<#macro unboxValue> ((Byte)<#nested>).byteValue() </#macro>