<#macro layout>
<!doctype html>
<html lang="en">
  <head>
	<!-- Required meta tags -->
	<meta charset="utf-8">
	<meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">

	<!-- Bootstrap CSS -->
	<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/twitter-bootstrap/4.0.0/css/bootstrap.min.css" crossorigin="anonymous">
	<link rel="stylesheet" href="https://use.fontawesome.com/releases/v5.0.8/css/solid.css" integrity="sha384-v2Tw72dyUXeU3y4aM2Y0tBJQkGfplr39mxZqlTBDUZAb9BGoC40+rdFCG0m10lXk" crossorigin="anonymous">
	<link rel="stylesheet" href="https://use.fontawesome.com/releases/v5.0.8/css/fontawesome.css" integrity="sha384-q3jl8XQu1OpdLgGFvNRnPdj5VIlCvgsDQTQB6owSOHWlAurxul7f+JpUOVdAiJ5P" crossorigin="anonymous">
	<style>
		.headed-table tr th,
		.headed-table tr td {
			padding-right:15px;
			vertical-align:top;
		}
		
		ul {
			padding-left:0;
			list-style:"\2023" inside;
		}
		
		h3 {
			padding-bottom:10px;
			padding-top:20px;
		}
	</style>

	<title>Conquery Admin UI</title>
  </head>
  <body>
  	<script src="https://ajax.googleapis.com/ajax/libs/jquery/3.3.1/jquery.min.js"></script>
	<script src="https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.12.9/umd/popper.min.js" integrity="sha384-ApNbgh9B+Y1QKtv3Rn7W3mgPxhU9K/ScQsAP7hUibX39j7fakFPskvXusvfa0b4Q" crossorigin="anonymous"></script>
	<script src="https://cdnjs.cloudflare.com/ajax/libs/twitter-bootstrap/4.0.0/js/bootstrap.min.js" crossorigin="anonymous"></script>
	
	<nav class="navbar navbar-expand-lg navbar-light bg-light" style="margin-bottom:30px">
	  <a class="navbar-brand" href="/admin-ui">Conquery Admin</a>
	  <button class="navbar-toggler" type="button" data-toggle="collapse" data-target="#navbarSupportedContent" aria-controls="navbarSupportedContent" aria-expanded="false" aria-label="Toggle navigation">
		<span class="navbar-toggler-icon"></span>
	  </button>
	
	  <div class="collapse navbar-collapse" id="navbarSupportedContent">
		<ul class="navbar-nav mr-auto">
		  <li class="nav-item">
			<a class="nav-link" href="/admin-ui/datasets">Datasets</a>
		  </li>
		  <li class="nav-item">
			<a class="nav-link" href="/admin-ui/jobs">Jobs</a>
		  </li>
		  <li class="nav-item">
          	<a class="nav-link" href="/admin-ui/queries">Queries</a>
          </li>
		  <li class="nav-item">
			<a class="nav-link" href="/admin-ui/script">Script</a>
		  <li class="nav-item dropdown">
			<a class="nav-link dropdown-toggle" href="#" id="navbarDropdown" role="button" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
			  Auth
			</a>
			<div class="dropdown-menu" aria-labelledby="navbarDropdown">
			  <a class="dropdown-item" href="/admin-ui/auth-overview">Overview</a>
			  <a class="dropdown-item" href="/admin-ui/groups">Groups</a>
			  <a class="dropdown-item" href="/admin-ui/users">Users</a>
			  <a class="dropdown-item" href="/admin-ui/roles">Roles</a>
			</div>
		  </li>
		  <li class="nav-item dropdown">
			<a class="nav-link dropdown-toggle" href="#" id="navbarDropdown" role="button" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
			  Dropwizard
			</a>
			<div class="dropdown-menu" aria-labelledby="navbarDropdown">
			  <a class="dropdown-item" href="/metrics?pretty=true">Metrics JSON</a>
			  <a class="dropdown-item" href="/threads">Threads</a>
			  <a class="dropdown-item" href="/healthcheck?pretty=true">Health</a>
			  <a class="dropdown-item" href="" onclick="event.preventDefault(); fetch('/tasks/shutdown', {method: 'post'});"><i class="fas fa-power-off text-danger"></i> Shutdown</a>
			</div>
		  </li>
		</ul>
		<!-- Status of the shardNodes -->
		<div>
			<#list ctx.namespaces.shardNodes as key,shardNode>
				<i class="fas fa-circle <#if shardNode.connected>text-success<#else>text-danger</#if>"></i>
			</#list>
		</div>
	  </div>

      <div class="pl-2">
      <button type="button" class="btn btn-secondary" onclick="logout()">Logout</button>
      </div>
	</nav>
	
	<div class="container">
		<#nested/>
	</div>
	
	<script type="application/javascript">
<#noparse>
		async function rest (url, options) {
			var res = await fetch(
				url,
				{
					method: 'get',
					credentials: 'same-origin',
					headers: {
      					'Content-Type': 'application/json'
					},
					...options
				}
			);
			var body = await res.json();
			if(!res.ok) {
			    alert(`Error\nSTATUS: ${res.status}\nBODY: ${JSON.stringify(body)}`)
			}
			return res;
		}
</#noparse>
		function logout(){
			event.preventDefault();
			rest('/${ctx.staticUriElem.ADMIN_SERVLET_PATH}/logout')
			    .then(function () { location.reload() });
		}
	
		function postFile(event, url) {
			event.preventDefault();
			
			let inputs = event.target.getElementsByClassName("restparam");
			if(inputs.length != 1) {
				console.log('Unexpected number of inputs in '+inputs);
			}

			var file;

			for (var i = 0; i < inputs[0].files.length; i++) {
				let file = inputs[0].files[i];
				let reader = new FileReader();
				reader.onload = function(){
					let json = reader.result;
					fetch(url, {method: 'post', credentials: 'same-origin', body: json, headers: {
						"Content-Type": "application/json"
					}})
						.then(function(response){
							if (response.ok) {
								setTimeout(location.reload, 2000);
							}
							else {
								let message = 'Status ' + response.status + ': ' + JSON.stringify(response.json());
	        					console.log(message);
	        					alert(message);
							}
						})
						.catch(function(error) {
							console.log('There has been a problem with posting a file', error.message);
						});
				};
				reader.readAsText(file);
			}
		}


        // Get to recent tab: https://stackoverflow.com/a/19015027
        $('#myTab a').click(function(e) {
          e.preventDefault();
          $(this).tab('show');
        });

        // store the currently selected tab in the hash value
        $("ul.nav-tabs > li > a").on("shown.bs.tab", function(e) {
          var id = $(e.target).attr("href").substr(1);
          window.location.hash = id;
        });

        // on load of the page: switch to the currently selected tab
        var hash = window.location.hash;
        $('#myTab a[href="' + hash + '"]').tab('show');
	</script>
</body>
</html>
</#macro>
<#macro kv k="" v="">
	<#if v?has_content>
		<@kc k=k>${v}</@kc>
	</#if>
</#macro>
<#macro kid k="" v="">
	<#if v?has_content>
		<@kc k=k><code>${v}</code></@kc>
	</#if>
</#macro>
<#macro kc k="">
	<div class="row" style="padding-top:5px">
		<div class="col">${k}</div>
		<div class="col-10"><#nested/></div>
	</div>
</#macro>
<#function si num>
  <#assign order     = num?round?length />
  <#assign thousands = ((order - 1) / 3)?floor />
  <#if (thousands < 0)><#assign thousands = 0 /></#if>
  <#assign siMap = [ {"factor": 1, "unit": ""}, {"factor": 1000, "unit": "K"}, {"factor": 1000000, "unit": "M"}, {"factor": 1000000000, "unit":"G"}, {"factor": 1000000000000, "unit": "T"} ]/>
  <#assign siStr = (num / (siMap[thousands].factor))?string("0.# ") + siMap[thousands].unit />
  <#return siStr />
</#function>