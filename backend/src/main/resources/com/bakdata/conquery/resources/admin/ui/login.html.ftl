<!doctype html>
<html lang="en">
	<head>
	<meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">

	<title>Conquery Admin Login</title>
    <link rel="stylesheet" href="/assets/bootstrap-4.3.1/css/bootstrap.min.css">

    </head>


    <body class="container">
    <div class="row min-vh-100">
    <div class="col"></div>
	  <div class="col-7 align-self-center">
		  <p class="h1 text-center mb-3 display-4">Conquery</p>
		  <p class="text-center lead">Admin Login</p>
		  <p class="h3 text-center font-weight-normal">Please sign in</p>
		  <form>
			  <div class="form-group">
				<label for="inputEmail">Username</label>
				<input type="text" id="inputEmail" autocomplete="username" class="form-control" placeholder="Enter username" required autofocus>
			  </div>
			  <div class="form-group">
				<label for="inputPassword">Password</label>
				<input type="password" id="inputPassword" autocomplete="current-password" class="form-control" placeholder="Enter password" required>
			  </div>
			  <button id="button-login" class="btn btn-lg btn-primary btn-block" type="submit" onclick="loginClickHandler()">Sign in</button>
		  </form>
	  </div>

    <div class="col"></div>
    </div>
    <script src="/assets/jquery-3.3.1/jquery.min.js"></script>
	<script src="/assets/bootstrap-4.3.1/js/bootstrap.min.js"></script>
	<script src="/assets/custom/js/script.js"></script>
    </body>

</html>