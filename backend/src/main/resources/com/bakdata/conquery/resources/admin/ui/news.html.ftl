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
							<input id="news-id" data-test-id="news-id" class="form-control text-monospace" pattern="[a-z0-9_-]+" required>
							<small class="form-text text-muted">Generated from the title until edited. Allowed characters: a-z, 0-9, underscore, and hyphen.</small>
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
						<label for="news-category-input">Categories:</label>
						<div id="news-category-control" class="category-token-input form-control d-flex flex-wrap align-items-center p-1" data-test-id="news-category-control">
							<div id="news-selected-categories" class="d-flex flex-wrap"></div>
							<input id="news-category-input" data-test-id="news-category-input" class="p-1" autocomplete="off"
								   role="combobox" aria-autocomplete="list" aria-controls="news-category-suggestions" aria-expanded="false"
								   placeholder="Type or select a category">
						</div>
						<div id="news-category-suggestions" data-test-id="news-category-suggestions" class="dropdown-menu w-100" role="listbox"></div>
						<small class="form-text text-muted">Choose a dataset ID to publish the item on specific datasets. Press Enter or comma to add it. Leave empty to publish on all datasets.</small>
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
								<th scope="col">Categories</th>
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
		let selectedCategories = [];
		let newsIdEdited = false;

		const newsForm = document.getElementById('news-form');
		const newsTitle = document.getElementById('news-title');
		const newsId = document.getElementById('news-id');
		const newsLink = document.getElementById('news-link');
		const newsDate = document.getElementById('news-date');
		const categoryInput = document.getElementById('news-category-input');
		const categorySuggestions = document.getElementById('news-category-suggestions');

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

		function renderSelectedCategories() {
			const container = document.getElementById('news-selected-categories');
			container.replaceChildren();
			selectedCategories.forEach(function (category) {
				const badge = document.createElement('span');
				badge.className = 'badge badge-secondary d-inline-flex align-items-center mr-1 mb-1 p-2';
				badge.setAttribute('data-test-id', 'news-category-token');
				badge.appendChild(document.createTextNode(category));

				const removeButton = document.createElement('button');
				removeButton.type = 'button';
				removeButton.className = 'close ml-2 text-white';
				removeButton.setAttribute('aria-label', 'Remove category ' + category);
				removeButton.innerHTML = '<span aria-hidden="true">&times;</span>';
				removeButton.addEventListener('click', function () {
					selectedCategories = selectedCategories.filter(function (selected) { return selected !== category; });
					renderSelectedCategories();
					renderCategorySuggestions();
				});
				badge.appendChild(removeButton);
				container.appendChild(badge);
			});
		}

		function addCategory(value) {
			const category = value.trim();
			if (!category || selectedCategories.includes(category)) return false;
			selectedCategories.push(category);
			categoryInput.value = '';
			renderSelectedCategories();
			renderCategorySuggestions();
			return true;
		}

		function createSuggestion(label, value) {
			const suggestion = document.createElement('button');
			suggestion.type = 'button';
			suggestion.className = 'dropdown-item';
			suggestion.setAttribute('role', 'option');
			suggestion.setAttribute('data-test-id', 'news-category-suggestion');
			suggestion.textContent = label;
			suggestion.addEventListener('mousedown', function (event) {
				event.preventDefault();
				addCategory(value);
				categoryInput.focus();
			});
			return suggestion;
		}

		function renderCategorySuggestions() {
			const query = categoryInput.value.trim();
			const normalizedQuery = query.toLowerCase();
			const matchingDatasets = availableDatasetIds.filter(function (datasetId) {
				return !selectedCategories.includes(datasetId) && datasetId.toLowerCase().includes(normalizedQuery);
			});

			categorySuggestions.replaceChildren();
			if (query && !selectedCategories.includes(query) && !availableDatasetIds.includes(query)) {
				categorySuggestions.appendChild(createSuggestion('Add custom category “' + query + '”', query));
			}
			matchingDatasets.forEach(function (datasetId) {
				categorySuggestions.appendChild(createSuggestion(datasetId, datasetId));
			});

			const show = document.activeElement === categoryInput && categorySuggestions.childElementCount > 0;
			categorySuggestions.classList.toggle('show', show);
			categoryInput.setAttribute('aria-expanded', String(show));
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
			selectedCategories = [];
			newsDate.value = localDateString(new Date());
			renderSelectedCategories();
			renderCategorySuggestions();
		}

		async function createNews(event) {
			event.preventDefault();
			if (categoryInput.value.trim()) addCategory(categoryInput.value);
			validateNewsLink();
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
						categories: selectedCategories
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
				appendTextCell(row, item.date);
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

				const categoriesCell = document.createElement('td');
				(item.categories || []).forEach(function (category) {
					const badge = document.createElement('span');
					badge.className = 'badge badge-secondary mr-1';
					badge.textContent = category;
					categoriesCell.appendChild(badge);
				});
				if (!categoriesCell.childElementCount) categoriesCell.textContent = '—';
				row.appendChild(categoriesCell);
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
		categoryInput.addEventListener('focus', renderCategorySuggestions);
		categoryInput.addEventListener('input', renderCategorySuggestions);
		categoryInput.addEventListener('keydown', function (event) {
			if (event.key === 'Enter' || event.key === ',') {
				event.preventDefault();
				addCategory(categoryInput.value);
			}
			else if (event.key === 'Backspace' && !categoryInput.value && selectedCategories.length) {
				selectedCategories.pop();
				renderSelectedCategories();
				renderCategorySuggestions();
			}
		});
		document.getElementById('news-category-control').addEventListener('click', function () { categoryInput.focus(); });
		document.addEventListener('click', function (event) {
			if (!document.getElementById('news-category-control').contains(event.target)
					&& !categorySuggestions.contains(event.target)) {
				categorySuggestions.classList.remove('show');
				categoryInput.setAttribute('aria-expanded', 'false');
			}
		});

		resetNewsForm();
		loadNews();
	</script>
</@layout.layout>
