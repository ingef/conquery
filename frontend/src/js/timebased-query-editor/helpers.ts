export const allConditionsFilled = timebasedQuery =>
  timebasedQuery.conditions.every(
    condition => !!condition.result0 && !!condition.result1
  );

export const anyConditionFilled = timebasedQuery =>
  timebasedQuery.conditions.some(
    condition => condition.result0 || condition.result1
  );
