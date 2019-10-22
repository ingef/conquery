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
					<td><a href="" onclick="event.preventDefault(); fetch('./${c.self.id}/${role.id}', {method: 'delete'}).then(function(){location.reload();});">Remove from user<i class="fas fa-trash-alt text-danger"></i></a></td>
					</li>
				</tr>
			</#list>
		</table>

		<h3>Add Role</h3>
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

		<h3>Permissions</h3>

		<@permissionTable.permissionTable ownerId=c.self.getId() permissions=c.permissions/>
	</div>
	<script type="application/javascript">
	function addRole() {
		event.preventDefault(); 
		fetch('./${c.self.id}/'+document.getElementById('role_id').value,
		{
			method: 'post',
			headers: {'Content-Type': 'application/json'}
		}).then(function(){location.reload()});
	}
	</script>
</@layout.layout>