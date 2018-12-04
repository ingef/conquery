<#macro nullValue type>-1</#macro>
<#macro kryoSerialization type>output.writeInt(<#nested/>, true)</#macro>
<#macro kryoDeserialization type>input.readInt(true)</#macro>
<#macro nullCheck type><#nested/> == -1</#macro>
<#macro majorTypeTransformation type><#nested></#macro>

<#macro unboxValue> ((Integer)<#nested>).intValue() </#macro>