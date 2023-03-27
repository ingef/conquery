<!doctype html>
<html lang="en">
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
			  <button id="button-login" class="btn btn-lg btn-primary btn-block" type="submit">Sign in</button>
		  </form>
	  </div>

    <div class="col"></div>
    </div>
    <script src="/assets/jquery-3.3.1/jquery.min.js"></script>
		<script src="/assets/bootstrap-4.3.1/js/bootstrap.min.js"></script>

        <script>
            function loginClickHandler(){
                event.preventDefault();
                fetch(
                    '/auth',
                    {
                        method: 'POST',
                        headers: {'Content-Type': 'application/json'},
                        body: JSON.stringify({
                            user: document.getElementById("inputEmail").value,
                            password: document.getElementById("inputPassword").value
                        })
                    })
                    .then((response) => {
                        if(!response.ok) {
                            throw new Error("Error fetching token");
                        }
                        return response.json();
                    })
                    .then( (json) => {
                        window.location = '${c}?access_token='+json.access_token;
                    }
                    )
                    .catch(function(error) {
                        var p = document.createElement('p');
                        p.appendChild(
                            document.createTextNode('Error: ' + error.message)
                        );
                        document.body.insertBefore(p, myImage);
                    });                    
            }

            document.getElementById("button-login").addEventListener("click",loginClickHandler)
        </script>
    </body>

</html>