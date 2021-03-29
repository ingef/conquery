import React, { useEffect } from "react";
import styled from "@emotion/styled";
import { useTranslation } from "react-i18next";

import type { DatasetIdT } from "../../api/types";

import EmptyList from "../../list/EmptyList";
import Loading from "../../list/Loading";
import FormConfigs from "./FormConfigs";
import FormConfigsSearchBox from "./search/FormConfigsSearchBox";
import FormConfigsFilter from "./filter/FormConfigsFilter";
import { useFilteredFormConfigs, useLoadFormConfigs } from "./selectors";

const Container = styled("div")`
  overflow-y: auto;
  font-size: ${({ theme }) => theme.font.sm};
  padding: 0 10px;
`;

interface PropsT {
  datasetId: DatasetIdT | null;
}

const FormConfigsTab = ({ datasetId }: PropsT) => {
  const formConfigs = useFilteredFormConfigs();
  const { loading, loadFormConfigs } = useLoadFormConfigs();
  const { t } = useTranslation();

  const hasConfigs = loading || formConfigs.length !== 0;

  useEffect(() => {
    if (datasetId) {
      loadFormConfigs(datasetId);
    }
  }, [datasetId, loadFormConfigs]);

  if (!datasetId) return null;

  return (
    <>
      <FormConfigsFilter />
      <FormConfigsSearchBox />
      <Container>
        {loading && <Loading message={t("formConfigs.loading")} />}
        {formConfigs.length === 0 && !loading && (
          <EmptyList emptyMessage={t("formConfigs.noneFound")} />
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
