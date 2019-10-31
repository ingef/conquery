<#import "templates/template.html.ftl" as layout>
<@layout.layout>
	<div class="row">
		<div class="col">
			<ul>
			<#list c as user>
				<li>
					<a href="/admin/users/${user.id}">${user.label}</a> 
					<a href="" onclick="event.preventDefault(); fetch('./users/${user.id}', {method: 'delete'}).then(function(){location.reload();});"><i class="fas fa-trash-alt text-danger"></i></a>
				</li>
			</#list>
			</ul>
			
			<button class="btn btn-primary" onclick="downloadUsers()"> Download Users </button>
		</div>
	</div>
	
	<script type="application/javascript">
	function downloadUsers() {
		event.preventDefault(); 
		fetch('./users/',
		{
			method: 'get',
			headers: {'Accept': 'application/json'}
		})
		.then(response => {return response.json()})
		.then(json => {
			console.log(json);
			uriContent = "data:application/octet-stream," + encodeURIComponent(JSON.stringify(json));
			newWindow = window.open(uriContent, 'Users');
			});
	}
	
	</script>
</@layout.layout>