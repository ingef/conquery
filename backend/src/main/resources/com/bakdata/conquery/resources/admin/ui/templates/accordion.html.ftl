<#macro accordionGroup>
  <div class="accordion my-2">
    <#nested />
  </div>
</#macro>
<#macro accordion summary infoText="">
  <div class="card accordion-card" data-test-id="accordion-${summary}">
    <div
      class="card-header d-inline-flex justify-content-between"
      style="user-select: none; cursor: pointer;"
      onclick="$(this).next().collapse('toggle');"
    >
      <div>
        <h5 class="p-0 m-0">${summary}</h5>
      </div>
      <div class="row pr-3">
        <div class="text-secondary">${infoText}</div>
      </div>
    </div>
    <div class="collapse">
      <div class="card-body py-0">
        <#nested />
      </div>
    </div>
  </div>
</#macro>