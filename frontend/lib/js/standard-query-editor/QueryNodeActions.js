// @flow

import React from "react";
import styled from "@emotion/styled";
import T from "i18n-react";
import IconButton from "../button/IconButton";
import WithTooltip from "../tooltip/WithTooltip";
import FaIcon from "../icon/FaIcon";

type PropsType = {
  excludeTimestamps?: boolean,
  hasActiveFilters?: boolean,
  isExpandable?: boolean,
  hasDetails?: boolean,
  previousQueryLoading?: boolean,
  error?: string,
  onDeleteNode: Function,
  onEditClick: Function,
  onExpandClick: Function,
  onToggleTimestamps: Function
};

const Actions = styled("div")`
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
`;

const StyledFaIcon = styled(FaIcon)`
  padding: 4px 6px;
`;
const StyledIconButton = styled(IconButton)`
  padding: 4px 6px;
`;

const QueryNodeActions = (props: PropsType) => {
  return (
    <Actions>
      <WithTooltip>
        <StyledIconButton
          icon="close"
          onClick={e => {
            e.stopPropagation();
            props.onDeleteNode();
          }}
        />
      </WithTooltip>
      {props.excludeTimestamps && (
        <WithTooltip>
          <StyledIconButton
            red
            data-tip={T.translate("queryNodeEditor.excludingTimestamps")}
            icon="calendar-o"
            onClick={e => {
              e.stopPropagation();
              props.onToggleTimestamps();
            }}
          />
        </WithTooltip>
      )}
      {!props.error && !!props.previousQueryLoading && (
        <WithTooltip>
          <StyledFaIcon
            noFrame
            icon="spinner"
            data-tip={T.translate("queryEditor.loadingPreviousQuery")}
          />
        </WithTooltip>
      )}
      {!props.error && props.isExpandable && !props.previousQueryLoading && (
        <WithTooltip>
          <StyledIconButton
            data-tip={T.translate("queryEditor.expand")}
            noFrame
            icon="expand"
            onClick={e => {
              e.stopPropagation();
              props.onExpandClick();
            }}
          />
        </WithTooltip>
      )}
    </Actions>
  );
};

export default QueryNodeActions;
