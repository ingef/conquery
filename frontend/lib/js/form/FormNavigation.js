// @flow

import React                 from 'react';
import T                     from 'i18n-react';
import type { Dispatch }     from 'redux-thunk';
import { connect }           from 'react-redux';

import { InputSelect }       from '../editorComponents';

import { setForm }           from './actions';
import { AVAILABLE_FORMS }   from './formTypes';

type PropsType = {
  activeForm: $Keys<typeof AVAILABLE_FORMS>,
  onItemClick: Function,
};

const FormNavigation = (props: PropsType) => {
  const options = Object
    .keys(AVAILABLE_FORMS)
    .map(formType => ({
      label: formType,
      value: formType
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
  activeForm: state.form.activeForm,
});

const mapDispatchToProps = (dispatch: Dispatch) => ({
  onItemClick: (form) => dispatch(setForm(form)),
});

export default connect(mapStateToProps, mapDispatchToProps)(FormNavigation);
