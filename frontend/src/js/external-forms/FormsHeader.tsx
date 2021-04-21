import styled from "@emotion/styled";
import { StateT } from "app-types";
import React from "react";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";
import { reset } from "redux-form";

import IconButton from "../button/IconButton";

import { selectActiveFormType } from "./stateSelectors";

const Root = styled("div")`
  display: flex;
  flex-direction: row;
  justify-content: flex-end;
  align-items: center;
  width: 100%;
`;

const FormsHeader: React.FC = () => {
  const { t } = useTranslation();
  const activeFormType = useSelector<StateT, string | null>((state) =>
    selectActiveFormType(state),
  );

  const dispatch = useDispatch();
  const onClear = (form: string | null) => {
    if (form) {
      dispatch(reset(form));
    }
  };

  return (
    <Root>
      <IconButton
        frame
        regular
        icon="trash-alt"
        onClick={() => onClear(activeFormType)}
        title={t("externalForms.common.clear")}
      >
        {t("externalForms.common.clear")}
      </IconButton>
    </Root>
  );
};

export default FormsHeader;
