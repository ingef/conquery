// @flow

import React from "react";
import styled from "@emotion/styled";
import T from "i18n-react";
import IconButton from "../button/IconButton";
import FaIcon from "../icon/FaIcon";
import WithTooltip from "../tooltip/WithTooltip";

type PropsType = {
  excludeTimestamps?: boolean,
  hasActiveFilters?: boolean,
  isExpandable?: boolean,
  hasDetails?: boolean,
  previousQueryLoading?: boolean,
  error?: string,
  onDeleteNode: Function,
  onEditClick: Function,
  onExpandClick: Function
};

const Actions = styled("div")`
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
`;

const StyledFaIcon = styled(FaIcon)`
  color: ${({ theme }) => theme.col.red};
  padding: 4px 6px;
`;

const StyledIconButton = styled(IconButton)`
  padding: 4px 6px;
  font-size: 5px;
`;

const QueryNodeActions = (props: PropsType) => {
  return (
    <Actions>
      <WithTooltip>
        <IconButton
          noFrame
          tiny
          icon="close"
          onClick={e => {
            e.stopPropagation();
            props.onDeleteNode();
          }}
        />
      </WithTooltip>
      {props.excludeTimestamps && (
        <WithTooltip>
          <StyledFaIcon
            data-tip={T.translate("queryNodeEditor.excludingTimestamps")}
            icon="calendar-o"
          />
        </WithTooltip>
      )}
      {!props.error && !!props.previousQueryLoading && (
        <WithTooltip>
          <StyledIconButton
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
