<#import "templates/template.html.ftl" as layout>
<#import "templates/permissionTable.html.ftl" as permissionTable>

<@layout.layout>
	<div class="row">
		<div class="col">
		<h2>${c.self.label}</h2>
		<h3>Dataset Permissions</h3>
		<@permissionTable.permissionTable ownerId=c.self.getId() permissions=c.datasetPermissions/>
		
	<h3>Create Permission</h3>
	<#assign TARGETS = "targets_">
	<#assign ABILITIES = "abilities_">
    <table class="table table-striped">
        <thead>
            <tr>
            <th scope="col">Permission Type</th>
            <th scope="col">Target</th>
            <th scope="col">Abilities</th>
            <th></th>
            </tr>
        </thead>
        <tbody>
		<#list c.permissionTemplateMap?keys as key>
            <tr>
				<td>${key}</td>
				<td>
					<#if c.permissionTemplateMap[key].right?size != 0>
						<select id="${TARGETS}${key}" class="form-control">
							<#list c.permissionTemplateMap[key].right as target>
									<option>${target}</option>
							</#list>
						</select>
					</#if>
				</td>
				<td>
				<div class="form-group col" id="${ABILITIES}${key}">
					<#list c.permissionTemplateMap[key].left as ability>
						<div class="form-check">
							<input class="form-check-input" type="checkbox" value="${ability}" id="ability_${key}_${ability}">
							<label class="form-check-label" for="ability_${key}_${ability}">
								${ability}
							</label>
						</div>
					</#list>
				</div>
				</td>
				<td><a href="#" onclick="submitPermission('${key}')"> <i class="fas fa-share"></i> </a> </td>
            </tr>
		</#list>
		</tbody>
	</table>
	<script type="application/javascript">
	function submitPermission(permissionType) {
		event.preventDefault();

		let permission = {}
		permission.type = permissionType;

		let targetSelector = document.getElementById("${TARGETS}"+permissionType);
		let target = null
		if (targetSelector != null) {
			let option = targetSelector.options[targetSelector.selectedIndex]
			if(option.length > 0 && option == null) {
				alert("No target for permission specified");
				return;
			}
			target = option.value;
			permission.instanceId = target;
		}

		let abilitySelector = document.getElementById("${ABILITIES}"+permissionType);
		let abilities = [];
		if (abilitySelector != null) {
			let abilityCheckboxes =abilitySelector.getElementsByTagName('input');
			for(let cb of abilityCheckboxes ) {
				if(cb.checked){
					abilities.push(cb.value)
				}
			}
			if(abilityCheckboxes.length > 0 && abilities.length == 0 ) {
				alert("No abilities for permission specified");
				return;
			}
			permission.abilities = abilities;
		}

		
		
		console.log("Sending Permission: Type: " + permissionType + "\tTarget: " + target+ "\tAbilities: " + abilities);
		fetch('/admin/permissions/${c.self.getId()}',
			{
				method: 'post',
				headers: {'Content-Type': 'application/json'},
				body: JSON.stringify(permission)}).then(function(){location.reload()})
	}
	</script>


		<h3>Query Permissions</h3>
		<@permissionTable.permissionTable ownerId=c.self.getId() permissions=c.queryPermissions/>
		<h3>Other Permissions</h3>
		<@permissionTable.permissionTable ownerId=c.self.getId() permissions=c.otherPermissions/>
		</div>
	</div>
	<h3>Users</h3>
	<div class="row">
		<div class="col">
			<ul>
			<#list c.users as user>
				<li>
					${user.label}
				</li>
			</#list>
			</ul>
		</div>
	</div>
</@layout.layout>