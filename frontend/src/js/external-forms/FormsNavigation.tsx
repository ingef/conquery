import styled from "@emotion/styled";
import type { StateT } from "app-types";
import React, { FC } from "react";
import { useTranslation } from "react-i18next";
import { useSelector, useDispatch } from "react-redux";
import { reset } from "redux-form";

import IconButton from "../button/IconButton";
import { useActiveLang } from "../localization/useActiveLang";
import WithTooltip from "../tooltip/WithTooltip";
import InputSelect from "../ui-components/InputSelectOld";

import { setExternalForm } from "./actions";
import { Form } from "./config-types";
import { selectActiveFormType, selectAvailableForms } from "./stateSelectors";

const Root = styled("div")`
  flex-shrink: 0;
  margin-bottom: 10px;
  padding: 0 20px 10px 10px;
  box-shadow: 0 0 5px 0 rgba(0, 0, 0, 0.2);
  box-sizing: border-box;
  display: flex;
  flex-direction: row;
  align-items: flex-end;
`;

const SxInputSelect = styled(InputSelect)`
  flex-grow: 1;
`;

const SxIconButton = styled(IconButton)`
  flex-shrink: 0;
  margin-left: 10px;
  padding: 6px 10px;
`;

const FormsNavigation: FC = () => {
  const language = useActiveLang();
  const { t } = useTranslation();

  const availableForms = useSelector<
    StateT,
    {
      [formName: string]: Form;
    }
  >((state) => selectAvailableForms(state));

  const activeForm = useSelector<StateT, string | null>((state) =>
    selectActiveFormType(state),
  );

  const dispatch = useDispatch();

  const onItemClick = (form: string) => dispatch(setExternalForm(form));

  const options = Object.values(availableForms)
    .map((formType) => ({
      label: formType.title[language]!,
      value: formType.type,
    }))
    .sort((a, b) => (a.label < b.label ? -1 : 1));

  const activeFormType = useSelector<StateT, string | null>((state) =>
    selectActiveFormType(state),
  );
  const onClear = () => {
    if (activeFormType) {
      dispatch(reset(activeFormType));
    }
  };

  return (
    <Root>
      <SxInputSelect
        label={t("externalForms.forms")}
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
      <WithTooltip text={t("externalForms.common.clear")}>
        <SxIconButton frame regular icon="trash-alt" onClick={onClear} />
      </WithTooltip>
    </Root>
  );
};

export default FormsNavigation;
