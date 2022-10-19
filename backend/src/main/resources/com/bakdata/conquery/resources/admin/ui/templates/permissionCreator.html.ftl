<#macro permissionCreator ownerId permissionTemplateMap>
    <#local INSTANCES = "instances_">
	<#local ABILITIES = "abilities_">
	<div class"table-responsive">
    <table class="table table-sm table-striped">
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
			<#assign abilities=permissionTemplateMap[key].getLeft()>
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
				<td id="${INSTANCES}${key}">
					<#if abilities?has_content>
                        <input type="text" class="form-control" name="fname" oninput="renderInstanceList(this)" placeholder="Instance1, Instance2, ...">
                        <ul class="list-group">
                            <!-- list for rendered instances -->
                        </ul>
					</#if>
				</td>
				<td><a href="#" onclick="submitPermission('${key}')"> <i class="fas fa-share"></i> </a> </td>
            </tr>
		</#list>
		</tbody>
	</table>
	</div>
	<script type="application/javascript">

	function renderInstanceList(instanceListString) {
	    // from the target input field render a list with the target instances cleaned from white spaces
	    const instanceList = instanceListString.parentNode.getElementsByTagName("ul")[0];
	    // reset list content
	    instanceList.innerHTML = '';
	    instanceListString.value.split(',')
	        .map(i => i.trim())
	        .filter(i => !!i)
	        .forEach(i => {
                var li = document.createElement("li");
                li.setAttribute("class", "list-group-item p-1");
                li.innerHTML = i
	            instanceList.appendChild(li)
	        });
	}

	async function submitPermission(domain) {
		event.preventDefault();


		let abilitySelector = document.getElementById("${ABILITIES}"+domain);
		abilityJoin = null;
		if (abilitySelector != null) {
			let abilities = [];
			let abilityCheckboxes =abilitySelector.getElementsByTagName('input');
			for(let cb of abilityCheckboxes ) {
				if(cb.checked){
					abilities.push(cb.value.trim())
				}
			}
			if(abilityCheckboxes.length > 0 && abilities.length == 0 ) {
				alert("No abilities for permission specified");
				return;
			}
			abilityJoin = abilities.join(",");
		}

        // Collection only only as a member if a permission supports abilities and thus targets
		let instanceInput = document.getElementById("${INSTANCES}"+domain).getElementsByTagName("ul");


		if(instanceInput.length != 0) {
		    // Permission can have targets
		    instances = Array.from(instanceInput[0].children,c => c.innerHTML).join(",");
		}
		else {
		    // Permission does not support abilities and targets (e.g. super permission)
		    instances = null;
		}
		
		permission = [domain, abilityJoin, instances].join(":")
		console.log("Sending Permission: " + permission);
		await fetch('/admin/permissions/${ownerId}',
			{
				method: 'post',
				credentials: 'same-origin',
				headers: {'Content-Type': 'application/json'},
				body: permission
			});
		location.reload();
	}
	</script>
</#macro>