import React from "react";
import styled from "@emotion/styled";
import { reset } from "redux-form";

import { T } from "../localization";
import IconButton from "../button/IconButton";
import { useDispatch, useSelector } from "react-redux";
import { selectActiveForm } from "./stateSelectors";
import { StateT } from "app-types";

const Root = styled("div")`
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  width: 100%;
`;

const FormsHeader: React.FC = () => {
  const activeForm = useSelector<StateT, string | null>(state =>
    selectActiveForm(state)
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
        onClick={() => onClear(activeForm)}
        title={T.translate("externalForms.common.clear")}
      >
        {T.translate("externalForms.common.clear")}
      </IconButton>
    </Root>
  );
};

export default FormsHeader;
