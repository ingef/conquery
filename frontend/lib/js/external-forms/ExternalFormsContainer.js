// @flow

import React                    from 'react';
import { connect }              from 'react-redux';

import UploadConceptListModal   from '../upload-concept-list-modal/UploadConceptListModal';

type PropsType = {
  availableForms: Object,
  activeForm: string,
  datasetId: string
};

const ExternalFormsContainer = (props: PropsType) => {
  const form = React.createElement(
    props.availableForms[props.activeForm].component,
    { selectedDatasetId: props.datasetId }
  );

  return (
    <div className="external-forms-container">
      { form }
      <UploadConceptListModal selectedDatasetId={props.datasetId} />
    </div>
  );
};

const mapStateToProps = (state) => ({
  availableForms: state.externalForms.availableForms,
  activeForm: state.externalForms.activeForm,
});

export default connect(mapStateToProps)(ExternalFormsContainer);
