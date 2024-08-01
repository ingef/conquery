<#macro copyableText text copyContent link="">
  <div
    class="d-flex flex-row align-items-center"
    style="gap: 0.25rem;"
  >
    <div>
      <#if link?has_content>
        <a href="${link}">${text}</a>
      <#else>
       ${text}
      </#if>
    </div>
    <div
      style="cursor: pointer;"
      data-toggle="tooltip"
      data-placement="bottom"
      title="Copy"
      onclick="navigator.clipboard.writeText('${copyContent}'); $(this).tooltip('hide').attr('data-original-title', 'Copied').tooltip('show');"
      onmouseout="$(this).attr('data-original-title', 'Copy')"
    >
      <i class="fa fa-clipboard"></i>
    </div>
  </div>
</#macro>