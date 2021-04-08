import React from "react";
import { useTranslation } from "react-i18next";
import styled from "@emotion/styled";

import {
  BEFORE,
  BEFORE_OR_SAME,
  DAYS_BEFORE,
  SAME,
  DAYS_OR_NO_EVENT_BEFORE,
} from "../common/constants/timebasedQueryOperatorTypes";
import { isEmpty } from "../common/helpers";

import IconButton from "../button/IconButton";

import VerticalToggleButton from "../form-components/VerticalToggleButton";

import TimebasedQueryEditorDropzone from "./TimebasedQueryEditorDropzone";
import TimebasedConditionDayRange from "./TimebasedConditionDayRange";
import TimebasedNode from "./TimebasedNode";

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
  condition: Object;
  conditionIdx: number;
  indexResult: number | string | null;
  removable: boolean;
  onRemove: Function;
  onSetOperator: Function;
  onDropTimebasedNode: Function;
  onSetTimebasedNodeTimestamp: Function;
  onRemoveTimebasedNode: Function;
  onSetTimebasedIndexResult: Function;
  onSetTimebasedConditionMinDays: Function;
  onSetTimebasedConditionMaxDays: Function;
  onSetTimebasedConditionMinDaysOrNoEvent: Function;
  onSetTimebasedConditionMaxDaysOrNoEvent: Function;
};

const TimebasedCondition = (props: PropsType) => {
  const { t } = useTranslation();

  const minDays = !isEmpty(props.condition.minDays)
    ? props.condition.minDays
    : "";
  const maxDays = !isEmpty(props.condition.maxDays)
    ? props.condition.maxDays
    : "";
  const minDaysOrNoEvent = !isEmpty(props.condition.minDaysOrNoEvent)
    ? props.condition.minDaysOrNoEvent
    : "";

  const createTimebasedResult = (idx) => {
    return props.condition[`result${idx}`] ? (
      <TimebasedNode
        node={props.condition[`result${idx}`]}
        conditionIdx={props.conditionIdx}
        resultIdx={idx}
        isIndexResult={props.condition[`result${idx}`].id === props.indexResult}
        position={idx === 0 ? "left" : "right"}
        onRemove={() => props.onRemoveTimebasedNode(idx, false)}
        onSetTimebasedNodeTimestamp={(timestamp) => {
          props.onSetTimebasedNodeTimestamp(idx, timestamp);
        }}
        onSetTimebasedIndexResult={() => {
          props.onSetTimebasedIndexResult(props.condition[`result${idx}`].id);
        }}
        isIndexResultDisabled={
          idx === 0 && props.condition.operator === DAYS_OR_NO_EVENT_BEFORE
        }
      />
    ) : (
      <TimebasedQueryEditorDropzone
        onDropNode={(node, moved) =>
          props.onDropTimebasedNode(idx, node, moved)
        }
      />
    );
  };

  const result0 = createTimebasedResult(0);
  const result1 = createTimebasedResult(1);

  return (
    <Root>
      {props.removable && (
        <StyledIconButton icon="times" onClick={props.onRemove} />
      )}
      <NodesContainer>
        <HorizontalLine />
        <Nodes>
          {result0}
          <Operator>
            <StyledVerticalToggleButton
              onToggle={props.onSetOperator}
              activeValue={props.condition.operator}
              options={[
                {
                  label: t("timebasedQueryEditor.opBefore"),
                  value: BEFORE,
                },
                {
                  label: t("timebasedQueryEditor.opBeforeOrSame"),
                  value: BEFORE_OR_SAME,
                },
                {
                  label: t("timebasedQueryEditor.opDays"),
                  value: DAYS_BEFORE,
                },
                {
                  label: t("timebasedQueryEditor.opSame"),
                  value: SAME,
                },
                {
                  label: t("timebasedQueryEditor.opDaysOrNoEventBefore"),
                  value: DAYS_OR_NO_EVENT_BEFORE,
                },
              ]}
            />
          </Operator>
          {result1}
        </Nodes>
      </NodesContainer>
      {props.condition.operator === DAYS_BEFORE && (
        <TimebasedConditionDayRange
          minDays={minDays}
          maxDays={maxDays}
          onSetTimebasedConditionMinDays={props.onSetTimebasedConditionMinDays}
          onSetTimebasedConditionMaxDays={props.onSetTimebasedConditionMaxDays}
        />
      )}
      {props.condition.operator === DAYS_OR_NO_EVENT_BEFORE && (
        <TimebasedConditionDayRange
          minDays={minDaysOrNoEvent}
          onSetTimebasedConditionMinDays={
            props.onSetTimebasedConditionMinDaysOrNoEvent
          }
        />
      )}
    </Root>
  );
};

export default TimebasedCondition;
