import {
  DROP_TIMEBASED_NODE,
  REMOVE_TIMEBASED_NODE,
  SET_TIMEBASED_NODE_TIMESTAMP,
  SET_TIMEBASED_CONDITION_OPERATOR,
  SET_TIMEBASED_CONDITION_MIN_DAYS,
  SET_TIMEBASED_CONDITION_MAX_DAYS,
  SET_TIMEBASED_INDEX_RESULT,
  ADD_TIMEBASED_CONDITION,
  REMOVE_TIMEBASED_CONDITION,
  CLEAR_TIMEBASED_QUERY,
  SET_TIMEBASED_CONDITION_MIN_DAYS_OR_NO_EVENT
} from "./actionTypes";

export const dropTimebasedNode = (conditionIdx, resultIdx, node, moved) => ({
  type: DROP_TIMEBASED_NODE,
  payload: { conditionIdx, resultIdx, node, moved }
});

export const removeTimebasedNode = (conditionIdx, resultIdx, moved) => ({
  type: REMOVE_TIMEBASED_NODE,
  payload: { conditionIdx, resultIdx, moved }
});

export const setTimebasedNodeTimestamp = (
  conditionIdx,
  resultIdx,
  timestamp
) => ({
  type: SET_TIMEBASED_NODE_TIMESTAMP,
  payload: { conditionIdx, resultIdx, timestamp }
});

export const setTimebasedConditionOperator = (conditionIdx, operator) => ({
  type: SET_TIMEBASED_CONDITION_OPERATOR,
  payload: { conditionIdx, operator }
});

export const setTimebasedConditionMaxDays = (conditionIdx, days) => ({
  type: SET_TIMEBASED_CONDITION_MAX_DAYS,
  payload: { conditionIdx, days }
});

export const setTimebasedConditionMinDays = (conditionIdx, days) => ({
  type: SET_TIMEBASED_CONDITION_MIN_DAYS,
  payload: { conditionIdx, days }
});

export const setTimebasedConditionMinDaysOrNoEvent = (conditionIdx, days) => ({
  type: SET_TIMEBASED_CONDITION_MIN_DAYS_OR_NO_EVENT,
  payload: { conditionIdx, days }
});

export const setTimebasedIndexResult = indexResult => ({
  type: SET_TIMEBASED_INDEX_RESULT,
  payload: { indexResult }
});

export const addTimebasedCondition = () => ({ type: ADD_TIMEBASED_CONDITION });

export const removeTimebasedCondition = conditionIdx => ({
  type: REMOVE_TIMEBASED_CONDITION,
  payload: { conditionIdx }
});

export const clearTimebasedQuery = () => ({ type: CLEAR_TIMEBASED_QUERY });
