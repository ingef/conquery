<#macro breadcrumbs labels links class="">
  <nav aria-label="breadcrumb" class="${class}">
    <ol class="breadcrumb">
      <#list labels as label>
        <#--  last element as active page  -->
        <#assign liAttributes>class="breadcrumb-item"</#assign>
        <#if (label?index == labels?size - 1)>
          <#assign liAttributes>class="breadcrumb-item active" aria-current="page"</#assign>
        </#if>

        <li ${liAttributes}>
          <#if (label?index < links?size)>
            <a href="${links[label?index]}">${label}</a>
          <#else>
            <span>${label}</span>
          </#if>
        </li>
      </#list>
    </ol>
  </nav>
</#macro>