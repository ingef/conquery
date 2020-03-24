import React from "react";
import styled from "@emotion/styled";
import { connect } from "react-redux";

import type { DatasetIdT } from "../api/types";
import Form from "./form/Form";

import { selectFormConfig } from "./stateSelectors";
import type { Form as FormType } from "./config-types";

type PropsType = {
  formConfig: FormType;
  datasetId: DatasetIdT;
};

const Root = styled("div")`
  flex-grow: 1;
  overflow-y: auto;
  padding: 0 20px 20px 10px;
`;

const mapStateToProps = state => ({
  formConfig: selectFormConfig(state)
});

export default connect(mapStateToProps)((props: PropsType) => {
  return (
    <Root>
      {!!props.formConfig && (
        <Form config={props.formConfig} selectedDatasetId={props.datasetId} />
      )}
    </Root>
  );
});
