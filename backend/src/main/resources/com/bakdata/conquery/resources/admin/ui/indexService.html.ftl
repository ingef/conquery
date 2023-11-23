<#import "templates/template.html.ftl" as layout>
<#import "templates/table.html.ftl" as table>
<@layout.layout>
	<div class="row">
		<div class="col">
			<h1>Index Service</h1>
			<a data-test-id="delete-btn-index" href="" onclick="resetIndexService()" class="btn btn-danger">Reset Index <i class="fas fa-trash-alt" ></i></a>
			<div id="index-service-stats" class="mt-1">
				<h3>Statistics</h3>
				<#assign columns=["name", "value" ]>
					<#assign items=[
						{"name":"Hit count", "value":c.getStats().hitCount()}
						{"name":"Miss count", "value":c.getStats().missCount()},
						{"name":"Eviction count", "value":c.getStats().evictionCount()},
						{"name":"Load success count", "value":c.getStats().loadSuccessCount()},
						{"name":"Load exception count", "value":c.getStats().loadExceptionCount()},
						{"name":"Total load time", "value":c.getStats().totalLoadTime()}
						]>
						<@table.table columns=columns items=items cypressId="statistics"/>
			</div>

			<div id="index-service-indexes" class="mt-1">
				<h3>Indexes</h3>
				<#assign columns=["csv", "internalColumn", "externalTemplates" ]>
					<#assign items=c.getIndexes()?sort_by('csv') >
						<@table.table columns=columns items=items cypressId="indexes" />
			</div>
		</div>
	</div>
	<script type="application/javascript">
		function resetIndexService() {
			event.preventDefault();
			fetch('/${ctx.staticUriElem.ADMIN_SERVLET_PATH}/${ctx.staticUriElem.INDEX_SERVICE_PATH_ELEMENT}/reset', {
				method: 'post',
				credentials: 'same-origin',
				headers: {'Content-Type': 'application/json'},
			
		}).then(function() {location.reload()});
		}
	</script>
</@layout.layout>