import styled from "@emotion/styled";
import { StateT } from "app-types";
import React from "react";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";

import IconButton from "../button/IconButton";

import TimebasedCondition from "./TimebasedCondition";
import {
  addTimebasedCondition,
  removeTimebasedCondition,
  setTimebasedConditionOperator,
  dropTimebasedNode,
  setTimebasedNodeTimestamp,
  removeTimebasedNode,
  setTimebasedConditionMinDays,
  setTimebasedConditionMaxDays,
  setTimebasedConditionMinDaysOrNoEvent,
} from "./actions";
import type {
  TimebasedOperatorType,
  TimebasedQueryStateT,
  TimebasedResultType,
} from "./reducer";

const Root = styled("div")`
  flex-grow: 1;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  padding: 0 20px 0 10px;
`;
const Connector = styled("p")`
  font-size: ${({ theme }) => theme.font.sm};
  color: ${({ theme }) => theme.col.gray};
  text-align: center;
  margin: 5px auto;
`;
const AddBtn = styled(IconButton)`
  margin: 0 auto;
  display: block;
`;

const TimebasedQueryEditor = () => {
  const { t } = useTranslation();

  const query = useSelector<StateT, TimebasedQueryStateT>(
    (state) => state.timebasedQueryEditor.timebasedQuery,
  );

  const dispatch = useDispatch();

  const onAddTimebasedCondition = () => dispatch(addTimebasedCondition());
  const onRemoveTimebasedCondition = (conditionIdx: number) =>
    dispatch(removeTimebasedCondition({ conditionIdx }));
  const onSetTimebasedConditionOperator = (
    conditionIdx: number,
    operator: TimebasedOperatorType,
  ) => dispatch(setTimebasedConditionOperator({ conditionIdx, operator }));
  const onDropTimebasedNode = (
    conditionIdx: number,
    resultIdx: number,
    node: TimebasedResultType,
    moved: boolean,
  ) => dispatch(dropTimebasedNode({ conditionIdx, resultIdx, node, moved }));
  const onSetTimebasedNodeTimestamp = (
    conditionIdx: number,
    resultIdx: number,
    timestamp: string,
  ) =>
    dispatch(setTimebasedNodeTimestamp({ conditionIdx, resultIdx, timestamp }));
  const onRemoveTimebasedNode = (
    conditionIdx: number,
    resultIdx: number,
    moved: boolean,
  ) => dispatch(removeTimebasedNode({ conditionIdx, resultIdx, moved }));
  // const onSetTimebasedIndexResult = (indexResult) =>
  //   dispatch(setTimebasedIndexResult(indexResult));
  const onSetTimebasedConditionMinDays = (
    conditionIdx: number,
    days: number | null,
  ) => dispatch(setTimebasedConditionMinDays({ conditionIdx, days }));
  const onSetTimebasedConditionMaxDays = (
    conditionIdx: number,
    days: number | null,
  ) => dispatch(setTimebasedConditionMaxDays({ conditionIdx, days }));
  const onSetTimebasedConditionMinDaysOrNoEvent = (
    conditionIdx: number,
    days: number | null,
  ) => dispatch(setTimebasedConditionMinDaysOrNoEvent({ conditionIdx, days }));

  return (
    <Root>
      {query.conditions.map((condition, idx) => (
        <div key={`condition-${idx}`}>
          <TimebasedCondition
            condition={condition}
            conditionIdx={idx}
            // indexResult={query.indexResult}
            removable={query.conditions.length > 1}
            onRemove={() => onRemoveTimebasedCondition(idx)}
            onRemoveTimebasedNode={(resultIdx, moved) => {
              onRemoveTimebasedNode(idx, resultIdx, moved);
            }}
            onSetOperator={(value) =>
              onSetTimebasedConditionOperator(idx, value)
            }
            onDropTimebasedNode={(resultIdx, node, moved) => {
              onDropTimebasedNode(idx, resultIdx, node, moved);
            }}
            onSetTimebasedNodeTimestamp={(resultIdx, timestamp) => {
              onSetTimebasedNodeTimestamp(idx, resultIdx, timestamp);
            }}
            // onSetTimebasedIndexResult={onSetTimebasedIndexResult}
            onSetTimebasedConditionMinDays={(days) => {
              onSetTimebasedConditionMinDays(idx, days);
            }}
            onSetTimebasedConditionMaxDays={(days) => {
              onSetTimebasedConditionMaxDays(idx, days);
            }}
            onSetTimebasedConditionMinDaysOrNoEvent={(days) => {
              onSetTimebasedConditionMinDaysOrNoEvent(idx, days);
            }}
          />

          <Connector>{t("common.and")}</Connector>
        </div>
      ))}
      <AddBtn icon="plus" onClick={onAddTimebasedCondition}>
        {t("timebasedQueryEditor.addCondition")}
      </AddBtn>
    </Root>
  );
};

export default TimebasedQueryEditor;
