package news;

import java.io.IOException;
import java.util.List;

public interface NewsService extends AutoCloseable {

	String ID_PATTERN = "[a-z0-9_-]+";

	void reloadNews() throws IOException;

	List<NewsItem> getNews(String category);

	void addNewsItem(NewsItem news) throws IOException;

	boolean removeNewsItem(String id) throws IOException;
}
