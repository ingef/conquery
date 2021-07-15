<#import "templates/template.html.ftl" as layout>
<@layout.layout>
	<div class="row">
		<div class="col">
			<ul id="datasets" >
			</ul>
			<br/>
			<form>
				<div class="form-group">
				    <h3>Create Dataset</h3>
                    <label for="entity_name">Name:</label>
                    <input id="entity_name" name="entity_name" pattern="<#include "templates/namePattern.ftl">" class="form-control text-monospace" style="font-family:monospace;">
                    <label for="entity_id">ID:</label>
                    <input id="entity_id" name="entity_id"  class="form-control text-monospace" style="font-family:monospace;">
                    <input class="btn btn-primary" type="submit" onclick="createDataset()"/>
				</div>
			</form>

		</div>
	</div>
    <script>
        function reloadDatasets() {
            if (this.readyState == 4 && this.status == 200) {
                var datasets = JSON.parse(this.responseText);
                var ul = document.getElementById('datasets');
                while (ul.firstChild) {
                    ul.removeChild(ul.lastChild);
                }

                datasets.forEach(dataset => {
                    var li = document.createElement("li");
                    var a = document.createElement('a');
                    a.appendChild(document.createTextNode(dataset));
                    li.appendChild(a)
                    a.title = dataset;
                    a.href = `/admin-ui/datasets/${r"${dataset}"}`;
                    ul.appendChild(li);

                })
            }
        };

        function renderDatasets() {
            var req = new XMLHttpRequest();

            req.open('GET', '/admin/datasets', true);
            req.setRequestHeader('Accept', 'application/json');
            req.setRequestHeader('Authorization', 'Bearer ' + keycloak.token);

            req.onreadystatechange = function reloadDatasets() {
                if (this.readyState == 4 && this.status == 200) {
                    var datasets = JSON.parse(this.responseText);
                    var ul = document.getElementById('datasets');
                    while (ul.firstChild) {
                        ul.removeChild(ul.lastChild);
                    }

                    datasets.forEach(dataset => {
                            var li = document.createElement("li");
                            var a = document.createElement('a');
                            a.appendChild(document.createTextNode(dataset));
                            li.appendChild(a)
                            a.title = dataset;
                            a.href = `/admin-ui/datasets/${r"${dataset}"}`;
                            ul.appendChild(li);
                    })
                    }
                };
            req.send();
        }

        function createDataset() {
            event.preventDefault();
            fetch('/admin/datasets',
            {
                method: 'post',
                headers: getHeader(),
                body: JSON.stringify({
                        name: document.getElementById('entity_id').value,
                        label: document.getElementById('entity_name').value
                    })
            }).then(renderDatasets);
        }

        onloadListeners.push(renderDatasets)
    </script>
</@layout.layout>