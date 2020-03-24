import React from "react";
import styled from "@emotion/styled";
import type { Dispatch } from "redux-thunk";
import { connect } from "react-redux";

import { setExternalForm } from "./actions";

import InputSelect from "../form-components/InputSelect";
import { T, getLocale } from "../localization";
import { selectActiveForm, selectAvailableForms } from "./stateSelectors";
import type { Forms as FormsType } from "./config-types";

type PropsType = {
  availableForms: FormsType;
  activeForm: string;
  onItemClick: Function;
  onClearForm: Function;
};

const Root = styled("div")`
  flex-shrink: 0;
  margin-bottom: 10px;
  padding: 0 20px 10px 10px;
  box-shadow: 0 0 5px 0 rgba(0, 0, 0, 0.2);
  box-sizing: border-box;
  display: flex;
  flex-direction: row;
  align-items: flex-start;
`;

const SxInputSelect = styled(InputSelect)`
  flex-grow: 1;
`;

const FormsNavigation = (props: PropsType) => {
  const locale = getLocale();
  const options = Object.values(props.availableForms).map(formType => ({
    label: formType.headline[locale],
    value: formType.type
  }));

  return (
    <Root>
      <SxInputSelect
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
  availableForms: selectAvailableForms(state),
  activeForm: selectActiveForm(state)
});

const mapDispatchToProps = (dispatch: Dispatch) => ({
  onItemClick: form => dispatch(setExternalForm(form))
});

export default connect(mapStateToProps, mapDispatchToProps)(FormsNavigation);
