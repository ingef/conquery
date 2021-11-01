import styled from "@emotion/styled";
import { useEffect } from "react";
import { useTranslation } from "react-i18next";

import type { DatasetIdT } from "../../api/types";
import EmptyList from "../../list/EmptyList";
import Loading from "../../list/Loading";

import FormConfigs from "./FormConfigs";
import FormConfigsFilter from "./filter/FormConfigsFilter";
import FormConfigsSearchBox from "./search/FormConfigsSearchBox";
import { useFilteredFormConfigs, useLoadFormConfigs } from "./selectors";

const Container = styled("div")`
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
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

  return (
    <>
      <FormConfigsSearchBox />
      <FormConfigsFilter />
      <Container>
        {loading && <Loading message={t("formConfigs.loading")} />}
        {formConfigs.length === 0 && !loading && (
          <EmptyList emptyMessage={t("formConfigs.noneFound")} />
        )}
      </Container>
      {hasConfigs && <FormConfigs formConfigs={formConfigs} />}
    </>
  );
};

export default FormConfigsTab;
