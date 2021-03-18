import React from "react";
import styled from "@emotion/styled";
import { connect } from "react-redux";
import { useTranslation } from "react-i18next";

import IconButton from "../button/IconButton";

import {
  addTimebasedCondition,
  removeTimebasedCondition,
  setTimebasedConditionOperator,
  dropTimebasedNode,
  setTimebasedNodeTimestamp,
  removeTimebasedNode,
  setTimebasedIndexResult,
  setTimebasedConditionMinDays,
  setTimebasedConditionMaxDays,
  setTimebasedConditionMinDaysOrNoEvent,
} from "./actions";

import TimebasedCondition from "./TimebasedCondition";

const Root = styled("div")`
  flex-grow: 1;
  overflow-y: auto;
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

type PropsType = {
  query: Object;
  onDropTimebasedNode: () => void;
  onRemoveTimebasedNode: () => void;
  onAddTimebasedCondition: () => void;
  onRemoveTimebasedCondition: () => void;
  onSetTimebasedConditionOperator: () => void;
  onSetTimebasedNodeTimestamp: () => void;
  onSetTimebasedIndexResult: () => void;
  onSetTimebasedConditionMinDays: () => void;
  onSetTimebasedConditionMaxDays: () => void;
  onSetTimebasedConditionMinDaysOrNoEvent: () => void;
};

const TimebasedQueryEditor = (props: PropsType) => {
  const { t } = useTranslation();

  return (
    <Root>
      {props.query.conditions.map((condition, idx) => (
        <div key={`condition-${idx}`}>
          <TimebasedCondition
            condition={condition}
            conditionIdx={idx}
            indexResult={props.query.indexResult}
            removable={props.query.conditions.length > 1}
            onRemove={() => props.onRemoveTimebasedCondition(idx)}
            onRemoveTimebasedNode={(resultIdx, moved) => {
              props.onRemoveTimebasedNode(idx, resultIdx, moved);
            }}
            onSetOperator={(value) =>
              props.onSetTimebasedConditionOperator(idx, value)
            }
            onDropTimebasedNode={(resultIdx, node, moved) => {
              props.onDropTimebasedNode(idx, resultIdx, node, moved);
            }}
            onSetTimebasedNodeTimestamp={(resultIdx, timestamp) => {
              props.onSetTimebasedNodeTimestamp(idx, resultIdx, timestamp);
            }}
            onSetTimebasedIndexResult={props.onSetTimebasedIndexResult}
            onSetTimebasedConditionMinDays={(days) => {
              props.onSetTimebasedConditionMinDays(idx, days);
            }}
            onSetTimebasedConditionMaxDays={(days) => {
              props.onSetTimebasedConditionMaxDays(idx, days);
            }}
            onSetTimebasedConditionMinDaysOrNoEvent={(days) => {
              props.onSetTimebasedConditionMinDaysOrNoEvent(idx, days);
            }}
          />

          <Connector>{t("common.and")}</Connector>
        </div>
      ))}
      <AddBtn icon="plus" onClick={props.onAddTimebasedCondition}>
        {t("timebasedQueryEditor.addCondition")}
      </AddBtn>
    </Root>
  );
};

const mapStateToProps = (state) => ({
  query: state.timebasedQueryEditor.timebasedQuery,
});

const mapDispatchToProps = (dispatch) => ({
  onAddTimebasedCondition: () => dispatch(addTimebasedCondition()),
  onRemoveTimebasedCondition: (idx) => dispatch(removeTimebasedCondition(idx)),
  onSetTimebasedConditionOperator: (idx, value) =>
    dispatch(setTimebasedConditionOperator(idx, value)),
  onDropTimebasedNode: (conditionIdx, resultIdx, node, moved) =>
    dispatch(dropTimebasedNode(conditionIdx, resultIdx, node, moved)),
  onSetTimebasedNodeTimestamp: (conditionIdx, resultIdx, timestamp) =>
    dispatch(setTimebasedNodeTimestamp(conditionIdx, resultIdx, timestamp)),
  onRemoveTimebasedNode: (conditionIdx, resultIdx, moved) =>
    dispatch(removeTimebasedNode(conditionIdx, resultIdx, moved)),
  onSetTimebasedIndexResult: (indexResult) =>
    dispatch(setTimebasedIndexResult(indexResult)),
  onSetTimebasedConditionMinDays: (conditionIdx, days) =>
    dispatch(setTimebasedConditionMinDays(conditionIdx, days)),
  onSetTimebasedConditionMaxDays: (conditionIdx, days) =>
    dispatch(setTimebasedConditionMaxDays(conditionIdx, days)),
  onSetTimebasedConditionMinDaysOrNoEvent: (conditionIdx, days) =>
    dispatch(setTimebasedConditionMinDaysOrNoEvent(conditionIdx, days)),
});

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(TimebasedQueryEditor);
