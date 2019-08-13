<#import "templates/template.html.ftl" as layout>
<@layout.layout>
	<@layout.kid k="Name" v=c.table.id/>
	<@layout.kv k="Label" v=c.table.label/>
	<@layout.kv k="Tags" v=c.table.tags?join(", ")/>
	<@layout.kv k="Entries" v=c.numberOfEntries/>
	<@layout.kc k="Columns">
		<table class="headed-table">
			<tr>
				<th>Label</th>
				<th>ID</th>
				<th>Type</th>
				<th>Infos</th>
			</tr>
		<#list c.table.columns as column>
			<tr>
				<td>${column.label}</td>
				<td><code>${column.id}</code></td>
				<td>${column.type}</td>
				<td>${("Shared Dictionary <code>"?no_esc+column.sharedDictionary+"</code>"?no_esc)!}</td>
			</tr>
		</#list>
		</table>
	</@layout.kc>
</@layout.layout>