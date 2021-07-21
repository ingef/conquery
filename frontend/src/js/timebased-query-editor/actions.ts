import { ActionType, createAction } from "typesafe-actions";

import { TimebasedOperatorType, TimebasedResultType } from "./reducer";

export type TimebasedActions = ActionType<
  | typeof dropTimebasedNode
  | typeof removeTimebasedNode
  | typeof setTimebasedNodeTimestamp
  | typeof setTimebasedConditionOperator
  | typeof setTimebasedConditionMaxDays
  | typeof setTimebasedConditionMinDays
  | typeof setTimebasedConditionMinDaysOrNoEvent
  | typeof setTimebasedIndexResult
  | typeof addTimebasedCondition
  | typeof removeTimebasedCondition
  | typeof clearTimebasedQuery
>;

export const dropTimebasedNode = createAction(
  "timebased-query-editor/DROP_TIMEBASED_NODE",
)<{
  conditionIdx: number;
  resultIdx: number;
  node: TimebasedResultType;
  moved: boolean;
}>();

export const removeTimebasedNode = createAction(
  "timebased-query-editor/REMOVE_TIMEBASED_NODE",
)<{ conditionIdx: number; resultIdx: number; moved: boolean }>();

export const setTimebasedNodeTimestamp = createAction(
  "timebased-query-editor/SET_TIMEBASED_NODE_TIMESTAMP",
)<{
  conditionIdx: number;
  resultIdx: number;
  timestamp: string;
}>();

export const setTimebasedConditionOperator = createAction(
  "timebased-query-editor/SET_TIMEBASED_CONDITION_OPERATOR",
)<{ conditionIdx: number; operator: TimebasedOperatorType }>();

export const setTimebasedConditionMaxDays = createAction(
  "timebased-query-editor/SET_TIMEBASED_CONDITION_MAX_DAYS",
)<{ conditionIdx: number; days: number | null }>();

export const setTimebasedConditionMinDays = createAction(
  "timebased-query-editor/SET_TIMEBASED_CONDITION_MIN_DAYS",
)<{ conditionIdx: number; days: number | null }>();

export const setTimebasedConditionMinDaysOrNoEvent = createAction(
  "timebased-query-editor/SET_TIME_BASED_CONDITION_MIN_DAYS_OR_NO_EVENT",
)<{ conditionIdx: number; days: number | null }>();

export const setTimebasedIndexResult = createAction(
  "timebased-query-editor/SET_TIMEBASED_INDEX_RESULT",
)<{ indexResult: string }>();

export const addTimebasedCondition = createAction(
  "timebased-query-editor/ADD_TIMEBASED_CONDITION",
)();

export const removeTimebasedCondition = createAction(
  "timebased-query-editor/REMOVE_TIMEBASED_CONDITION",
)<{ conditionIdx: number }>();

export const clearTimebasedQuery = createAction(
  "timebased-query-editor/CLEAR_TIMEBASED_QUERY",
)();
