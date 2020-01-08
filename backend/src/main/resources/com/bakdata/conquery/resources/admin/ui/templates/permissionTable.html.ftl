<#macro permissionTable ownerId permissions>
    <table class="table table-striped">
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
                <tr>
                    <td>
                        <#if permission.left.domains?has_content>
                            <#list permission.left.domains as domain>${domain} </#list>
                        </#if>
                    <td>
                        <#if permission.left.abilities?has_content>
                            <#list permission.left.abilities as ability>${ability} </#list>
                        </#if>
                    </td>
                    <td>
                        <#if permission.left.targets?has_content>
                            <#list permission.left.targets as target> ${target} </#list>
                        </#if>
                    </td>
                    <td>${permission.left.creationTime?datetime}</td>
                    <td><a href="#" onclick="handleDeletePermission('${permission.right}')"><i class="fas fa-trash-alt text-danger"></i></a></td>
                </tr>
            </#list>
        </tbody>
    </table>
    <script type="application/javascript">
    function handleDeletePermission(permission){
        event.preventDefault();
        fetch(
            '/admin/permissions/${ownerId}',
            {
                method: 'delete',
                 headers: {'Content-Type': 'text/plain'},
                 body: permission}).then(function(){location.reload()});
    }
    </script>
</#macro>