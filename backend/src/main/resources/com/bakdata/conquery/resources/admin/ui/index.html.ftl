<#import "templates/template.html.ftl" as layout>
<@layout.layout>
	<div class="row">
		<div class="col">
			<#list ctx.namespaces.shardNodes as key,shardNode>
				<span class="badge badge-pill <#if shardNode.connected??>badge-success<#else>badge-danger</#if>">${key}</span>
			</#list>
		</div>
	</div>
</@layout.layout>