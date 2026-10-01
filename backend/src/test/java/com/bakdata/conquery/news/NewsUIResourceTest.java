package com.bakdata.conquery.news;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

import com.bakdata.conquery.io.freemarker.Freemarker;
import com.bakdata.conquery.news.NewsUIResource.NewsUIModel;
import com.bakdata.conquery.resources.admin.ui.model.UIContext;
import com.bakdata.conquery.resources.admin.ui.model.UIView;
import org.junit.jupiter.api.Test;

class NewsUIResourceTest {

	@Test
	void rendersNewsPageWithDatasetSuggestions() throws Exception {
		UIView<NewsUIModel> view = new UIView<>(
				"news.html.ftl",
				new UIContext(List::of, "csrf-token"),
				new NewsUIModel(List.of("dataset-a", "dataset-b"))
		);
		ByteArrayOutputStream output = new ByteArrayOutputStream();

		Freemarker.HTML_RENDERER.render(view, Locale.ROOT, output);

		String html = output.toString(StandardCharsets.UTF_8);
		assertThat(html)
				.contains("<h1>News</h1>")
				.contains("const availableDatasetIds = [\n\t\t\t\"dataset-a\", \"dataset-b\"")
				.contains("var csrf_token = \"csrf-token\"");
	}
}
