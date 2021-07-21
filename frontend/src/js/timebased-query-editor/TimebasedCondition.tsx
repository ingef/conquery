import styled from "@emotion/styled";
import React from "react";
import { useTranslation } from "react-i18next";

import IconButton from "../button/IconButton";
import { isEmpty } from "../common/helpers";
import VerticalToggleButton from "../form-components/VerticalToggleButton";

import TimebasedConditionDayRange from "./TimebasedConditionDayRange";
import TimebasedNode from "./TimebasedNode";
import TimebasedQueryEditorDropzone from "./TimebasedQueryEditorDropzone";
import type {
  TimebasedConditionT,
  TimebasedOperatorType,
  TimebasedResultType,
} from "./reducer";

const StyledIconButton = styled(IconButton)`
  position: absolute;
  top: 0;
  right: 0;
  z-index: 1;
  display: inline;
`;

const Root = styled("div")`
  position: relative;
  padding: 30px 10px 10px;
  box-shadow: 0 0 10px 0 rgba(0, 0, 0, 0.12);
  border-radius: ${({ theme }) => theme.borderRadius};
  border: 1px solid ${({ theme }) => theme.col.grayLight};
  background-color: ${({ theme }) => theme.col.bg};

  &:hover {
    border: 1px solid ${({ theme }) => theme.col.grayLight};
  }
`;

const StyledVerticalToggleButton = styled(VerticalToggleButton)`
  max-width: 180px;
`;

const NodesContainer = styled("div")`
  margin-bottom: 10px;
  position: relative;
`;

const Nodes = styled("div")`
  position: relative;
  width: 100%;
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
`;

const HorizontalLine = styled("div")`
  position: absolute;
  top: 50%;
  right: 10%;
  width: 80%;
  border-bottom: 1px solid ${({ theme }) => theme.col.blueGray};
  margin-top: -0.5px;
`;

const Operator = styled("div")`
  margin: 0 10px;
`;

type PropsType = {
  condition: TimebasedConditionT;
  conditionIdx: number;
  // indexResult: number | string | null;
  removable: boolean;
  onRemove: () => void;
  onSetOperator: (value: TimebasedOperatorType) => void;
  onRemoveTimebasedNode: (idx: number, moved: boolean) => void;
  onDropTimebasedNode: (
    resultIdx: number,
    node: TimebasedResultType,
    moved: boolean,
  ) => void;
  onSetTimebasedNodeTimestamp: (idx: number, timestamp: string) => void;
  onSetTimebasedConditionMinDays: (value: number | null) => void;
  onSetTimebasedConditionMaxDays: (value: number | null) => void;
  onSetTimebasedConditionMinDaysOrNoEvent: (value: number | null) => void;
};

const TimebasedCondition = ({
  condition,
  conditionIdx,
  // indexResult,
  removable,
  onRemove,
  onSetOperator,
  onRemoveTimebasedNode,
  onDropTimebasedNode,
  onSetTimebasedNodeTimestamp,
  onSetTimebasedConditionMinDays,
  onSetTimebasedConditionMaxDays,
  onSetTimebasedConditionMinDaysOrNoEvent,
}: PropsType) => {
  const { t } = useTranslation();

  const minDays = !isEmpty(condition.minDays) ? condition.minDays : "";
  const maxDays = !isEmpty(condition.maxDays) ? condition.maxDays : "";
  const minDaysOrNoEvent = !isEmpty(condition.minDaysOrNoEvent)
    ? condition.minDaysOrNoEvent
    : "";

  const createTimebasedResult = (idx: 0 | 1) => {
    const node = idx === 0 ? condition.result0 : condition.result1;
    return node ? (
      <TimebasedNode
        node={node}
        conditionIdx={conditionIdx}
        resultIdx={idx}
        // isIndexResult={condition[`result${idx}`].id === indexResult}
        position={idx === 0 ? "left" : "right"}
        onRemove={() => onRemoveTimebasedNode(idx, false)}
        onSetTimebasedNodeTimestamp={(timestamp) => {
          onSetTimebasedNodeTimestamp(idx, timestamp);
        }}
        // onSetTimebasedIndexResult={() => {
        //   onSetTimebasedIndexResult(condition[`result${idx}`].id);
        // }}
        // isIndexResultDisabled={
        //   idx === 0 && condition.operator === "DAYS_OR_NO_EVENT_BEFORE"
        // }
      />
    ) : (
      <TimebasedQueryEditorDropzone
        onDropNode={(node: TimebasedResultType, moved: boolean) =>
          onDropTimebasedNode(idx, node, moved)
        }
      />
    );
  };

  const result0 = createTimebasedResult(0);
  const result1 = createTimebasedResult(1);

  return (
    <Root>
      {removable && <StyledIconButton icon="times" onClick={onRemove} />}
      <NodesContainer>
        <HorizontalLine />
        <Nodes>
          {result0}
          <Operator>
            <StyledVerticalToggleButton
              onToggle={(value) =>
                onSetOperator(value as TimebasedOperatorType)
              }
              activeValue={condition.operator}
              options={[
                {
                  label: t("timebasedQueryEditor.opBefore"),
                  value: "BEFORE",
                },
                {
                  label: t("timebasedQueryEditor.opBeforeOrSame"),
                  value: "BEFORE_OR_SAME",
                },
                {
                  label: t("timebasedQueryEditor.opDays"),
                  value: "DAYS_BEFORE",
                },
                {
                  label: t("timebasedQueryEditor.opSame"),
                  value: "SAME",
                },
                {
                  label: t("timebasedQueryEditor.opDaysOrNoEventBefore"),
                  value: "DAYS_OR_NO_EVENT_BEFORE",
                },
              ]}
            />
          </Operator>
          {result1}
        </Nodes>
      </NodesContainer>
      {condition.operator === "DAYS_BEFORE" && (
        <TimebasedConditionDayRange
          minDays={minDays}
          maxDays={maxDays}
          onSetTimebasedConditionMinDays={onSetTimebasedConditionMinDays}
          onSetTimebasedConditionMaxDays={onSetTimebasedConditionMaxDays}
        />
      )}
      {condition.operator === "DAYS_OR_NO_EVENT_BEFORE" && (
        <TimebasedConditionDayRange
          minDays={minDaysOrNoEvent}
          onSetTimebasedConditionMinDays={
            onSetTimebasedConditionMinDaysOrNoEvent
          }
        />
      )}
    </Root>
  );
};

export default TimebasedCondition;
