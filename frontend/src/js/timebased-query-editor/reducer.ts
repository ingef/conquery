import {
  BEFORE,
  DAYS_OR_NO_EVENT_BEFORE,
  TIMEBASED_OPERATOR_TYPES,
} from "../common/constants/timebasedQueryOperatorTypes";
import {
  EARLIEST,
  TIMEBASED_TIMESTAMP_TYPES,
} from "../common/constants/timebasedQueryTimestampTypes";
import { RENAME_PREVIOUS_QUERY_SUCCESS } from "../previous-queries/list/actionTypes";

import {
  DROP_TIMEBASED_NODE,
  REMOVE_TIMEBASED_NODE,
  SET_TIMEBASED_NODE_TIMESTAMP,
  SET_TIMEBASED_CONDITION_OPERATOR,
  SET_TIMEBASED_CONDITION_MIN_DAYS,
  SET_TIMEBASED_CONDITION_MAX_DAYS,
  SET_TIMEBASED_CONDITION_MIN_DAYS_OR_NO_EVENT,
  SET_TIMEBASED_INDEX_RESULT,
  ADD_TIMEBASED_CONDITION,
  REMOVE_TIMEBASED_CONDITION,
  CLEAR_TIMEBASED_QUERY,
} from "./actionTypes";

type ResultType = {
  id: number;
  timestamp: $Keys<typeof TIMEBASED_TIMESTAMP_TYPES>;
};

type ConditionType = {
  operator: $Keys<typeof TIMEBASED_OPERATOR_TYPES>;
  result0: ResultType | null;
  result1: ResultType | null;
};

export type TimebasedQueryStateT = {
  indexResult: number | null;
  conditions: ConditionType[];
};

const getEmptyNode = () => ({
  operator: BEFORE,
  result0: null,
  result1: null,
});

const setTimebasedConditionAttributes = (state, action, attributes) => {
  const { conditionIdx } = action.payload;

  return {
    ...state,
    conditions: [
      ...state.conditions.slice(0, conditionIdx),
      {
        ...state.conditions[conditionIdx],
        ...attributes,
      },
      ...state.conditions.slice(conditionIdx + 1),
    ],
  };
};

const setNode = (state, action, node) => {
  const { resultIdx } = action.payload;

  const attributes = {
    [`result${resultIdx}`]: {
      ...node,
      timestamp: node.timestamp || EARLIEST,
    },
  };

  return setTimebasedConditionAttributes(state, action, attributes);
};

const conditionResultsToArray = (conditions) => {
  return conditions.reduce((results, c) => {
    if (c.result0) results.push(c.result0);
    if (c.result1) results.push(c.result1);

    return results;
  }, []);
};

const getPossibleIndexResults = (conditions) => {
  return conditions.reduce((possibleResults, condition, i) => {
    if (condition.operator === DAYS_OR_NO_EVENT_BEFORE && condition.result1)
      possibleResults.push(condition.result1);

    if (condition.operator !== DAYS_OR_NO_EVENT_BEFORE && condition.result0)
      possibleResults.push(condition.result0);

    if (condition.operator !== DAYS_OR_NO_EVENT_BEFORE && condition.result1)
      possibleResults.push(condition.result1);

    return possibleResults;
  }, []);
};

const ensureIndexResult = (state) => {
  // Return if there is already an indexResult
  if (state.indexResult) return state;

  // Ok, so there is none, let's ensure it
  const possibleResults = getPossibleIndexResults(state.conditions);
  // allResults includes results on the left side
  // of conditions with a DAYS_OR_NO_EVENT_BEFORE operator
  const allResults = conditionResultsToArray(state.conditions);

  // Best case
  if (possibleResults.length > 0)
    return { ...state, indexResult: possibleResults[0].id };

  // Bad but ok case
  if (allResults.length > 0) return { ...state, indexResult: allResults[0].id };

  // Well, couldn't find any result
  return { ...state, indexResult: null };
};

const dropTimebasedNode = (state, action) => {
  const { node } = action.payload;
  const stateWithNode = setNode(state, action, node);

  return node.moved ? stateWithNode : ensureIndexResult(stateWithNode);
};

const removeTimebasedNode = (state, action) => {
  const { conditionIdx, resultIdx, moved } = action.payload;
  const node = state.conditions[conditionIdx][`result${resultIdx}`];

  const attributes = {
    [`result${resultIdx}`]: null,
  };

  const stateWithoutNode = setTimebasedConditionAttributes(
    state,
    action,
    attributes,
  );

  if (moved) return stateWithoutNode;

  // If item has not been moved and was indexResult, remove indexResult
  const nextState =
    node.id === state.indexResult
      ? { ...stateWithoutNode, indexResult: null }
      : stateWithoutNode;

  return ensureIndexResult(nextState);
};

const setTimebasedNodeTimestamp = (state, action) => {
  const { conditionIdx, resultIdx, timestamp } = action.payload;

  const attributes = {
    [`result${resultIdx}`]: {
      ...state.conditions[conditionIdx][`result${resultIdx}`],
      timestamp,
    },
  };

  return setTimebasedConditionAttributes(state, action, attributes);
};

const setTimebasedConditionOperator = (state, action) => {
  const { conditionIdx, operator } = action.payload;

  // Check if we're not switching to DAYS_OR_NO_EVENT_BEFORE. Then we're good.
  // But IF IN FACT we do, check if the indexResult is somewhere else than on the left result
  // Then we're also good.
  const nextState = setTimebasedConditionAttributes(state, action, {
    operator,
  });

  if (
    operator !== DAYS_OR_NO_EVENT_BEFORE ||
    !state.conditions[conditionIdx].result0 ||
    state.conditions[conditionIdx].result0.id !== state.indexResult
  )
    return nextState;

  // Now that this didn't work we're switching the operator to DAYS_OR_NO_EVENT_BEFORE
  // and the indexResult points to the first of both results of this condition.
  // This is not allowed with DAYS_OR_NO_EVENT_BEFORE.

  // Let's try to find a possible result to be the new indexResult
  const possibleResults = getPossibleIndexResults(nextState.conditions);

  // Now let's hope we found a possible result
  return possibleResults.length === 0
    ? // Too bad, couldn't find a possible result
      nextState
    : // Nice, take the first result
      {
        ...nextState,
        indexResult: possibleResults[0].id,
      };
};

const setTimebasedConditionMinDays = (state, action) => {
  const { days } = action.payload;

  return setTimebasedConditionAttributes(state, action, { minDays: days });
};

const setTimebasedConditionMaxDays = (state, action) => {
  const { days } = action.payload;

  return setTimebasedConditionAttributes(state, action, { maxDays: days });
};

const setTimebasedConditionMinDaysOrNoEvent = (state, action) => {
  const { days } = action.payload;

  return setTimebasedConditionAttributes(state, action, {
    minDaysOrNoEvent: days,
  });
};

const setTimebasedIndexResult = (state, action) => {
  const { indexResult } = action.payload;

  return {
    ...state,
    indexResult,
  };
};

const addTimebasedCondition = (state, action) => {
  return {
    ...state,
    conditions: [...state.conditions, getEmptyNode()],
  };
};

const removeTimebasedCondition = (state, action) => {
  const { conditionIdx } = action.payload;
  const deletedCondition = state.conditions[conditionIdx];
  const nextState = {
    ...state,
    conditions: [
      ...state.conditions.slice(0, conditionIdx),
      ...state.conditions.slice(conditionIdx + 1),
    ],
  };

  // if no node was indexed
  if (
    !(
      deletedCondition.result0 &&
      deletedCondition.result0.id === state.indexResult
    ) &&
    !(
      deletedCondition.result1 &&
      deletedCondition.result1.id === state.indexResult
    )
  )
    return nextState;

  return ensureIndexResult({ ...nextState, indexResult: null });
};

const renamePreviousQueries = (state, action) => {
  return {
    ...state,
    conditions: state.conditions.map((c) => {
      const result0 =
        c.result0 && c.result0.id === action.payload.queryId
          ? { ...c.result0, label: action.payload.label }
          : c.result0;

      const result1 =
        c.result1 && c.result1.id === action.payload.queryId
          ? { ...c.result1, label: action.payload.label }
          : c.result1;

      return {
        ...c,
        result0,
        result1,
      };
    }),
  };
};

const initialState = {
  indexResult: null,
  conditions: [getEmptyNode()],
};

// This state contains multiple and-conditions.
// Every and-condition sets exactly two previous queries in a time-based relation.
// To be more specific, the RESULTS of two previous queries are set in relation.
// (The relation is measured in days)
//
// The result of this timebased query will look like those from any other query:
//
// A number of results with
//   id,
//   first date of occurrence,
//   last date of occurrence,
//   random dateof occurrence
//
// These dates will have to be taken from a single previous query
// (result1 or result2 from within specific condition) the "indexResult".
//
// Example:
//
// {
//   indexResult: 2523,
//   conditions: [
//     {
//       operator: 'BEFORE',
//       // operator: 'BEFORE_OR_SAME',
//       // operator: 'SAME',
//       result0: {
//         id: 2523,
//         timestamp: 'FIRST',
//         // timestamp: 'LAST',
//         // timestamp: 'RANDOM',
//       },
//       result1: {
//         id: 2525,
//         timestamp: 'LAST'
//       },
//       minDays: 5,
//       maxDays: 10
//     },
//     {
//       operator: 'SAME',
//       result0: { id: 6274, timestamp: "RANDOM" },
//       result1: { id: 5274, timestamp: "RANDOM" },
//     }
//   ]
// }
const timebasedQuery = (
  state: TimebasedQueryStateT = initialState,
  action: Object,
): TimebasedQueryStateT => {
  switch (action.type) {
    case DROP_TIMEBASED_NODE:
      return dropTimebasedNode(state, action);
    case REMOVE_TIMEBASED_NODE:
      return removeTimebasedNode(state, action);
    case SET_TIMEBASED_NODE_TIMESTAMP:
      return setTimebasedNodeTimestamp(state, action);
    case SET_TIMEBASED_CONDITION_OPERATOR:
      return setTimebasedConditionOperator(state, action);
    case SET_TIMEBASED_CONDITION_MIN_DAYS:
      return setTimebasedConditionMinDays(state, action);
    case SET_TIMEBASED_CONDITION_MAX_DAYS:
      return setTimebasedConditionMaxDays(state, action);
    case SET_TIMEBASED_CONDITION_MIN_DAYS_OR_NO_EVENT:
      return setTimebasedConditionMinDaysOrNoEvent(state, action);
    case SET_TIMEBASED_INDEX_RESULT:
      return setTimebasedIndexResult(state, action);
    case ADD_TIMEBASED_CONDITION:
      return addTimebasedCondition(state, action);
    case REMOVE_TIMEBASED_CONDITION:
      return removeTimebasedCondition(state, action);
    case RENAME_PREVIOUS_QUERY_SUCCESS:
      return renamePreviousQueries(state, action);
    case CLEAR_TIMEBASED_QUERY:
      return initialState;
    default:
      return state;
  }
};

export default timebasedQuery;
