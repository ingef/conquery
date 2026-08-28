package com.bakdata.conquery.models.preproc;
/**
 * Note on the CQPP file format:
 * <p>
 * It is encoded as Smile/BinaryJson-format consisting of three documents:
 * - {@link com.bakdata.conquery.models.preproc.PreprocessedHeader}: metadata of the import.
 * - {@link com.bakdata.conquery.models.preproc.PreprocessedData}: the description and raw representation of the data as {@link com.bakdata.conquery.models.events.stores.root.ColumnStore}.
 * <p>
 * The file is split into three sections, so we can load them progressively:
 * Initially, we just read the header and determine if it isn't already loaded, and also fits to the {@link com.bakdata.conquery.models.datasets.Table} it is supposed to go in.
 * We then distribute each contained {@link com.bakdata.conquery.models.events.Bucket} to a {@link com.bakdata.conquery.models.worker.Worker} on a {@link com.bakdata.conquery.commands.ShardNode}.
 */
