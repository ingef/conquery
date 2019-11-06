<#macro permissionTable ownerId permissions>
    <table class="table table-striped">
        <thead>
            <tr>
            <th scope="col">Type</th>
            <th scope="col">Target</th>
            <th scope="col">Abilities</th>
            <th></th>
            </tr>
        </thead>
        <tbody>
            <#list permissions as permission>
                <tr>
                    <td>${permission.getLeft().getType()}</td>
                    <td>
                            ${permission.getLeft().getTarget()}</td>
                    <td>
                            <#list permission.getLeft().getAbilities() as ability>${ability} </#list></td>
                    <td><a href="#" onclick="handleDeletePermission(${permission.getRight()})"><i class="fas fa-trash-alt text-danger"></i></a></td>
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
                 headers: {'Content-Type': 'application/json'},
                 body: JSON.stringify(permission)}).then(function(){location.reload()});
    }
    </script>
</#macro>