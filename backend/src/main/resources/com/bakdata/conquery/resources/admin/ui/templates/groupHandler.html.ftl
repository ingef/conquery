<#macro groupHandler c adminPathBase>

<div class="col bg-light pt-3 pb-3 rounded">
	<form>
	    <h4>Add Group</h4>
		<div class="form-group">
			<label for="group_id">Group:</label>
			<select class="form-control" id="group_id" name="group_id">
				<#list c.availableGroups as group>
					<option value="${group.id}">${group.label} - ${group.id}</option>
				</#list>
			</select>
		</div>
		<input class="btn btn-primary" type="submit" onclick="addGroup()" />
	</form>
</div>

<div class="col pt-3 pb-3 table-responsive">
	<table class="table table-sm table-striped">
		<thead>
			<tr>
				<th scope="col">Group</th>
                <th scope="col">Id</th>
				<th scope="col"></th>
			</tr>
		</thead>
		<tbody>
			<#list c.groups as group>
				<tr>
					<td><a href="/admin-ui/groups/${group.id}">${group.label}</a></td>
					<td><a href="/admin-ui/groups/${group.id}">${group.id}</a></td>
					<td><a href="#"
							onclick="removeGroup('${adminPathBase}/${group.id}/${ctx.staticUriElem.USERS_PATH_ELEMENT}/${c.owner.id}')">Remove
							from Entity <i class="fas fa-trash-alt text-danger"></i></a></td>
				</tr>
			</#list>
		</tbody>
	</table>
</div>
	<script type="application/javascript">
		function addGroup() {
			event.preventDefault();
			fetch(
				'${adminPathBase}/' + document.getElementById('group_id').value + '/${ctx.staticUriElem.USERS_PATH_ELEMENT}/${c.owner.id}',
				{
					method: 'post',
					credentials: 'same-origin',
					headers: { 'Content-Type': 'application/json' }
				}).then(function () { location.reload() });
		}

		function removeGroup(path) {
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