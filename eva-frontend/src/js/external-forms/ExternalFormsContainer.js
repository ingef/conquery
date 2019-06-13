// @flow

import React from "react";
import styled from "@emotion/styled";
import { connect } from "react-redux";

import UploadConceptListModal from "conquery/lib/js/upload-concept-list-modal/UploadConceptListModal";

type PropsType = {
  availableForms: Object,
  activeForm: string,
  datasetId: string
};

const Root = styled("div")`
  flex-grow: 1;
  overflow-y: auto;
  padding: 0 20px 20px 10px;
`;

const ExternalFormsContainer = (props: PropsType) => {
  const form = React.createElement(
    props.availableForms[props.activeForm].component,
    { selectedDatasetId: props.datasetId }
  );

  return (
    <Root>
      {form}
      <UploadConceptListModal selectedDatasetId={props.datasetId} />
    </Root>
  );
};

const mapStateToProps = state => ({
  availableForms: state.externalForms.availableForms,
  activeForm: state.externalForms.activeForm
});

export default connect(mapStateToProps)(ExternalFormsContainer);
