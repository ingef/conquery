import React from "react";
import styled from "@emotion/styled";
import { reset } from "redux-form";

import { T } from "../localization";
import IconButton from "../button/IconButton";
import { useDispatch, useSelector } from "react-redux";
import { selectActiveFormType } from "./stateSelectors";
import { StateT } from "app-types";

const Root = styled("div")`
  display: flex;
  flex-direction: row;
  justify-content: flex-end;
  align-items: center;
  width: 100%;
`;

const FormsHeader: React.FC = () => {
  const activeFormType = useSelector<StateT, string | null>((state) =>
    selectActiveFormType(state)
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
        title={T.translate("externalForms.common.clear")}
      >
        {T.translate("externalForms.common.clear")}
      </IconButton>
    </Root>
  );
};

export default FormsHeader;
