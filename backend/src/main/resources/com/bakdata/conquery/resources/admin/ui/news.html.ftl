<#import "templates/template.html.ftl" as layout>
<@layout.layout>
	<div class="row">
		<div class="col">
			<h1>News</h1>
			<div class="bg-light px-3 pb-3 pt-1 rounded">
				<form id="news-form" onsubmit="createNews(event)">
					<h3>Create News</h3>
					<div class="form-row">
						<div class="form-group col-md-6">
							<label for="news-title">Title:</label>
							<input id="news-title" data-test-id="news-title" class="form-control" required>
						</div>
						<div class="form-group col-md-6">
							<label for="news-id">ID:</label>
							<input id="news-id" data-test-id="news-id" class="form-control text-monospace" pattern="[a-z0-9_-]+" required maxlength="100">
							<small class="form-text text-muted">Generated from the title until edited. Allowed characters: a-z, 0-9, underscore, and hyphen. Maximum 100 characters.</small>
						</div>
					</div>
					<div class="form-group">
						<label for="news-description">Description:</label>
						<textarea id="news-description" data-test-id="news-description" class="form-control" rows="5" required></textarea>
					</div>
					<div class="form-row">
						<div class="form-group col-md-8">
							<label for="news-link">Link:</label>
							<input id="news-link" data-test-id="news-link" class="form-control" placeholder="https://example.com/news or /news/details">
						</div>
						<div class="form-group col-md-4">
							<label for="news-date">Date:</label>
							<input id="news-date" data-test-id="news-date" class="form-control" type="date" required>
						</div>
					</div>
					<div class="form-group position-relative">
						<div class="d-flex justify-content-between align-items-center">
							<label for="news-dataset-input">Published to datasets:</label>
							<button id="news-add-all-datasets" data-test-id="news-add-all-datasets" class="btn btn-outline-secondary btn-sm mb-1" type="button">Add all</button>
						</div>
						<div id="news-dataset-control" class="dataset-token-input form-control d-flex flex-wrap align-items-center p-1" data-test-id="news-dataset-control">
							<div id="news-selected-datasets" class="d-flex flex-wrap"></div>
							<input id="news-dataset-input" data-test-id="news-dataset-input" class="p-1" autocomplete="off"
								   role="combobox" aria-autocomplete="list" aria-controls="news-dataset-suggestions" aria-expanded="false"
								   placeholder="Type or select a dataset ID">
						</div>
						<div id="news-dataset-suggestions" data-test-id="news-dataset-suggestions" class="dropdown-menu w-100" role="listbox"></div>
						<small class="form-text text-muted">Choose dataset IDs to publish this item. Press Enter or comma to add a custom ID. Leave empty to keep the item unpublished.</small>
					</div>
					<button id="news-submit" data-test-id="create-news-btn" class="btn btn-primary" type="submit">Create</button>
				</form>
			</div>

			<div class="mt-4">
				<h3>All News</h3>
				<div class="table-responsive">
					<table class="table table-sm table-striped text-break" data-test-id="news-table">
						<thead class="text-nowrap">
							<tr>
								<th scope="col">Date</th>
								<th scope="col">Title</th>
								<th scope="col">Description</th>
								<th scope="col">Link</th>
								<th scope="col">Published to datasets</th>
								<th scope="col">ID</th>
								<th scope="col" class="text-right">Actions</th>
							</tr>
						</thead>
						<tbody id="news-list" data-test-id="news-list">
							<tr><td colspan="7" class="text-center">Loading news…</td></tr>
						</tbody>
					</table>
				</div>
			</div>
		</div>
	</div>

	<script>
		const availableDatasetIds = [
			<#list c.datasetIds as datasetId>"${datasetId?js_string}"<#sep>, </#sep></#list>
		];
		let selectedDatasets = [];
		let newsIdEdited = false;

		const newsForm = document.getElementById('news-form');
		const newsTitle = document.getElementById('news-title');
		const newsId = document.getElementById('news-id');
		const newsLink = document.getElementById('news-link');
		const newsDate = document.getElementById('news-date');
		const datasetInput = document.getElementById('news-dataset-input');
		const datasetSuggestions = document.getElementById('news-dataset-suggestions');

		function slugifyTitle(title) {
			return title
				.normalize('NFKD')
				.replace(/[\u0300-\u036f]/g, '')
				.toLowerCase()
				.replace(/[^a-z0-9]+/g, '-')
				.replace(/^-+|-+$/g, '');
		}

		function localDateString(date) {
			const year = date.getFullYear();
			const month = String(date.getMonth() + 1).padStart(2, '0');
			const day = String(date.getDate()).padStart(2, '0');
			return year + '-' + month + '-' + day;
		}

		function renderSelectedDatasets() {
			const container = document.getElementById('news-selected-datasets');
			container.replaceChildren();
			selectedDatasets.forEach(function (dataset) {
				const badge = document.createElement('span');
				badge.className = 'badge badge-secondary d-inline-flex align-items-center mr-1 mb-1 p-2';
				badge.setAttribute('data-test-id', 'news-dataset-token');
				badge.appendChild(document.createTextNode(dataset));

				const removeButton = document.createElement('button');
				removeButton.type = 'button';
				removeButton.className = 'close ml-2 text-white';
				removeButton.setAttribute('aria-label', 'Remove dataset ' + dataset);
				removeButton.innerHTML = '<span aria-hidden="true">&times;</span>';
				removeButton.addEventListener('click', function () {
					selectedDatasets = selectedDatasets.filter(function (selected) { return selected !== dataset; });
					renderSelectedDatasets();
					renderDatasetSuggestions();
				});
				badge.appendChild(removeButton);
				container.appendChild(badge);
			});
		}

		function addDataset(value) {
			const dataset = value.trim();
			if (!dataset || selectedDatasets.includes(dataset)) return false;
			selectedDatasets.push(dataset);
			datasetInput.value = '';
			renderSelectedDatasets();
			renderDatasetSuggestions();
			return true;
		}

		function addAllDatasets() {
			availableDatasetIds.forEach(function (datasetId) {
				if (!selectedDatasets.includes(datasetId)) selectedDatasets.push(datasetId);
			});
			datasetInput.value = '';
			renderSelectedDatasets();
			renderDatasetSuggestions();
		}

		function createSuggestion(label, value) {
			const suggestion = document.createElement('button');
			suggestion.type = 'button';
			suggestion.className = 'dropdown-item';
			suggestion.setAttribute('role', 'option');
			suggestion.setAttribute('data-test-id', 'news-dataset-suggestion');
			suggestion.textContent = label;
			suggestion.addEventListener('mousedown', function (event) {
				event.preventDefault();
				addDataset(value);
				datasetInput.focus();
			});
			return suggestion;
		}

		function renderDatasetSuggestions() {
			const query = datasetInput.value.trim();
			const normalizedQuery = query.toLowerCase();
			const matchingDatasets = availableDatasetIds.filter(function (datasetId) {
				return !selectedDatasets.includes(datasetId) && datasetId.toLowerCase().includes(normalizedQuery);
			});

			datasetSuggestions.replaceChildren();
			if (query && !selectedDatasets.includes(query) && !availableDatasetIds.includes(query)) {
				datasetSuggestions.appendChild(createSuggestion('Add custom dataset ID “' + query + '”', query));
			}
			matchingDatasets.forEach(function (datasetId) {
				datasetSuggestions.appendChild(createSuggestion(datasetId, datasetId));
			});

			const show = document.activeElement === datasetInput && datasetSuggestions.childElementCount > 0;
			datasetSuggestions.classList.toggle('show', show);
			datasetInput.setAttribute('aria-expanded', String(show));
		}

		function validateNewsId() {
			const id = newsId.value.trim();
			if (id.length > 100) {
				newsId.setCustomValidity('ID must not exceed 100 characters');
				return false;
			} else {
				newsId.setCustomValidity('');
				return true;
			}
		}

		function validateNewsLink() {
			const value = newsLink.value.trim();
			newsLink.setCustomValidity('');
			if (!value) return true;
			try {
				const parsed = new URL(value, window.location.origin);
				if (parsed.protocol !== 'http:' && parsed.protocol !== 'https:') {
					newsLink.setCustomValidity('Use an HTTP(S) or relative link.');
				}
			}
			catch (error) {
				newsLink.setCustomValidity('Enter a valid link.');
			}
			return newsLink.checkValidity();
		}

		function resetNewsForm() {
			newsForm.reset();
			newsIdEdited = false;
			selectedDatasets = [];
			newsDate.value = localDateString(new Date());
			renderSelectedDatasets();
			renderDatasetSuggestions();
		}

		async function createNews(event) {
			event.preventDefault();
			if (datasetInput.value.trim()) addDataset(datasetInput.value);
			validateNewsLink();
			validateNewsId();
			if (!newsForm.reportValidity()) return;

			const submitButton = document.getElementById('news-submit');
			submitButton.disabled = true;
			try {
				const response = await rest('/admin/news', {
					method: 'post',
					body: JSON.stringify({
						id: newsId.value.trim(),
						title: newsTitle.value.trim(),
						description: document.getElementById('news-description').value.trim(),
						link: newsLink.value.trim() || null,
						date: newsDate.value,
						publishedToDatasets: selectedDatasets
					})
				});
				if (response.ok) {
					showToastMessage(ToastTypes.SUCCESS, 'Success', 'The news item was created.');
					resetNewsForm();
					await loadNews();
				}
				else {
					await showMessageForResponse(response);
				}
			}
			catch (error) {
				showToastMessage(ToastTypes.ERROR, 'Error', 'The news item could not be created: ' + error.message);
			}
			finally {
				submitButton.disabled = false;
			}
		}

		function appendTextCell(row, value, className) {
			const cell = document.createElement('td');
			if (className) cell.className = className;
			cell.textContent = value || '—';
			row.appendChild(cell);
			return cell;
		}

		function safeLink(value) {
			if (!value) return null;
			try {
				const parsed = new URL(value, window.location.origin);
				return parsed.protocol === 'http:' || parsed.protocol === 'https:' ? parsed : null;
			}
			catch (error) {
				return null;
			}
		}

		function renderNews(newsItems) {
			const newsList = document.getElementById('news-list');
			newsList.replaceChildren();
			if (!newsItems.length) {
				const row = document.createElement('tr');
				const cell = document.createElement('td');
				cell.colSpan = 7;
				cell.className = 'text-center';
				cell.textContent = 'No news found';
				row.appendChild(cell);
				newsList.appendChild(row);
				return;
			}

			newsItems.forEach(function (item) {
				const row = document.createElement('tr');
				row.setAttribute('data-test-id', 'news-row-' + item.id);
				const dateCell = appendTextCell(row, item.date);
				if (item.publicationStatus !== 'PUBLISHED') {
					row.classList.add('table-warning');
					const statusBadge = document.createElement('span');
					statusBadge.className = 'badge badge-warning ml-2';
					statusBadge.setAttribute('data-test-id', 'news-publication-status-' + item.id);
					statusBadge.textContent = item.publicationStatus === 'UNPUBLISHED' ? 'Not published' : 'Not published yet';
					dateCell.appendChild(statusBadge);
				}
				appendTextCell(row, item.title);
				appendTextCell(row, item.description, 'news-description');

				const linkCell = document.createElement('td');
				const parsedLink = safeLink(item.link);
				if (parsedLink) {
					const link = document.createElement('a');
					link.href = parsedLink.href;
					link.target = '_blank';
					link.rel = 'noopener noreferrer';
					link.textContent = item.link;
					linkCell.appendChild(link);
				}
				else {
					linkCell.textContent = item.link || '—';
				}
				row.appendChild(linkCell);

				const datasetsCell = document.createElement('td');
				(item.publishedToDatasets || []).forEach(function (dataset) {
					const badge = document.createElement('span');
					badge.className = 'badge badge-secondary mr-1';
					badge.textContent = dataset;
					datasetsCell.appendChild(badge);
				});
				if (!datasetsCell.childElementCount) datasetsCell.textContent = '—';
				row.appendChild(datasetsCell);
				appendTextCell(row, item.id, 'text-monospace');

				const actionsCell = document.createElement('td');
				actionsCell.className = 'text-right';
				const deleteButton = document.createElement('button');
				deleteButton.type = 'button';
				deleteButton.className = 'btn btn-link btn-sm p-0';
				deleteButton.setAttribute('data-test-id', 'delete-news-' + item.id);
				deleteButton.setAttribute('aria-label', 'Delete news ' + item.title);
				deleteButton.innerHTML = '<i class="fas fa-trash-alt text-danger"></i>';
				deleteButton.addEventListener('click', function () { deleteNews(item); });
				actionsCell.appendChild(deleteButton);
				row.appendChild(actionsCell);
				newsList.appendChild(row);
			});
		}

		async function loadNews() {
			try {
				const response = await rest('/admin/news', { headers: { Accept: 'application/json' } });
				if (!response.ok) {
					await showMessageForResponse(response);
					return;
				}
				const newsItems = await response.json();
				newsItems.sort(function (left, right) {
					return right.date.localeCompare(left.date) || left.id.localeCompare(right.id);
				});
				renderNews(newsItems);
			}
			catch (error) {
				showToastMessage(ToastTypes.ERROR, 'Error', 'News could not be loaded: ' + error.message);
			}
		}

		async function deleteNews(item) {
			if (!window.confirm('Delete news “' + item.title + '” (' + item.id + ')?')) return;
			try {
				const response = await rest('/admin/news/' + encodeURIComponent(item.id), { method: 'delete' });
				if (response.ok) {
					showToastMessage(ToastTypes.SUCCESS, 'Success', 'The news item was deleted.');
					await loadNews();
				}
				else {
					await showMessageForResponse(response);
				}
			}
			catch (error) {
				showToastMessage(ToastTypes.ERROR, 'Error', 'The news item could not be deleted: ' + error.message);
			}
		}

		newsTitle.addEventListener('input', function () {
			if (!newsIdEdited) newsId.value = slugifyTitle(newsTitle.value);
		});
		newsId.addEventListener('input', function () { newsIdEdited = true; });
		newsLink.addEventListener('input', validateNewsLink);
		datasetInput.addEventListener('focus', renderDatasetSuggestions);
		datasetInput.addEventListener('input', renderDatasetSuggestions);
		datasetInput.addEventListener('keydown', function (event) {
			if (event.key === 'Enter' || event.key === ',') {
				event.preventDefault();
				addDataset(datasetInput.value);
			}
			else if (event.key === 'Backspace' && !datasetInput.value && selectedDatasets.length) {
				selectedDatasets.pop();
				renderSelectedDatasets();
				renderDatasetSuggestions();
			}
		});
		document.getElementById('news-add-all-datasets').addEventListener('click', addAllDatasets);
		document.getElementById('news-dataset-control').addEventListener('click', function () { datasetInput.focus(); });
		document.addEventListener('click', function (event) {
			if (!document.getElementById('news-dataset-control').contains(event.target)
					&& !datasetSuggestions.contains(event.target)) {
				datasetSuggestions.classList.remove('show');
				datasetInput.setAttribute('aria-expanded', 'false');
			}
		});

		resetNewsForm();
		loadNews();
	</script>
</@layout.layout>
