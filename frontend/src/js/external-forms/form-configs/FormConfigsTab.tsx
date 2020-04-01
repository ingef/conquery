import React, { useEffect, useState } from "react";
import styled from "@emotion/styled";
import { useDispatch, useSelector } from "react-redux";

import type { DatasetIdT } from "../../api/types";

import { loadFormConfigsSuccess, loadFormConfigsError } from "./actions";
import { selectFormConfigs } from "./selectors";
import { T } from "js/localization";
import EmptyList from "js/list/EmptyList";
import { FormConfigT } from "./reducer";
import { StateT } from "app-types";
import { getFormConfigs } from "js/api/api";
import Loading from "js/list/Loading";
import FormConfigs from "./FormConfigs";

const Container = styled("div")`
  overflow-y: auto;
  font-size: ${({ theme }) => theme.font.sm};
  padding: 0 10px;
`;

type PropsT = {
  datasetId: DatasetIdT | null;
};

const FormConfigsTab = ({ datasetId }: PropsT) => {
  const [loading, setLoading] = useState<boolean>(false);

  const formConfigs = useSelector<StateT, FormConfigT[]>(state =>
    selectFormConfigs(
      state.formConfigs.data,
      state.previousQueriesSearch,
      state.previousQueriesFilter
    )
  );
  const dispatch = useDispatch();

  const hasConfigs = loading || formConfigs.length !== 0;

  useEffect(() => {
    async function loadConfigs(dataset: DatasetIdT) {
      setLoading(true);
      try {
        const data = await getFormConfigs(dataset);

        dispatch(loadFormConfigsSuccess(data));
      } catch (e) {
        dispatch(loadFormConfigsError(e));
      }
      setLoading(false);
    }

    if (datasetId) {
      loadConfigs(datasetId);
    }
  }, [dispatch, datasetId]);

  if (!datasetId) return null;

  return (
    <>
      <Container>
        {loading && <Loading message={T.translate("formConfigs.loading")} />}
        {formConfigs.length === 0 && !loading && (
          <EmptyList emptyMessage={T.translate("formConfigs.noneFound")} />
        )}
      </Container>
      {hasConfigs && (
        <>
          <FormConfigs formConfigs={formConfigs} datasetId={datasetId} />
        </>
      )}
    </>
  );
};

// <DeleteFormConfigModal datasetId={datasetId} />
// <PreviousQueriesFilter />
// <PreviousQueriesSearchBox />

export default FormConfigsTab;
