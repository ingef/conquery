<#macro permissionTable ownerId permissions>
    <table class="table table-striped">
        <thead>
            <tr>
            <th scope="col">Type</th>
            <th scope="col">Abilities</th>
            <th scope="col">Target</th>
            <th></th>
            </tr>
        </thead>
        <tbody>
            <#list permissions as permission>
                <tr>
                    <td>${permission.left.type}</td>
                    <td>
                        <#if permission.left.abilities?has_content>
                            <#list permission.left.abilities as ability>${ability} </#list>
                        </#if>
                    </td>
                    <td>
                        <#if permission.left.target?has_content>
                            <#list permission.left.target as tar> ${tar} </#list>
                        </#if>
                    </td>
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