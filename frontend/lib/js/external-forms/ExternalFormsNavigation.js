// @flow

import React                 from 'react';
import T                     from 'i18n-react';
import type { Dispatch }     from 'redux-thunk';
import { connect }           from 'react-redux';

import { InputSelect }       from '../form-components';

import { setExternalForm }   from './actions';

type PropsType = {
  availableForms: Object,
  activeForm: string,
  onItemClick: Function,
};

const ExternalFormsNavigation = (props: PropsType) => {
  const options = Object.values(props.availableForms)
    .sort((a, b) => a.order - b.order)
    .map(formType => ({
      label: T.translate(formType.headline),
      value: formType.type
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
  availableForms: state.externalForms.availableForms,
  activeForm: state.externalForms.activeForm,
});

const mapDispatchToProps = (dispatch: Dispatch) => ({
  onItemClick: (form) => dispatch(setExternalForm(form)),
});

export default connect(mapStateToProps, mapDispatchToProps)(ExternalFormsNavigation);
