<#macro styledTable columns items deleteButton link="">
    <div>
        <table class="table table-bordered table-hover">
            <thead>
                <tr>
                    <#list columns as column>
                        <th scope="col">${column}</th>
                    </#list>
                    <th>actions</th>
                </tr>
            </thead>
            <tbody>
                <#list items as item>
                    <tr>
                        <#list columns as column>
                            <#if column == "id" && link?has_content>
                                <td scope="row"><a href="${link}${item.id}">${item.id}</a></td>
                            <#elseif column == "initialized">
                                <td> <#if item.initialized() ><i class="fas fa-check" alt="In use"></i><#else><i class="fas fa-moon" alt="Not used by any select"></i></i></#if> </td>
                            <#else>
                                <td scope="row">${item[column]}</td>
                            </#if>
                        </#list>
                        <td>
                            <#if link?has_content>
                                <a href="${link}${item.id}"><i class="fas fa-edit"></i></a>
                            </#if>
                            <@deleteButton id="${item.id}"/>
                        </td>
                    </tr>
                </#list>
            </tbody>
        </table>
    </div>
</#macro>