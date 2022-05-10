package com.bakdata.conquery.models.index;

import java.io.IOException;
import java.nio.file.Path;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.OptBoolean;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;

@Slf4j
@CPSType(id = "CSV_LUCENE", base = InternToExternMapper.class)
@RequiredArgsConstructor
public class LuceneInternToExternMapper implements InternToExternMapper {


	@JacksonInject(useInput = OptBoolean.FALSE)
	@JsonIgnore
	private LuceneIndexService luceneIndexService;
	@JsonIgnore
	private IndexSearcher indexSearcher;

	@NotNull
	private final Path csv;
	@NotNull
	private final String internalColumn;
	@NotNull
	private final String externalColumn;


	@Override
	public void init() {
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
