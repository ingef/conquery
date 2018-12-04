<#import "templates/template.html.ftl" as layout>
<@layout.layout>
	<div class="row">
		<div class="col">
			<#list ctx.namespaces.slaves as key,slave>
				<span class="badge badge-pill <#if slave.connected??>badge-success<#else>badge-danger</#if>">${key}</span>
			</#list>
		</div>
	</div>
</@layout.layout>