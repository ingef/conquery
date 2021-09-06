<#import "templates/template.html.ftl" as layout>
<#import "templates/permissionTable.html.ftl" as permissionTable>
<#import "templates/permissionCreator.html.ftl" as permissionCreator>
<#import "templates/roleHandler.html.ftl" as roleHandler>
<@layout.layout>
	<div class="row">
		<div class="col">
		<h2>Group: ${c.owner.label}</h2>
		<h3>Id: ${c.owner.id}</h3>

		<ul class="nav nav-tabs" id="myTab" role="tablist">
			<li class="nav-item">
				<a class="nav-link active" id="ownedPermissions-tab" data-toggle="tab" href="#ownedPermissions" role="tab" aria-controls="ownedPermissions" aria-selected="true">Owned Permissions</a>
			</li>
			<li class="nav-item">
				<a class="nav-link" id="createPermission-tab" data-toggle="tab" href="#createPermission" role="tab" aria-controls="createPermission" aria-selected="false">Create Permission</a>
			</li>
			<li class="nav-item">
				<a class="nav-link" id="members-tab" data-toggle="tab" href="#member" role="tab" aria-controls="member" aria-selected="false">Members</a>
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
			<div class="tab-pane fade" id="member" role="tabpanel" aria-labelledby="member-tab">
				<table class="table table-striped">
					<thead>
						<tr>
						<th scope="col">Member</th>
						<th scope="col"></th>
						</tr>
					</thead>
					<tbody>
					<#list c.members as member>
						<tr>
							<td><a href="/admin-ui/${ctx.staticUriElem.USERS_PATH_ELEMENT}/${member.id}">${member.label}</a></td>
							<td><a href="#" onclick="removeMember('/admin/${ctx.staticUriElem.GROUPS_PATH_ELEMENT}/${c.owner.id}/${ctx.staticUriElem.USERS_PATH_ELEMENT}/${member.id}')">Remove from ${c.owner.label}<i class="fas fa-trash-alt text-danger"></i></a></td>
						</tr>
					</#list>
					</tbody>
				</table>
				<h4>Add Member</h4>
				<form>
					<div class="form-group col">
						<label for="member_id">Member:</label>
						<select class="form-control" id="member_id" name="member_id">
							<#list c.availableMembers as member>
								<option value="${member.id}">${member.label}</option>
							</#list>
						</select>
						<input class="btn btn-primary" type="submit" onclick="addMember()"/>
					</div>
				</form>
			</div>
			<div class="tab-pane fade" id="roles" role="tabpanel" aria-labelledby="roles-tab">
				<@roleHandler.roleHandler c=c adminPathBase="/admin/${ctx.staticUriElem.GROUPS_PATH_ELEMENT}" />
			</div>
		</div>

	</div>
	<script type="application/javascript">
	function addMember() {
		event.preventDefault(); 
		fetch(
			'/admin/${ctx.staticUriElem.GROUPS_PATH_ELEMENT}/${c.owner.id}/${ctx.staticUriElem.USERS_PATH_ELEMENT}/'+document.getElementById('member_id').value,
			{
				method: 'post',
				credentials: 'same-origin',
				headers: {'Content-Type': 'application/json'}
			})
			.then(function(){location.reload()});
	}

	function removeMember(path){
		event.preventDefault();
		fetch(
			path,
			{
				method: 'delete',
				credentials: 'same-origin',
			})
			.then(function(){location.reload();});
	}
	</script>
</@layout.layout>