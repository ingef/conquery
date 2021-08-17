<#macro permissionCreator ownerId permissionTemplateMap>
    <#local INSTANCES = "instances_">
	<#local ABILITIES = "abilities_">
    <table class="table table-striped">
        <thead>
            <tr>
            <th scope="col">Domain</th>
            <th scope="col">Abilities</th>
            <th scope="col">Instance</th>
            <th></th>
            </tr>
        </thead>
        <tbody>
		<#list permissionTemplateMap?keys as key>
            <tr>
				<td>${key}</td>
				<td>
					<#assign abilities=permissionTemplateMap[key].getLeft()>
					<#if abilities?has_content>
						<div class="form-group col" id="${ABILITIES}${key}">
							<#list abilities as ability>
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
				<td>
					<input id="${INSTANCES}${key}" type="text" name="fname">
				</td>
				<td><a href="#" onclick="submitPermission('${key}')"> <i class="fas fa-share"></i> </a> </td>
            </tr>
		</#list>
		</tbody>
	</table>
	<script type="application/javascript">
	function submitPermission(domain) {
		event.preventDefault();


		let abilitySelector = document.getElementById("${ABILITIES}"+domain);
		abilityJoin = null;
		if (abilitySelector != null) {
			let abilities = [];
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
			abilityJoin = abilities.join(",");
		}

		let instanceInput = document.getElementById("${INSTANCES}"+domain).value;
		
		permission = [domain, abilityJoin, instanceInput].join(":")
		console.log("Sending Permission: " + permission);
		fetch('/admin/permissions/${ownerId}',
			{
				method: 'post',
				credentials: 'same-origin',
				headers: {'Content-Type': 'text/plain'},
				body: permission}).then(function(){location.reload()})
	}
	</script>
</#macro>