<#import "templates/template.html.ftl" as layout>
<@layout.layout>
	<@layout.kv k="Name" v=c.table.id/>
	<@layout.kv k="Label" v=c.table.label/>
	<@layout.kv k="Tags" v=c.table.tags?join(", ")/>
	<@layout.kv k="Entries" v=c.numberOfEntries/>
	<@layout.kc k="Columns">
		<table>
			<tr>
				<th>Label</th>
				<th>Name</th>
				<th>Type</th>
			</tr>
		<#list c.table.columns as column>
			<tr>
				<td>${column.label}</td>
				<td>${column.id}</td>
				<td>${column.type}</td>
			</tr>
		</#list>
		</table>
	</@layout.kc>
</@layout.layout>