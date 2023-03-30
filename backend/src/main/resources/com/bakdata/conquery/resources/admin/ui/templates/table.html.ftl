<#import "copyableText.html.ftl" as copyableText>

<#macro table columns items deleteButton="" link="" renderers={}>
  <div class="row">
    <div class="col m-0 table-responsive">
      <table class="table table-sm table-striped text-break">
        <thead class="text-nowrap">
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
                <#if renderers?keys?seq_contains(column)>
                  <td scope="row"><@renderers[column] id="${item.id}" /></td>
                <#elseif column == "id">
                  <#assign idLabel="${item.id}" />
                  <#if item?keys?seq_contains('name')>
                    <#assign idLabel="${item.name}" />
                  </#if>
                  <#assign idLink="" />
                  <#if link?has_content>
                    <#assign idLink="${link}${item.id}" />
                  </#if>

                  <td scope="row">
                    <@copyableText.copyableText text="${idLabel}" copyContent="${item.id}" link="${idLink}" />
                  </td>
                <#elseif column == "initialized">
                  <td> <#if item.initialized() ><i class="fas fa-check" alt="In use"></i><#else><i class="fas fa-moon" alt="Not used by any select"></i></i></#if> </td>
                <#elseif column == "actions">
                  <td class="text-right">
                    <#if deleteButton?is_macro>
                      <@deleteButton id="${item.id}"/>
                    <#else>
                      <#stop "Expected macro for deleteButton">
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