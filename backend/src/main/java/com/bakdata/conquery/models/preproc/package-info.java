package com.bakdata.conquery.models.preproc;
/**
 * Note on the CQPP file format:
 * <p>
 * It is encoded as Smile/BinaryJson-format consisting of three documents:
 * - {@link com.bakdata.conquery.models.preproc.PreprocessedHeader}: metadata of the import.
 * - {@link com.bakdata.conquery.models.preproc.PreprocessedDictionaries}: dictionary encoded strings for the import.
 * - {@link com.bakdata.conquery.models.preproc.PreprocessedData}: the description and raw representation of the data as {@link com.bakdata.conquery.models.events.stores.root.ColumnStore}.
 * <p>
 * The file is split into three sections, so we can load them progressively:
 * Initially, we just read the header and determine if it isn't already loaded, and also fits to the {@link com.bakdata.conquery.models.datasets.Table} it is supposed to go in.
 * We then submit an {@link com.bakdata.conquery.models.jobs.ImportJob} which will load the data.
 * First the {@link com.bakdata.conquery.models.dictionary.Dictionary}s. Those are imported and are potentially altered or ingested into shared-Dictionaries (via {@link com.bakdata.conquery.models.datasets.Column#getSharedDictionary()}).
 * <p>
 * We then load the raw data, having claims for Dictionaries in the import resolved via {@link com.bakdata.conquery.io.jackson.serializer.NsIdRef}, which is why they need to be loaded in a second step.
 * <p>
 * TODO write the rest of the documentation for {@link com.bakdata.conquery.models.jobs.ImportJob}
 */