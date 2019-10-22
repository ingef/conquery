<#import "templates/template.html.ftl" as layout>
<#import "templates/permissionTable.html.ftl" as permissionTable>
<@layout.layout>
	<div class="row">
		<div class="col">
		<h2>${c.self.label}</h2>
		<h3>Roles</h3>
    <table class="table table-striped">
		<#list c.roles as role>
			<tr>
				<td><a href="/admin/roles/${role.id}">${role.label}</a> </td>
				<td><a href="" onclick="event.preventDefault(); fetch('./roles/${role.id}', {method: 'delete'}).then(function(){location.reload();});">Remove from user<i class="fas fa-trash-alt text-danger"></i></a></td>
				</li>
			</tr>
		</#list>
    </table>

		<h3>Permissions</h3>

		<@permissionTable.permissionTable ownerId=c.self.getId() permissions=c.permissions/>
	</div>
</@layout.layout>