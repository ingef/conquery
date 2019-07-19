<#macro permissionTable permissions>
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
                    <td><a href="" onclick="event.preventDefault(); fetch('/admin/permissions/${permission.id}',{method: 'delete'}).then(function(){location.reload()});"><i class="fas fa-trash-alt text-danger"></i></a></td>
                </tr>
            </#list>
        </tbody>
    </table>
</#macro>