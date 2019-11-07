<#macro permissionCreator ownerId permissionTemplateMap>
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
		<#list permissionTemplateMap?keys as key>
            <tr>
				<td>${key}</td>
				<td>
					<#if permissionTemplateMap[key].right?size != 0>
						<select id="${TARGETS}${key}" class="form-control">
							<#list permissionTemplateMap[key].right as target>
									<option>${target}</option>
							</#list>
						</select>
					</#if>
				</td>
				<td>
					<#if permissionTemplateMap[key].left?has_content>
						<div class="form-group col" id="${ABILITIES}${key}">
							<#list permissionTemplateMap[key].left as ability>
								<div class="form-check">
									<input class="form-check-input" type="checkbox" value="${ability}" id="ability_${key}_${ability}">
									<label class="form-check-label" for="ability_${key}_${ability}">
										${ability}
									</label>
								</div>
							</#list>
						</div>
					</#if>
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

		
		
		console.log("Sending Permission: " + permission);
		fetch('/admin/permissions/${ownerId}',
			{
				method: 'post',
				headers: {'Content-Type': 'application/json'},
				body: JSON.stringify(permission)}).then(function(){location.reload()})
	}
	</script>
</#macro>