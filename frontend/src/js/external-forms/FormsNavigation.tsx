import React, { FC } from "react";
import styled from "@emotion/styled";
import { useSelector, useDispatch } from "react-redux";

import { setExternalForm } from "./actions";

import InputSelect from "../form-components/InputSelect";
import { T, getLocale } from "../localization";
import { selectActiveFormType, selectAvailableForms } from "./stateSelectors";
import type { StateT } from "app-types";
import type { Form } from "../api/form-types";

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

const FormsNavigation: FC = () => {
  const availableForms = useSelector<
    StateT,
    {
      [formName: string]: Form;
    }
  >((state) => selectAvailableForms(state));
  const activeForm = useSelector<StateT, string | null>((state) =>
    selectActiveFormType(state)
  );

  const dispatch = useDispatch();

  const onItemClick = (form: string) => dispatch(setExternalForm(form));

  const locale = getLocale();
  const options = Object.values(availableForms)
    .map((formType) => ({
      label: formType.headline[locale]!,
      value: formType.type,
    }))
    .sort((a, b) => (a.label < b.label ? -1 : 1));

  return (
    <Root>
      <SxInputSelect
        label={T.translate("externalForms.forms")}
        options={options}
        input={{
          value: activeForm,
          onChange: (value: string) => onItemClick(value),
        }}
        selectProps={{
          clearable: false,
          autosize: true,
          searchable: false,
        }}
      />
    </Root>
  );
};

export default FormsNavigation;
