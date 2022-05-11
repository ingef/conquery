<#macro snippet permissionOwner linkBase>
    <a   class="text-dark text-nowrap" href="${linkBase}${permissionOwner.id}" tabindex="0" data-toggle="popover" data-html="true" data-trigger="focus" title="${permissionOwner.id}" data-content="<div><ul><#list permissionOwner.permissions as permission><li>${permission}</li></#list></ul></div>"><span>
            ${permissionOwner.label}
        </span>
    </a>
</#macro>