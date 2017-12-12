// @flow

import React                 from 'react';
import T                     from 'i18n-react';
import type { Dispatch }     from 'redux-thunk';
import { connect }           from 'react-redux';

import { InputSelect }       from '../form-components';

import { setExternalForm }   from './actions';
import { AVAILABLE_FORMS }   from './externalFormTypes';

type PropsType = {
  activeForm: $Keys<typeof AVAILABLE_FORMS>,
  onItemClick: Function,
};

const ExternalFormsNavigation = (props: PropsType) => {
  const options = Object
    .keys(AVAILABLE_FORMS)
    .map(formType => ({
      label: formType,
      value: formType
    }));

  return (
    <div className="external-forms-navigation">
      <InputSelect
        label={T.translate('externalForms.forms')}
        options={options}
        input={{
          value: props.activeForm,
          onChange: (value) => props.onItemClick(value)
        }}
        selectProps={{
          clearable: false,
          autosize: true,
          searchable: false,
        }}
      />
    </div>
  );
};

const mapStateToProps = (state) => ({
  activeForm: state.externalForms.activeForm,
});

const mapDispatchToProps = (dispatch: Dispatch) => ({
  onItemClick: (form) => dispatch(setExternalForm(form)),
});

export default connect(mapStateToProps, mapDispatchToProps)(ExternalFormsNavigation);
