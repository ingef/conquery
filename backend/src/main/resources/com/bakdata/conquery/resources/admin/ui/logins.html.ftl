<#import "templates/base.html.ftl" as base>
<@base.html "Conquery Admin UI">

	<body class="container">
		<div class="row min-vh-100">
			<div class="col"></div>
			<div class="col-7 align-self-center">
				<p class="h1 text-center mb-3 display-4">Conquery</p>
				<p class="text-center lead">Available Logins</p>
				<div class="row justify-content-md-center">
					<ul class="col list-group">
						<#list c as login_schema>
							<a href="${login_schema}"class="list-group-item list-group-item-action text-truncate">
									${(login_schema.getHost())!""}${login_schema.getPath()}
							</a>
						</#list>
					</ul>
				</div>
			</div>
			<div class="col"></div>
		</div>
	</body>
</@base.html>