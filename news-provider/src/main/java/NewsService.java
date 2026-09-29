import java.io.IOException;
import java.util.List;

public interface NewsService {
	List<NewsItem> getNews(String category);

	void addNewsItem(NewsItem news) throws IOException;
}
