<#macro html title >
	<!doctype html>
	<html lang="en">

	<head>
		<!-- Required meta tags -->
		<meta charset="utf-8">
		<meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">

		<link rel="stylesheet" href="/assets/bootstrap-4.3.1/css/bootstrap.min.css">
		<link rel="stylesheet" href="/assets/fontawesome-5.0.8/css/fa-solid.min.css">
		<link rel="stylesheet" href="/assets/fontawesome-5.0.8/css/fontawesome.min.css">
		<link rel="stylesheet" href="/assets/custom/css/style.css">

		<script src="/assets/jquery-3.3.1/jquery.min.js"></script>
		<script src="/assets/popper-1.12.9/popper.min.js"></script>
		<script src="/assets/bootstrap-4.3.1/js/bootstrap.min.js"></script>
		<script src="/assets/custom/js/script.js"></script>


		<script>
			<#-- Global varaible for csrf used by rest-method  -->
			var csrf_token = "${ctx.csrfToken}"
		</script>

		<title>${title}</title>
	</head>
	<#nested />
	</html>
</#macro>