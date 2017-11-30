// @flow

import React                 from 'react';
import T                     from 'i18n-react';
import type { Dispatch }     from 'redux-thunk';
import { connect }           from 'react-redux';

import { InputSelect }       from '../editorComponents';

import { setForm }           from './actions';

type PropsType = {
  availableForms: Object,
  activeForm: string,
  onItemClick: Function,
};

const FormNavigation = (props: PropsType) => {
  const options = Object.values(props.availableForms)
    .map(formType => ({
      label: T.translate(formType.headline),
      value: formType.type
    }));

  return (
    <div className="form-navigation">
      <InputSelect
        label={T.translate('form.forms')}
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
  availableForms: state.form.availableForms,
  activeForm: state.form.activeForm,
});

const mapDispatchToProps = (dispatch: Dispatch) => ({
  onItemClick: (form) => dispatch(setForm(form)),
});

export default connect(mapStateToProps, mapDispatchToProps)(FormNavigation);
