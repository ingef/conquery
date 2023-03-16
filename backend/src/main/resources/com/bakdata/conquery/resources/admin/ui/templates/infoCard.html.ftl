<#macro infoCard labels values title="" subtitle="" links={} class="" style="" gridTemplateColumns="auto auto">
  <div class="card ${class}" style="${style}">
    <div class="card-body">
      <#if title?has_content>
        <h5 class="card-title">${title}</h5>
      </#if>
      <#if subtitle?has_content>
        <h6 class="card-subtitle text-muted mb-2">${title}</h6>
      </#if>
      <div class="card-text text-break" style="display: grid; grid-template-columns: ${gridTemplateColumns}; gap: 1rem 3rem;">
        <#list values as value>
          <#if value?is_macro || value?has_content>
            <div>
              <#if (value?index < labels?size)>
                <div class="text-secondary">${labels[value?index]}</div>
              </#if>
              <#if value?is_macro>
                <@value />
              <#else>
                <#if links?keys?seq_contains(labels[value?index])>
                  <div><a href="${links[labels[value?index]]}">${value}</a></div>
                <#else>
                  <div>${value}</div>
                </#if>
              </#if>
            </div>
          </#if>
        </#list>
      </div>
    </div>
  </div>
</#macro>