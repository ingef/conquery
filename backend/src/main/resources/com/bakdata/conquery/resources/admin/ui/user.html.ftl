<#import "templates/template.html.ftl" as layout>
<#import "templates/permissionTable.html.ftl" as permissionTable>
<#import "templates/permissionCreator.html.ftl" as permissionCreator>
<@layout.layout>
	<div class="row">
		<div class="col">
		<h2>User: ${c.owner.label}</h2>
		<h3>Id: ${c.owner.id}</h3>

		<ul class="nav nav-tabs" id="myTab" role="tablist">
			<li class="nav-item">
				<a class="nav-link active" id="ownedPermissions-tab" data-toggle="tab" href="#ownedPermissions" role="tab" aria-controls="ownedPermissions" aria-selected="true">Owned Permissions</a>
			</li>
			<li class="nav-item">
				<a class="nav-link" id="createPermission-tab" data-toggle="tab" href="#createPermission" role="tab" aria-controls="createPermission" aria-selected="false">Create Permission</a>
			</li>
			<li class="nav-item">
				<a class="nav-link" id="roles-tab" data-toggle="tab" href="#roles" role="tab" aria-controls="roles" aria-selected="false">Roles</a>
			</li>
		</ul>
		<div class="tab-content" id="myTabContent">
			<div class="tab-pane fade show active" id="ownedPermissions" role="tabpanel" aria-labelledby="ownedPermissions-tab">
				<@permissionTable.permissionTable ownerId=c.owner.getId() permissions=c.permissions />
			</div>
			<div class="tab-pane fade" id="createPermission" role="tabpanel" aria-labelledby="createPermission-tab">
			<@permissionCreator.permissionCreator ownerId=c.owner.getId() permissionTemplateMap=c.permissionTemplateMap />
			</div>
			<div class="tab-pane fade" id="roles" role="tabpanel" aria-labelledby="roles-tab">
				<table class="table table-striped">
					<thead>
						<tr>
						<th scope="col">Role</th>
						<th scope="col">Target</th>
						</tr>
					</thead>
					<tbody>
					<#list c.roles as role>
						<tr>
							<td><a href="/admin/roles/${role.id}">${role.label}</a></td>
							<td><a href="#" onclick="removeRole('./${c.owner.id}/${role.id}')">Remove from user<i class="fas fa-trash-alt text-danger"></i></a></td>
						</tr>
					</#list>
					</tbody>
				</table>
				<h4>Add Role</h4>
				<form>
					<div class="form-group col">
						<label for="role_id">Role:</label>
						<select class="form-control" id="role_id" name="role_id">
							<#list c.availableRoles as role>
								<option value="${role.id}">${role.label}</option>
							</#list>
						</select>
						<input class="btn btn-primary" type="submit" onclick="addRole()"/>
					</div>
				</form>
			</div>
		</div>

	</div>
	<script type="application/javascript">
	function addRole() {
		event.preventDefault(); 
		fetch(
			'./${c.owner.id}/'+document.getElementById('role_id').value,
			{
				method: 'post',
				headers: {'Content-Type': 'application/json'}
			}).then(function(){location.reload()});
	}

	function removeRole(path){
		event.preventDefault();
		fetch(
			path,
			{method: 'delete'})
			.then(function(){location.reload();});
	}
	</script>
</@layout.layout>