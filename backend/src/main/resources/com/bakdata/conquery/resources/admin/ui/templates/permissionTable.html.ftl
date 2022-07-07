<#macro permissionTable ownerId permissions>
    <div class="table-responsive">
    <table class="table table-sm table-striped">
        <thead>
            <tr>
            <th scope="col">Domains</th>
            <th scope="col">Abilities</th>
            <th scope="col">Targets</th>
            <th scope="col">Creation Time</th>
            <th></th>
            </tr>
        </thead>
        <tbody>
            <#list permissions as permission>
                <#assign domains=permission.getDomains()>
                <#assign abilities=permission.getAbilities()>
                <#assign targets=permission.getTargets()>
                <tr>
                    <td>
                        <#if domains?has_content>
                            <#list domains as domain>${domain} </#list>
                        </#if>
                    <td>
                        <#if abilities?has_content>
                            <ul class="list-group">
                                <#list abilities as ability>
                                    <li class="list-group-item p-1">
                                        ${ability}
                                    </li>
                                </#list>
                            </ul>
                        </#if>
                    </td>
                    <td>
                        <#if targets?has_content>
                            <ul class="list-group">
                                <#list targets as target>
                                    <li class="list-group-item p-1">
                                        ${target}
                                    </li>
                                </#list>
                            </ul>
                        </#if>
                    </td>
                    <td>${permission.creationTime}</td>
                    <td><a href="#" onclick="handleDeletePermission('${permission.rawPermission}')"><i class="fas fa-trash-alt text-danger"></i></a></td>
                </tr>
            </#list>
        </tbody>
    </table>
    </div>
    <script type="application/javascript">
    function handleDeletePermission(permission){
        event.preventDefault();
        fetch(
            '/admin/permissions/${ownerId}',
            {
                method: 'delete',
                credentials: 'same-origin',
                headers: {'Content-Type': 'text/plain'},
                body: permission
            })
            .then(function(){location.reload()});
    }
    </script>
</#macro>