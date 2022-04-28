package com.bakdata.conquery.models.index;

import java.io.IOException;
import java.nio.file.Path;

import com.bakdata.conquery.io.cps.CPSType;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;

@Slf4j
@CPSType(id = "CSV_LUCENE", base = InternToExternMapper.class)
public class LuceneInternToExternMapper implements InternToExternMapper {

	@JsonIgnore
	private final IndexSearcher indexSearcher;
	@JsonIgnore
	private final String internalColumn;
	@JsonIgnore
	private final String externalColumn;


	@JsonCreator
	public LuceneInternToExternMapper(
			@JacksonInject LuceneIndexService luceneIndexService,
			Path csv,
			String internalColumn,
			String externalColumn
	) {
		this.internalColumn = internalColumn;
		this.externalColumn = externalColumn;
		indexSearcher = luceneIndexService.getIndexSearcher(csv);
	}

	@SneakyThrows(IOException.class)
	public String external(String internalValue) {
		final TermQuery termQuery = new TermQuery(new Term(internalColumn, internalValue));
		final TopDocs search = indexSearcher.search(termQuery, 1);

		final int resultCount = search.scoreDocs.length;
		if (resultCount < 1) {
			return null;
		}
		if (resultCount > 1) {
			log.warn("Found more than one result ({}) for internal term: {}", resultCount, internalValue);
		}

		final Document doc = indexSearcher.doc(search.scoreDocs[0].doc);
		return doc.get(externalColumn);
	}

}
