<#macro nullValue type>java.lang.Float.NaN</#macro>
<#macro kryoSerialization type>output.writeFloat(<#nested/>)</#macro>
<#macro kryoDeserialization type>input.readFloat()</#macro>
<#macro nullCheck type>java.lang.Float.isNaN(<#nested/>)</#macro>
<#macro majorTypeTransformation type><#nested/></#macro>