import React, { FC } from "react";
import styled from "@emotion/styled";
import T from "i18n-react";
import IconButton from "../button/IconButton";
import WithTooltip from "../tooltip/WithTooltip";
import FaIcon from "../icon/FaIcon";

interface PropsT {
  excludeTimestamps?: boolean;
  excludeFromSecondaryIdQuery?: boolean;
  isExpandable?: boolean;
  hasDetails?: boolean;
  previousQueryLoading?: boolean;
  error?: string;
  hasActiveSecondaryId?: boolean;
  onDeleteNode: () => void;
  onExpandClick: () => void;
  onToggleTimestamps: () => void;
  onToggleSecondaryIdExclude: () => void;
}

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

const RelativeContainer = styled.div`
  position: relative;
`;
const CrossedOut = styled.div`
  position: absolute;
  top: 40%;
  left: 10%;
  width: 22px;
  height: 3px;
  transform: rotate(135deg);
  background-color: ${({ theme }) => theme.col.red};
  opacity: 0.5;
  pointer-events: none;
`;

const QueryNodeActions: FC<PropsT> = (props) => {
  return (
    <Actions>
      <StyledIconButton
        icon="times"
        onClick={(e) => {
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
            onClick={(e) => {
              e.stopPropagation();
              props.onToggleTimestamps();
            }}
          />
        </WithTooltip>
      )}
      {!props.error && !!props.previousQueryLoading && (
        <WithTooltip text={T.translate("queryEditor.loadingPreviousQuery")}>
          <StyledFaIcon icon="spinner" />
        </WithTooltip>
      )}
      {!props.error && props.isExpandable && !props.previousQueryLoading && (
        <WithTooltip text={T.translate("queryEditor.expand")}>
          <StyledIconButton
            icon="expand-arrows-alt"
            onClick={(e) => {
              e.stopPropagation();
              props.onExpandClick();
            }}
          />
        </WithTooltip>
      )}
      {props.hasActiveSecondaryId && (
        <WithTooltip
          text={
            props.excludeFromSecondaryIdQuery
              ? T.translate("queryNodeEditor.excludingFromSecondaryIdQuery")
              : T.translate("queryEditor.hasSecondaryId")
          }
        >
          <RelativeContainer>
            <StyledIconButton
              icon="microscope"
              onClick={(e) => {
                e.stopPropagation();
                props.onToggleSecondaryIdExclude();
              }}
            />
            {props.excludeFromSecondaryIdQuery && <CrossedOut />}
          </RelativeContainer>
        </WithTooltip>
      )}
    </Actions>
  );
};

export default QueryNodeActions;
