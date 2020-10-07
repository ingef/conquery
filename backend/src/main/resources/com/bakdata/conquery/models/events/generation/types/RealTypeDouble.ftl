<#macro nullValue type>java.lang.Double.NaN</#macro>
<#macro kryoSerialization type>output.writeDouble(<#nested/>)</#macro>
<#macro kryoDeserialization type>input.readDouble()</#macro>
<#macro nullCheck type>java.lang.Double.isNaN(<#nested/>)</#macro>
<#macro majorTypeTransformation type><#nested></#macro>