// Bootstrap enable tooltips everywhere
$(function () { $('[data-toggle="tooltip"]').tooltip() })

const ToastTypes = {
	INFO: "badge-info",
	SUCCESS: "badge-success",
	WARNING: "badge-warning",
	ERROR: "badge-danger"
};

function createCustomButton(text) {
	const customButton = document.createElement('button');
	customButton.setAttribute('type', 'button');
	customButton.setAttribute('data-test-id', 'toast-custom-button');
	customButton.classList = 'mt-3 btn btn-danger';
	customButton.innerText = text;
	return customButton;
}

function toForceURL(url) {
	const forceURL = new URL(url, window.location);
	forceURL.searchParams.append('force', true);
	return forceURL;
}

async function rest(url, options) {
	var res = await fetch(
		url,
		{
			method: 'get',
			credentials: 'same-origin',
			...options,
			// Overwrite options[headers] here, BUT merge them with our headers
			headers: {
				'Content-Type': 'application/json',
				// Csrf token is provided via global variable
				'X-Csrf-Token': csrf_token,
				...options?.headers,
			}
		}
	);
	return res;
}

async function restOptionalForce(url, options) {
	return rest(url, options).then((res) => {
		// force button in case of 409 status
		const customButton = createCustomButton('Force delete');
		customButton.onclick = () => rest(toForceURL(url), options).then((res) => {
			res.ok && location.reload();
		});

		showMessageForResponse(res, customButton);
		return res;
	});
}

function getToast(type, title, text, smalltext = "", customButton) {
	if (!type) type = ToastTypes.INFO;

	let toast = document.createElement("div");
	toast.classList.add("toast");
	toast.setAttribute("role", "alert");
	toast.setAttribute("aria-live", "assertive");
	toast.setAttribute("aria-atomic", "true");
	toast.setAttribute("style", "width: 500px; max-width: none");
	toast.setAttribute('data-test-id', 'toast');
	toast.innerHTML = `
		<div class="toast-header">
		<strong class="mr-auto badge badge-pill ${type}" style="font-size: 1rem;">${title}</strong>
		${smalltext ? '<small class="text-muted">' + smalltext + '</small>' : ''}
		<button type="button" class="ml-2 mb-1 close" data-dismiss="toast" aria-label="Close">
		<span aria-hidden="true">&times;</span>
		</button>
		</div>
		<div class="toast-body">
		<div style="white-space: normal; overflow-x: auto;">
		${text.trim()}
		</div>
		</div>
		`;
	if (customButton) {
		toast.querySelector('.toast-body').appendChild(customButton);
	}
	return toast;
}



function showToastMessage(type, title, text, smalltext = "", customButton) {
	let toastContainer = document.getElementById("toast-container");
	let toast = getToast(type, title, text, smalltext, customButton);
	toastContainer.appendChild(toast);
	$(toast).toast({ delay: 10000 });
	$(toast).toast('show');
}

async function showMessageForResponse(response, customButton) {
	if (response) {
		try {
			let body = await response.json();
			if (!response.ok) {
				showToastMessage(
					ToastTypes.ERROR,
					"Error",
					"The send request came back with the following error: " + JSON.stringify(body),
					"Status " + response.status,
					response.status == 409 ? customButton : undefined
				);
			}
		} catch (e) {
			// ignore, because some responses don't have a body
		}
	}
}


function loginClickHandler() {
	event.preventDefault();
	fetch(
		'/auth',
		{
			method: 'POST',
			headers: { 'Content-Type': 'application/json' },
			body: JSON.stringify({
				user: document.getElementById("inputEmail").value,
				password: document.getElementById("inputPassword").value
			})
		})
		.then((response) => {
			if (!response.ok) {
				throw new Error("Error fetching token");
			}
			return response.json();
		})
		.then((json) => {
			var searchParams = new URLSearchParams(window.location.search);
			var redirect = new URL(searchParams.get("redirect_uri"))
			redirect.searchParams.append("access_token", json.access_token)

			// This triggers a page reload
			window.location = redirect.toString();
		})
		.catch(function (error) {
			var p = document.createElement('p');
			p.appendChild(
				document.createTextNode('Error: ' + error.message)
			);
			document.getElementById('login-form').insertBefore(p, document.getElementById('login-button'));
		});
}

function postFile(event, url) {
	event.preventDefault();

	let inputs = event.target.getElementsByClassName("restparam");
	if (inputs.length != 1) {
		console.log('Unexpected number of inputs in ' + inputs);
	}

	var file;

	for (var i = 0; i < inputs[0].files.length; i++) {
		let file = inputs[0].files[i];
		let reader = new FileReader();
		reader.onload = function () {
			let json = reader.result;
			rest(url, {
				method: 'post',
				body: json
			})
				.then(function (response) {
					if (response.ok) {
						setTimeout(() => location.reload(), 2000);
						showToastMessage(ToastTypes.SUCCESS, "Success", "The file has been posted successfully");
					} else {
						// force button in case of 409 status
						let customButton;
						// only apply for concept uploads
						if (toForceURL(url).pathname.includes('/concepts')) {
							customButton = createCustomButton('Replace file');
							customButton.onclick = () => postFile(event, toForceURL(url));
						}
						showMessageForResponse(response, customButton);
					}
				})
				.catch(function (error) {
					showToastMessage(ToastTypes.ERROR, "Error", "There has been a problem with posting a file: " + error.message);
					console.log('There has been a problem with posting a file', error.message);
				});
		};
		reader.readAsText(file);
	}
}

function postScriptHandler(event, jsonOut, responseTargetTextfield) {
	event.preventDefault();
	responseTargetTextfield.innerHTML = 'waiting for response';
	rest('/admin/script',
		{
			method: 'post',
			body: document.getElementById('script').value,
			headers: {
				'Content-Type': 'text/plain',
				'Accept': !!jsonOut ? 'application/json' : 'text/plain'
			}
		}).then(response => response.text().then(function (text) { responseTargetTextfield.innerHTML = text }))
}


// Get to recent tab: https://stackoverflow.com/a/19015027
$('#myTab a').click(function (e) {
	e.preventDefault();
	$(this).tab('show');
});

// store the currently selected tab in the hash value
$("ul.nav-tabs > li > a").on("shown.bs.tab", function (e) {
	var id = $(e.target).attr("href").substr(1);
	window.location.hash = id;
});

// on load of the page: switch to the currently selected tab
var hash = window.location.hash;
$('#myTab a[href="' + hash + '"]').tab('show');

const uploadFormMapping = {
	mapping: {
		name: "mapping",
		uri: "internToExtern",
		accept: "*.mapping.json",
	},
	table: {
		name: "table_schema",
		uri: "tables",
		accept: "*.table.json"
	},
	concept: {
		name: "concept_schema",
		uri: "concepts",
		accept: "*.concept.json",
	},
	structure: {
		name: "structure_schema",
		uri: "structure",
		accept: "structure.json",
	},
};

function updateDatasetUploadForm(select, datasetId) {
	const data = uploadFormMapping[select.value];
	const fileInput = $(select).next();
	fileInput.value = "";
	fileInput.attr("accept", data.accept);
	fileInput.attr("name", data.name);
	$(select)
		.parent()
		.attr(
			"onsubmit",
			`postFile(event, '/admin/datasets/${datasetId}/${data.uri}')`
		);
}

function createDataset(event) {
	event.preventDefault();
	rest(
		'/admin/datasets',
		{
			method: 'post',
			body: JSON.stringify({
				name: document.getElementById('entity_id').value,
				label: document.getElementById('entity_name').value
			})
		}).then(function (res) {
			if (res.ok)
				location.reload();
			else
				showMessageForResponse(res);
		});
}



function deleteDataset(event, datasetId) {
	event.preventDefault();
	rest(
		`/admin/datasets/${datasetId}`,
		{
			method: 'delete',
		}).then(function (res) {
			if (res.ok)
				location.reload();
			else
				showMessageForResponse(res);
		});
}

function shutdown(event) {
	event.preventDefault();
	rest(
		'/tasks/shutdown',
		{
			method: 'post'
		}
	);
}