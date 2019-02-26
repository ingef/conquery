<#import "templates/template.html.ftl" as layout>
<@layout.layout>
	Id Mapping
	<div class="row">
		<div class="col">Internal Id</div>
		<#list c.externalIdFields as externalIdField>
			<div class="col">externalIdField</div>
		</#list>
	</div>
	<#list c.data as key, val>
		<div class="row">
		<div class="col">${key}</div>
		<#list val as externalIdPart>
			<div class="col">${externalIdPart}</div>
		</#list>
		</div>
	</#list>
</@layout.layout>