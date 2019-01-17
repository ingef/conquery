<#macro permissionTable permissions>
    <table class="table table-striped">
        <thead>
            <tr>
            <th scope="col">Type</th>
            <th scope="col">Target</th>
            <th scope="col">Abilities</th>
            </tr>
        </thead>
        <tbody>
            <#list permissions as permission>
                <tr>
                    <td>${permission.getClass().getSimpleName()}</td>
                    <td>${permission.getTarget().toString()}</td>
                    <td><#list permission.getAbilities() as ability>${ability} </#list></td>
                </tr>
            </#list>
        </tbody>
    </table>
</#macro>