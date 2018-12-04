<#import "templates/template.html.ftl" as layout>
<@layout.layout>
	<div class="row">
		<div class="col">Name</div>
		<div class="col-7">${c.table.id}</div>
	<div class="w-100"></div>
		<div class="col">Label</div>
		<div class="col-7">${c.table.label}</div>
	<div class="w-100"></div>
		<div class="col">Tags</div>
		<div class="col-7">[ ${c.table.tags?join(", ")} ]</div>
	<div class="w-100"></div>
		<div class="col">Blocks</div>
		<div class="col-7">${c.numberOfBlocks}</div>
	<div class="w-100"></div>
		<div class="col">Entries</div>
		<div class="col-7">${c.numberOfEntries}</div>
	<div class="w-100"></div>
		<div class="col">Columns</div>
		<div class="col-7">
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
		</div>
	</div>
</@layout.layout>