import styled from "@emotion/styled";
import React from "react";
import { useTranslation } from "react-i18next";
import { connect } from "react-redux";

import IconButton from "../button/IconButton";

import { clearTimebasedQuery } from "./actions";
import { anyConditionFilled } from "./helpers";

const Root = styled("div")`
  margin-bottom: 20px;
  padding: 0 20px 0 10px;
`;

type PropsType = {
  clearQuery: () => void;
  isEnabled: boolean;
};

const TimebasedQueryClearButton = (props: PropsType) => {
  const { t } = useTranslation();
  return (
    <Root>
      <IconButton
        frame
        onClick={props.clearQuery}
        regular
        icon="trash-alt"
        disabled={!props.isEnabled}
      >
        {t("common.clear")}
      </IconButton>
    </Root>
  );
};

const mapStateToProps = (state) => ({
  isEnabled:
    state.timebasedQueryEditor.timebasedQuery.conditions.length > 1 ||
    anyConditionFilled(state.timebasedQueryEditor.timebasedQuery),
});

const mapDispatchToProps = (dispatch) => ({
  clearQuery: () => dispatch(clearTimebasedQuery()),
});

export default connect(
  mapStateToProps,
  mapDispatchToProps,
)(TimebasedQueryClearButton);
