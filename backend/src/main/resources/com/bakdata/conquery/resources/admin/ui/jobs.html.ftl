<#import "templates/template.html.ftl" as layout>
<@layout.layout>
	<#list c as node, jobs>
	<div class="row">
		<div class="col">
			<div class="card">
				<div class="card-body">
					<h5 class="card-title">
						${node}
						<span class="badge badge-secondary">${jobs?size}</span>
					</h5>
					<hr/>
				</div>
				<div class="card-body" style="max-height:50vh; overflow: auto">
					<table class="card-text">
						<#list jobs as job>
						<tr>
							<td>
								${job.label}
							</td>
							<td>
								<div class="progress position-relative" style="width:400px">
			  						<div class="progress-bar" role="progressbar" style="width: ${job.progressReporter.progress}" aria-valuenow="${job.progressReporter.progress?c}" aria-valuemin="0" aria-valuemax="1"></div>
			  						<small class="justify-content-center d-flex position-absolute w-100"><pre>${job.progressReporter.estimate}</pre></small>
			  					</div>
							</td>
						</tr>
						</#list>
					</table>
				</div>
			</div>
		</div>
	</div>
	</#list>
	
	<div class="row">
		<div class="col">
			<br/><br/>
			<form action="/admin/jobs" method="post" enctype="multipart/form-data">
				<h3>Create Demo Job</h3>
				<input class="btn btn-primary" type="submit"/>
			</form>
			<script type="text/javascript">
				setTimeout(function () { 
					location.reload(false);
				}, 2000);
			</script>
		</div>
	</div>
</@layout.layout>