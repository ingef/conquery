import React, { useEffect } from "react";
import styled from "@emotion/styled";
import { useSelector } from "react-redux";
import { StateT } from "app-types";
import { useTranslation } from "react-i18next";

import type { DatasetIdT } from "../../api/types";
import PreviousQueriesSearchBox from "../search/PreviousQueriesSearchBox";
import PreviousQueriesFilter from "../filter/PreviousQueriesFilter";
import PreviousQueries from "./PreviousQueries";
import UploadQueryResults from "../upload/UploadQueryResults";

import { useLoadPreviousQueries } from "./actions";
import { selectPreviousQueries } from "./selector";
import { canUploadResult } from "../../user/selectors";

import EmptyList from "../../list/EmptyList";
import Loading from "../../list/Loading";
import { PreviousQueryT } from "./reducer";

const Container = styled("div")`
  overflow-y: auto;
  font-size: ${({ theme }) => theme.font.sm};
  padding: 0 10px;
`;

interface PropsT {
  datasetId: DatasetIdT | null;
}

const PreviousQueryEditorTab = ({ datasetId }: PropsT) => {
  const { t } = useTranslation();
  const queries = useSelector<StateT, PreviousQueryT[]>((state) =>
    selectPreviousQueries(
      state.previousQueries.queries,
      state.previousQueriesSearch,
      state.previousQueriesFilter
    )
  );
  const loading = useSelector<StateT, boolean>(
    (state) => state.previousQueries.loading
  );
  const hasPermissionToUpload = useSelector<StateT, boolean>((state) =>
    canUploadResult(state)
  );

  const loadPreviousQueries = useLoadPreviousQueries();

  const hasQueries = loading || queries.length !== 0;

  useEffect(() => {
    if (datasetId) {
      loadPreviousQueries(datasetId);
    }
  }, [datasetId]);

  return (
    <>
      <PreviousQueriesFilter />
      <PreviousQueriesSearchBox />
      {hasPermissionToUpload && <UploadQueryResults datasetId={datasetId} />}
      <Container>
        {loading && <Loading message={t("previousQueries.loading")} />}
        {queries.length === 0 && !loading && (
          <EmptyList emptyMessage={t("previousQueries.noQueriesFound")} />
        )}
      </Container>
      {hasQueries && (
        <PreviousQueries queries={queries} datasetId={datasetId} />
      )}
    </>
  );
};

export default PreviousQueryEditorTab;
