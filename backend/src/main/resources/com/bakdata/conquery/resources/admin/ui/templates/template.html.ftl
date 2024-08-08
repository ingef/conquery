<#import "base.html.ftl" as base>
<#macro layout>
	<@base.html "Conquery Admin UI" >
	  <body>
		  <nav class="navbar navbar-expand-lg navbar-light bg-light" style="margin-bottom:30px">
			  <a class="navbar-brand" href="${ctxPath}/admin-ui">Conquery Admin</a>
			  <button class="navbar-toggler" type="button" data-toggle="collapse" data-target="#navbarSupportedContent"
				  aria-controls="navbarSupportedContent" aria-expanded="false" aria-label="Toggle navigation">
				  <span class="navbar-toggler-icon"></span>
			  </button>

			  <div class="collapse navbar-collapse" id="navbarSupportedContent">
				  <ul class="navbar-nav mr-auto">
					  <li class="nav-item">
						  <a class="nav-link" href="${ctxPath}/admin-ui/datasets">Datasets</a>
					  </li>
					  <li class="nav-item">
						  <a class="nav-link" href="${ctxPath}/${ctx.staticUriElem.ADMIN_UI_SERVLET_PATH}/${ctx.staticUriElem.INDEX_SERVICE_PATH_ELEMENT}">Index Service</a>
					  </li>
					  <li class="nav-item">
						  <a class="nav-link" href="${ctxPath}/admin-ui/jobs">Jobs</a>
					  </li>
					  <li class="nav-item">
						  <a class="nav-link" href="${ctxPath}/admin-ui/queries">Queries</a>
					  </li>
					  <li class="nav-item">
						  <a class="nav-link" href="${ctxPath}/admin-ui/script">Script</a>
					  <li class="nav-item dropdown">
						  <a class="nav-link dropdown-toggle" href="#" id="navbarDropdown" role="button"
							  data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
							  Auth
						  </a>
						  <div class="dropdown-menu" aria-labelledby="navbarDropdown">
							  <a class="dropdown-item" href="${ctxPath}/admin-ui/auth-overview">Overview</a>
							  <a class="dropdown-item" href="${ctxPath}/admin-ui/groups">Groups</a>
							  <a class="dropdown-item" href="${ctxPath}/admin-ui/users">Users</a>
							  <a class="dropdown-item" href="${ctxPath}/admin-ui/roles">Roles</a>
						  </div>
					  </li>
					  <li class="nav-item dropdown">
						  <a class="nav-link dropdown-toggle" href="#" id="navbarDropdown" role="button"
							  data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
							  Dropwizard
						  </a>
						  <div class="dropdown-menu" aria-labelledby="navbarDropdown">
							  <a class="dropdown-item" href="${ctxPath}/metrics?pretty=true">Metrics JSON</a>
							  <a class="dropdown-item" href="${ctxPath}/threads">Threads</a>
							  <a class="dropdown-item" href="${ctxPath}/healthcheck?pretty=true">Health</a>
							  <a class="dropdown-item" href=""
								  onclick="shutdown(event)"><i
									  class="fas fa-power-off text-danger"></i> Shutdown</a>
						  </div>
					  </li>
				  </ul>
				  <!-- Status of the shardNodes -->
				  <div>
					  <#list ctx.shardNodes as key,shardNode>
						  <i class="fas fa-circle <#if shardNode.connected>text-success<#else>text-danger</#if>"></i>
					  </#list>
				  </div>
			  </div>

			  <div class="pl-2">
				<a href="${ctxPath}/admin-ui/logout" class="btn btn-secondary">Logout</a>
			  </div>
		  </nav>

		  <div class="container">
			  <#nested />
		  </div>

		  <div aria-live="polite" aria-atomic="true">
			  <div id="toast-container" style="position: fixed; bottom: 10px; right: 10px; z-index: 9999; float: right" />
		  </div>

	  </body>
	</@base.html>
</#macro>

<#-- General key-value macro -->
<#macro kv k="" v="">
	<#if v?has_content>
		<@kc k=k>${v}</@kc>
	</#if>
</#macro>

<#-- Key-value macro for ids -->
<#macro kid k="" v="">
	<#if v?has_content>
		<@kc k=k><code>${v}</code></@kc>
	</#if>
</#macro>

<#-- Key-value macro for container -->
<#macro kc k="">
	<div class="row" style="padding-top:5px">
		<div class="col">${k}</div>
		<div class="col-10">
			<#nested />
		</div>
	</div>
</#macro>

<#-- Macro to display SI unit -->
<#function si num>
	<#assign order=num?round?length />
	<#assign thousands=((order - 1) / 3)?floor />
	<#if (thousands < 0)>
		<#assign thousands=0 />
	</#if>
	<#assign siMap=[ {"factor": 1, "unit" : "" }, {"factor": 1000, "unit" : "K" }, {"factor": 1000000, "unit" : "M" },
		{"factor": 1000000000, "unit" :"G"}, {"factor": 1000000000000, "unit" : "T" } ] />
	<#assign siStr=(num / (siMap[thousands].factor))?string("0.# ") + siMap[thousands].unit />
  <#return siStr />
</#function>
