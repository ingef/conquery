package com.bakdata.conquery.models.query;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;
import jakarta.ws.rs.core.Response;

import com.bakdata.conquery.apiv1.execution.ResultAsset;
import com.bakdata.conquery.io.external.form.ExternalFormBackendApi;
import com.bakdata.conquery.io.result.ExternalResult;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.FormBackendConfig;
import com.google.common.collect.MoreCollectors;
import it.unimi.dsi.fastutil.Pair;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
public class ExternalResultImpl implements ExternalResult {
    private final CountDownLatch latch;

    @Getter
    private final ExternalFormBackendApi api;

    private final FormBackendConfig formBackendConfig;

    @Getter
    private final User serviceUser;

    /**
     * Pairs of external result assets (internal url) and their internal asset builder.
     * The internal asset builder generates the asset url with the context of a user request.
     */
    @Setter
    private List<Pair<ResultAsset, AssetBuilder>> resultsAssetMap = Collections.emptyList();

    @Override
    public CountDownLatch getExecutingLock() {
        return latch;
    }

    @Override
    public Stream<AssetBuilder> getResultAssets() {
        return resultsAssetMap.stream().map(Pair::value);
    }

    @Override
    public Response fetchExternalResult(String assetId) {
        final ResultAsset resultRef = resultsAssetMap.stream()
                .map(Pair::key).filter(a -> a.getAssetId().equals(assetId))
                .collect(MoreCollectors.onlyElement());

        return api.getResult(resultRef.url());
    }
}
