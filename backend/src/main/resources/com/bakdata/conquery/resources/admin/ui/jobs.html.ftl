<#import "templates/template.html.ftl" as layout>
<@layout.layout>
	<#list c as node, status>
	<div class="row">
		<div class="col">
			<div class="card">
				<div class="card-body">
					<h5 class="card-title">
						${node}
						<span class="float-right">
							<small>updated ${status.ageString} ago</small> 
							<span class="badge badge-secondary">${status.jobs?size}</span>
						</span>
					</h5>
                    <div class="card-text" style="max-height:50vh; overflow: auto">
                        <table class="table">
                            <#list status.jobs as job>
                            <tr class="${job.cancelled?then('active','')}">
                                <td>
                                    ${job.label}
                                </td>
                                <td class="w-100">
                                    <div class="progress position-relative">
                                        <div class="progress-bar" role="progressbar" style="width: ${job.progressReporter.progress?string.percent}" aria-valuenow="${job.progressReporter.progress?c}" aria-valuemin="0" aria-valuemax="1"></div>
                                        <small class="justify-content-center d-flex position-absolute w-100"><pre>${job.progressReporter.estimate}</pre></small>
                                    </div>
                                </td>
                                <td>
                                    <#if !job.cancelled>
                                        <form action="/admin/jobs/${job.jobId}/cancel" method="post" enctype="multipart/form-data">
                                            <input class="btn btn-warning btn-sm" type="submit" value="Cancel"/>
                                        </form>
                                    <#else>
                                        <div>Cancelled</div>
                                    </#if>
                                </td>
                            </tr>
                            </#list>
                        </table>
                    </div>
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