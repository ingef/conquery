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
                                        <div class="progress-bar" role="progressbar" style="width: ${job.progress?string.percent}" aria-valuenow="${job.progressReporter.progress?c}" aria-valuemin="0" aria-valuemax="1"></div>
                                    </div>
                                </td>
                                <td>
                                    <#if !job.cancelled>
                                        <a href="" onclick="cancelJob('${job.jobId}')" class="btn btn-warning btn-sm"/>
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
            <input type="checkbox" id="update" name="update" checked>
            <label for="update">Reload automatically.</label><br>
			<script type="text/javascript">
				setTimeout(function () {
                    if(!document.getElementById("update").checked){
                        return
                    }
					
                    location.reload(false);
				}, 5000);

                
                function cancelJob(jobId) {
		            event.preventDefault(); 
                    fetch(
                        ${r"`/admin/jobs/${jobId}/cancel`"},
                        {
                            method: "post",
                            credentials: "same-origin"
                        }
                    )
                }
			</script>
		</div>
	</div>
</@layout.layout>