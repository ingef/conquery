// import { createActionTypes }     from './actionTypes';

export default function createQueryNodeEditorReducer(type: string) {
  // In the future: initialize QueryNodeEditor-internal state here
  const initialState = { };

  // In the future: import QueryNodeEditor-internal action types here
  // const { } = createActionTypes(type);

  return (state = initialState, action) => {
    switch (action.type) {
      default:
        return state;
    }
  };
}
