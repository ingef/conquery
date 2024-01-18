<#import "templates/template.html.ftl" as layout>
<@layout.layout>
	Id Mapping
	<#list c as key, val>
		<div class="row">
		<div class="col">${key}</div>
		<#list val.externalId as externalIdPart>
			<div class="col">${externalIdPart}</div>
		</#list>
		</div>
	</#list>
</@layout.layout>