<#macro roleHandler c adminPathBase>

<div class="col bg-light pt-3 pb-3 rounded">
	<form>
	    <h4>Add Role</h4>
		<div class="form-group">
			<label for="role_id">Role:</label>
			<select class="form-control" id="role_id" name="role_id">
				<#list c.availableRoles as role>
					<option value="${role.id}">${role.label} - ${role.id}</option>
				</#list>
			</select>
		</div>
		<input class="btn btn-primary" type="submit" onclick="addRole()" />
	</form>
</div>

<div class="col pt-3 pb-3 table-responsive">
	<table class="table table-sm table-striped">
		<thead>
			<tr>
				<th scope="col">Role</th>
                <th scope="col">Id</th>
				<th scope="col">Target</th>
			</tr>
		</thead>
		<tbody>
			<#list c.roles as role>
				<tr>
					<td><a href="/admin-ui/roles/${role.id}">${role.label}</a></td>
					<td><a href="/admin-ui/roles/${role.id}">${role.id}</a></td>
					<td><a href="#"
							onclick="removeRole('${adminPathBase}/${c.owner.id}/${ctx.staticUriElem.ROLES_PATH_ELEMENT}/${role.id}')">Remove
							from Entity<i class="fas fa-trash-alt text-danger"></i></a></td>
				</tr>
			</#list>
		</tbody>
	</table>
</div>
	<script type="application/javascript">
		function addRole() {
			event.preventDefault();
			fetch(
				'${adminPathBase}/${c.owner.id}/${ctx.staticUriElem.ROLES_PATH_ELEMENT}/' + document.getElementById('role_id').value,
				{
					method: 'post',
					credentials: 'same-origin',
					headers: { 'Content-Type': 'application/json' }
				}).then(function () { location.reload() });
		}

		function removeRole(path) {
			event.preventDefault();
			fetch(
				path,
				{
					method: 'delete',
					credentials: 'same-origin'
				})
				.then(function () { location.reload(); });
		}
	</script>
</#macro>