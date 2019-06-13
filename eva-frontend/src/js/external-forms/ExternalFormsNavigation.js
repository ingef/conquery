// @flow

import React from "react";
import styled from "@emotion/styled";
import type { Dispatch } from "redux-thunk";
import { connect } from "react-redux";

import { setExternalForm } from "./actions";

import InputSelect from "conquery/lib/js/form-components/InputSelect";
import { T } from "conquery/lib/js/localization";

type PropsType = {
  availableForms: Object,
  activeForm: string,
  onItemClick: Function,
  onClearForm: Function
};

const Root = styled("div")`
  flex-shrink: 0;
  margin-bottom: 10px;
  padding: 0 20px 0 10px;
  box-sizing: border-box;
  display: flex;
  flex-direction: row;
  align-items: flex-start;
`;

const ExternalFormsNavigation = (props: PropsType) => {
  const options = Object.values(props.availableForms)
    .sort((a, b) => a.order - b.order)
    .map(formType => ({
      label: T.translate(formType.headline),
      value: formType.type
    }));

  return (
    <Root>
      <InputSelect
        label={T.translate("externalForms.forms")}
        options={options}
        input={{
          value: props.activeForm,
          onChange: value => props.onItemClick(value)
        }}
        selectProps={{
          clearable: false,
          autosize: true,
          searchable: false
        }}
      />
    </Root>
  );
};

const mapStateToProps = state => ({
  availableForms: state.externalForms.availableForms,
  activeForm: state.externalForms.activeForm
});

const mapDispatchToProps = (dispatch: Dispatch) => ({
  onItemClick: form => dispatch(setExternalForm(form))
});

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(ExternalFormsNavigation);
