import styled from "@emotion/styled";
import { StateT } from "app-types";
import React, { useEffect } from "react";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";

import type { DatasetIdT } from "../../api/types";
import EmptyList from "../../list/EmptyList";
import Loading from "../../list/Loading";
import { canUploadResult } from "../../user/selectors";
import PreviousQueriesFilter from "../filter/PreviousQueriesFilter";
import PreviousQueriesSearchBox from "../search/PreviousQueriesSearchBox";
import UploadQueryResults from "../upload/UploadQueryResults";

import PreviousQueries from "./PreviousQueries";
import { useLoadPreviousQueries } from "./actions";
import { PreviousQueryT } from "./reducer";
import { selectPreviousQueries } from "./selector";

const Container = styled("div")`
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
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
      state.previousQueriesFilter,
    ),
  );
  const loading = useSelector<StateT, boolean>(
    (state) => state.previousQueries.loading,
  );
  const hasPermissionToUpload = useSelector<StateT, boolean>((state) =>
    canUploadResult(state),
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
