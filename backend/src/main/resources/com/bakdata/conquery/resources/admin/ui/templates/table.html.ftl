<#macro table columns items deleteButton="" link="">
    <div class="row">
        <div class="col m-0 table-responsive">
            <table class="table table-sm table-striped">
                <thead>
                    <tr>
                        <#list columns as column>
                          <#if column == "actions">
                            <th class="text-right" scope="col">${column}</th>
                          <#else>
                            <th scope="col">${column}</th>
                          </#if>
                        </#list>
                    </tr>
                </thead>
                <tbody>
                    <#if items?size == 0>
                        <tr>
                            <td colspan="${columns?size}" class="text-center">No items found</td>
                        </tr>
                    </#if>
                    <#list items as item>
                        <tr>
                            <#list columns as column>
                                <#if column == "id" && link?has_content>
                                    <td scope="row"><a href="${link}${item.id}">${item.id}</a></td>
                                <#elseif column == "initialized">
                                    <td> <#if item.initialized() ><i class="fas fa-check" alt="In use"></i><#else><i class="fas fa-moon" alt="Not used by any select"></i></i></#if> </td>
                                <#elseif column == "actions">
                                    <td class="text-right">
                                        <#if deleteButton?is_macro>
                                            <@deleteButton id="${item.id}"/>
                                        </#if>
                                    </td>
                                <#else>
                                    <td scope="row">${item[column]}</td>
                                </#if>
                            </#list>
                        </tr>
                    </#list>
                </tbody>
            </table>
        </div>
    </div>
</#macro>