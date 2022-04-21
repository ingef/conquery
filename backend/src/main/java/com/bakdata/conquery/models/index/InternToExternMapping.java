package com.bakdata.conquery.models.index;

import java.io.IOException;
import java.nio.file.Path;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.xml.builders.TermQueryBuilder;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;

@Slf4j
public class InternToExternMapping {

	@JsonIgnore
	private final IndexSearcher indexSearcher;
	@JsonIgnore
	private final String internalColumn;
	@JsonIgnore
	private final String externalColumn;


	@JsonCreator
	public InternToExternMapping(
			@JacksonInject IndexService indexService,
			Path csv,
			String internalColumn,
			String externalColumn
	) {
		this.internalColumn = internalColumn;
		this.externalColumn = externalColumn;
		indexSearcher = indexService.getIndexSearcher(csv);
	}

	public String external(String internalValue) throws IOException {
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
