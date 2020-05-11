import React, { useEffect, useState } from "react";
import styled from "@emotion/styled";
import { useDispatch } from "react-redux";

import type { DatasetIdT } from "../../api/types";

import { loadFormConfigsSuccess, loadFormConfigsError } from "./actions";
import { T } from "../../localization";
import EmptyList from "../../list/EmptyList";
import { getFormConfigs } from "../../api/api";
import Loading from "../../list/Loading";
import FormConfigs from "./FormConfigs";
import FormConfigsSearchBox from "./search/FormConfigsSearchBox";
import FormConfigsFilter from "./filter/FormConfigsFilter";
import { useFilteredFormConfigs } from "./selectors";

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

  const formConfigs = useFilteredFormConfigs();
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
      <FormConfigsFilter />
      <FormConfigsSearchBox />
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

export default FormConfigsTab;
