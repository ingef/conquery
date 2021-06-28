import styled from "@emotion/styled";
import { StateT } from "app-types";
import React from "react";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";

import IconButton from "../button/IconButton";

import { clearTimebasedQuery } from "./actions";
import { anyConditionFilled } from "./helpers";

const Root = styled("div")`
  margin-bottom: 20px;
  padding: 0 20px 0 10px;
`;

const TimebasedQueryClearButton = () => {
  const { t } = useTranslation();
  const isEnabled = useSelector<StateT, boolean>(
    (state) =>
      state.timebasedQueryEditor.timebasedQuery.conditions.length > 1 ||
      anyConditionFilled(state.timebasedQueryEditor.timebasedQuery),
  );

  const dispatch = useDispatch();
  const clearQuery = () => dispatch(clearTimebasedQuery());

  return (
    <Root>
      <IconButton
        frame
        onClick={clearQuery}
        regular
        icon="trash-alt"
        disabled={!isEnabled}
      >
        {t("common.clear")}
      </IconButton>
    </Root>
  );
};

export default TimebasedQueryClearButton;
