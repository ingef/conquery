<#import "templates/template.html.ftl" as layout>
<#import "templates/permissionTable.html.ftl" as permissionTable>
<#import "templates/permissionCreator.html.ftl" as permissionCreator>

<@layout.layout>
	<div class="row">
		<div class="col">
		<h2>Role: ${c.owner.label}</h2>
		<h3>Id: ${c.owner.id}</h3>

		<ul class="nav nav-tabs" id="myTab" role="tablist">
			<li class="nav-item">
				<a class="nav-link active" id="ownedPermissions-tab" data-toggle="tab" href="#ownedPermissions" role="tab" aria-controls="ownedPermissions" aria-selected="true">Owned Permissions</a>
			</li>
			<li class="nav-item">
				<a class="nav-link" id="createPermission-tab" data-toggle="tab" href="#createPermission" role="tab" aria-controls="createPermission" aria-selected="false">Create Permission</a>
			</li>
			<li class="nav-item">
				<a class="nav-link" id="users-tab" data-toggle="tab" href="#users" role="tab" aria-controls="users" aria-selected="false">Users</a>
			</li>
		</ul>
		<div class="tab-content" id="myTabContent">
			<div class="tab-pane fade show active" id="ownedPermissions" role="tabpanel" aria-labelledby="ownedPermissions-tab">
				<@permissionTable.permissionTable ownerId=c.owner.getId() permissions=c.permissions />
			</div>
			<div class="tab-pane fade" id="createPermission" role="tabpanel" aria-labelledby="createPermission-tab">
			<@permissionCreator.permissionCreator ownerId=c.owner.getId() permissionTemplateMap=c.permissionTemplateMap />
			</div>
			<div class="tab-pane fade" id="users" role="tabpanel" aria-labelledby="users-tab">
				<ul>
				<#list c.users as user>
					<li>
						<a href="/admin/users/${user.id}">${user.label}</a>
					</li>
				</#list>
				</ul>
			</div>
		</div>
	</div>
</@layout.layout>