<#import "templates/template.html.ftl" as layout>
<@layout.layout>
	<@layout.kid k="ID" v=c.table.id/>
	<@layout.kv k="Label" v=c.table.label/>
	<@layout.kv k="Entries" v=c.numberOfEntries?string.number/>
	<@layout.kv k="Dictionaries" v=layout.si(c.dictionariesSize)+"B"/>
	<@layout.kv k="CBlocks" v=layout.si(c.getCBlocksSize())+"B"/>
	<@layout.kv k="Size" v=layout.si(c.size)+"B"/>
	<@layout.kc k="Tags">
		<ul>
		<#list c.imports as import>
			<li>
				<a href="/admin-ui/datasets/${c.table.dataset.id}/tables/${c.table.id}/import/${import.id}">${import.name} (${import.numberOfEntries})</a>
				<a href="" onclick="event.preventDefault(); rest('/admin/datasets/${c.table.dataset.id}/tables/${c.table.id}/imports/${import.id}', {method : 'delete'}).then(() => location.reload());"><i class="fas fa-trash-alt text-danger"></i></a>
			</li>
		</#list>
		</ul>
	</@layout.kc>
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
				<td>
				    <ul>
                        <#if column.sharedDictionary??>
                            <li style="display: inline;">Shared Dictionary  ${("<code>"?no_esc+column.sharedDictionary+"</code>"?no_esc)!}</li>
                        </#if>
                        <#if column.secondaryId??>
                            <li style="display: inline;">${column.secondaryId}</li>
                        </#if>
                    </ul>
				</td>
			</tr>
		</#list>
		</table>
	</@layout.kc>
</@layout.layout>