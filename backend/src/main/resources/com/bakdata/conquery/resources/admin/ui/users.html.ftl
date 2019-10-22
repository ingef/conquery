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
		</div>
	</div>
</@layout.layout>