import React from "react";
import styled from "@emotion/styled";
import T from "i18n-react";
import IconButton from "../button/IconButton";
import WithTooltip from "../tooltip/WithTooltip";
import FaIcon from "../icon/FaIcon";

type PropsType = {
  excludeTimestamps?: boolean;
  isExpandable?: boolean;
  hasDetails?: boolean;
  previousQueryLoading?: boolean;
  error?: string;
  onDeleteNode: Function;
  onEditClick: Function;
  onExpandClick: Function;
  onToggleTimestamps: Function;
};

const Actions = styled("div")`
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
`;

const StyledFaIcon = styled(FaIcon)`
  margin: 7px 6px 4px;
`;

const StyledIconButton = styled(IconButton)`
  padding: 0px 6px 4px;
`;

const QueryNodeActions = (props: PropsType) => {
  return (
    <Actions>
      <StyledIconButton
        icon="times"
        onClick={e => {
          e.stopPropagation();
          props.onDeleteNode();
        }}
      />
      {props.excludeTimestamps && (
        <WithTooltip text={T.translate("queryNodeEditor.excludingTimestamps")}>
          <StyledIconButton
            red
            regular
            icon="calendar"
            onClick={e => {
              e.stopPropagation();
              props.onToggleTimestamps();
            }}
          />
        </WithTooltip>
      )}
      {!props.error && !!props.previousQueryLoading && (
        <WithTooltip text={T.translate("queryEditor.loadingPreviousQuery")}>
          <StyledFaIcon noFrame icon="spinner" />
        </WithTooltip>
      )}
      {!props.error && props.isExpandable && !props.previousQueryLoading && (
        <WithTooltip text={T.translate("queryEditor.expand")}>
          <StyledIconButton
            noFrame
            icon="expand-arrows-alt"
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
