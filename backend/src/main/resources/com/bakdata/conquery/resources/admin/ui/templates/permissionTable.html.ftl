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
                    <td>${permission.class.getSimpleName()}</td>
                    <td>${permission.getTarget().toString()}</td>
                    <td><#list permission.getAbilities() as ability>${ability} </#list></td>
                    <td><a href="" onclick="event.preventDefault(); fetch('/admin/permissions/${ownerId}',{method: 'delete', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({type: 'DATASET_PERMISSION', abilities: '${permission.getAbilities()?first}', instanceId:'${permission.getTarget()}'})}).then(function(){location.reload()});"><i class="fas fa-trash-alt text-danger"></i></a></td>
                </tr>
            </#list>
        </tbody>
    </table>
</#macro>