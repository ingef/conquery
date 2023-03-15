<#macro infoCard labels values title="" subtitle="" class="">
  <div class="card ${class}">
    <div class="card-body">
      <#if title?has_content>
        <h5 class="card-title">${title}</h5>
      </#if>
      <#if subtitle?has_content>
        <h6 class="card-subtitle text-muted mb-2">${title}</h6>
      </#if>
      <div class="card-text" style="display: grid; grid-template-columns: auto auto; gap: 1rem 3rem;">
        <#list values as value>
          <div>
            <#if (value?index < labels?size)>
              <div class="text-secondary">${labels[value?index]}</div>
            </#if>
            <#if value?is_macro>
              <@value />
            <#else>
              <div>${value}</div>
            </#if>
          </div>
        </#list>
      </div>
    </div>
  </div>
</#macro>