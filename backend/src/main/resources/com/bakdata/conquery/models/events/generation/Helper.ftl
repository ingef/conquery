<#macro field col>col${safeName(col.name)?cap_first}</#macro>
<#macro set col>setCol${safeName(col.name)?cap_first}</#macro>
<#macro get col>getCol${safeName(col.name)?cap_first}</#macro>
<#macro getMajor col>getCol${safeName(col.name)?cap_first}AsMajor</#macro>