import React, { FC } from "react";
import styled from "@emotion/styled";

import type { DatasetIdT } from "../api/types";
import Form from "./form/Form";

import { selectFormConfig } from "./stateSelectors";
import type { Form as FormType } from "./config-types";
import { useSelector } from "react-redux";
import { StateT } from "app-types";
import FormConfigSaver from "./FormConfigSaver";

interface PropsT {
  datasetId: DatasetIdT;
}

const Root = styled("div")`
  flex-grow: 1;
  overflow-y: auto;
  padding: 0 20px 20px 10px;
`;

const FormsContainer: FC<PropsT> = ({ datasetId }) => {
  const formConfig = useSelector<StateT, FormType | null>((state) =>
    selectFormConfig(state)
  );

  return (
    <Root>
      {!!formConfig && (
        <>
          <FormConfigSaver activeFormType datasetId={datasetId} />
          <Form config={formConfig} selectedDatasetId={datasetId} />
        </>
      )}
    </Root>
  );
};

export default FormsContainer;
