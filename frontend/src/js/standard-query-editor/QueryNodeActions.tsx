import styled from "@emotion/styled";
import { memo } from "react";
import { useTranslation } from "react-i18next";

import IconButton from "../button/IconButton";
import FaIcon from "../icon/FaIcon";
import WithTooltip from "../tooltip/WithTooltip";

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

interface Props {
  andIdx: number;
  orIdx: number;
  excludeTimestamps?: boolean;
  excludeFromSecondaryId?: boolean;
  isExpandable?: boolean;
  hasDetails?: boolean;
  previousQueryLoading?: boolean;
  error?: string;
  hasActiveSecondaryId?: boolean;
  onDeleteNode: (andIdx: number, orIdx: number) => void;
  onExpandClick: () => void;
  onToggleTimestamps: (andIdx: number, orIdx: number) => void;
  onToggleSecondaryIdExclude: (andIdx: number, orIdx: number) => void;
}

const QueryNodeActions = (props: Props) => {
  const { t } = useTranslation();

  return (
    <Actions>
      <WithTooltip text={t("queryEditor.removeNode")}>
        <StyledIconButton
          icon="times"
          onClick={(e) => {
            e.stopPropagation();
            props.onDeleteNode(props.andIdx, props.orIdx);
          }}
        />
      </WithTooltip>
      {props.excludeTimestamps && (
        <WithTooltip text={t("queryNodeEditor.excludingTimestamps")}>
          <StyledIconButton
            red
            regular
            icon="calendar"
            onClick={(e) => {
              e.stopPropagation();
              props.onToggleTimestamps(props.andIdx, props.orIdx);
            }}
          />
        </WithTooltip>
      )}
      {!props.error && !!props.previousQueryLoading && (
        <WithTooltip text={t("queryEditor.loadingPreviousQuery")}>
          <StyledFaIcon icon="spinner" />
        </WithTooltip>
      )}
      {!props.error && props.isExpandable && !props.previousQueryLoading && (
        <WithTooltip text={t("queryEditor.expand")}>
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
            props.excludeFromSecondaryId
              ? t("queryNodeEditor.excludingFromSecondaryId")
              : t("queryEditor.hasSecondaryId")
          }
        >
          <RelativeContainer>
            <StyledIconButton
              icon="microscope"
              onClick={(e) => {
                e.stopPropagation();
                props.onToggleSecondaryIdExclude(props.andIdx, props.orIdx);
              }}
            />
            {props.excludeFromSecondaryId && <CrossedOut />}
          </RelativeContainer>
        </WithTooltip>
      )}
    </Actions>
  );
};

export default memo(QueryNodeActions);
