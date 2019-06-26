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

	<title>Conquery Admin UI</title>
  </head>
  <body>
  	<script src="https://ajax.googleapis.com/ajax/libs/jquery/3.3.1/jquery.min.js"></script>
	<script src="https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.12.9/umd/popper.min.js" integrity="sha384-ApNbgh9B+Y1QKtv3Rn7W3mgPxhU9K/ScQsAP7hUibX39j7fakFPskvXusvfa0b4Q" crossorigin="anonymous"></script>
	<script src="https://cdnjs.cloudflare.com/ajax/libs/twitter-bootstrap/4.0.0/js/bootstrap.min.js" crossorigin="anonymous"></script>
	
	<nav class="navbar navbar-expand-lg navbar-light bg-light" style="margin-bottom:30px">
	  <a class="navbar-brand" href="/admin">Conquery Admin</a>
	  <button class="navbar-toggler" type="button" data-toggle="collapse" data-target="#navbarSupportedContent" aria-controls="navbarSupportedContent" aria-expanded="false" aria-label="Toggle navigation">
		<span class="navbar-toggler-icon"></span>
	  </button>
	
	  <div class="collapse navbar-collapse" id="navbarSupportedContent">
		<ul class="navbar-nav mr-auto">
		  <li class="nav-item">
			<a class="nav-link" href="/admin/datasets">Datasets</a>
		  </li>
		  <li class="nav-item">
			<a class="nav-link" href="/admin/jobs">Jobs</a>
		  </li>
		  <li class="nav-item">
			<a class="nav-link" href="/admin/script">Script</a>
		  </li>
		  <li class="nav-item">
			<a class="nav-link" href="/admin/mandators">Mandators</a>
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
		<!-- Status of the slaves -->
		<div>
			<#list ctx.namespaces.slaves as key,slave>
				<i class="fas fa-circle <#if slave.connected>text-success<#else>text-danger</#if>"></i>
			</#list>
		</div>
	  </div>
	</nav>
	
	<div class="container">
		<#nested/>
	</div>
	
	<script type="application/javascript">
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
					fetch(url, {method: 'post', body: json, headers: {
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
	</script>
</body>
</html>
</#macro>
<#macro kv k="" v="">
	<#if v?has_content>
		<@kc k=k>${v}</@kc>
	</#if>
</#macro>
<#macro kc k="">
	<div class="row" style="padding-top:5px">
		<div class="col">${k}</div>
		<div class="col-10"><#nested/></div>
	</div>
</#macro>