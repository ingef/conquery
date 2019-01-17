<#import "templates/template.html.ftl" as layout>
<#import "templates/permissionTable.html.ftl" as permissionTable>
<@layout.layout>
	<div class="row">
		<div class="col">
		<h3>Dataset Permissions</h3>
		<@permissionTable.permissionTable permissions=c.datasetPermissions />
		<h3>Query Permissions</h3>
		<@permissionTable.permissionTable permissions=c.queryPermissions />
		<h3>Other Permissions</h3>
		<@permissionTable.permissionTable permissions=c.otherPermissions />
		</div>
	</div>
	<div class="row">
		<h3>Users</h3>
		<div class="col">
			<ul>
			<#list c.users as user>
				<li>
					<a href="/admin/user/${user.id}">${user.label}</a> 
					<a href="" onclick="event.preventDefault(); fetch('./user/${user.id}', {method: 'delete'}).then(function(){location.reload();});"><i class="fas fa-trash-alt text-danger"></i></a>
				</li>
			</#list>
			</ul>
		</div>
	</div>
</@layout.layout>