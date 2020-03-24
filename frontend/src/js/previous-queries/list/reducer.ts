import {
  LOAD_PREVIOUS_QUERIES_START,
  LOAD_PREVIOUS_QUERIES_SUCCESS,
  LOAD_PREVIOUS_QUERIES_ERROR,
  LOAD_PREVIOUS_QUERY_START,
  LOAD_PREVIOUS_QUERY_SUCCESS,
  LOAD_PREVIOUS_QUERY_ERROR,
  RENAME_PREVIOUS_QUERY_START,
  RENAME_PREVIOUS_QUERY_SUCCESS,
  RENAME_PREVIOUS_QUERY_ERROR,
  TOGGLE_EDIT_PREVIOUS_QUERY_LABEL,
  TOGGLE_EDIT_PREVIOUS_QUERY_TAGS,
  RETAG_PREVIOUS_QUERY_START,
  RETAG_PREVIOUS_QUERY_SUCCESS,
  RETAG_PREVIOUS_QUERY_ERROR,
  TOGGLE_SHARE_PREVIOUS_QUERY_START,
  TOGGLE_SHARE_PREVIOUS_QUERY_SUCCESS,
  TOGGLE_SHARE_PREVIOUS_QUERY_ERROR,
  DELETE_PREVIOUS_QUERY_START,
  DELETE_PREVIOUS_QUERY_SUCCESS,
  DELETE_PREVIOUS_QUERY_ERROR
} from "./actionTypes";

const initialState = {
  queries: []
};

const findQuery = (queries, queryId) => {
  const query = queries.find(q => q.id === queryId);

  return {
    query,
    queryIdx: queries.indexOf(query)
  };
};

const updatePreviousQuery = (state, action, attributes) => {
  const { query, queryIdx } = findQuery(state.queries, action.payload.queryId);
  return {
    ...state,
    queries: [
      ...state.queries.slice(0, queryIdx),
      {
        ...query,
        ...attributes
      },
      ...state.queries.slice(queryIdx + 1)
    ]
  };
};

const sortQueries = queries => {
  return queries.sort((a, b) => {
    return new Date(b.createdAt) - new Date(a.createdAt);
  });
};

const toggleQueryAttribute = (state, action, attribute) => {
  const { query } = findQuery(state.queries, action.payload.queryId);

  return updatePreviousQuery(state, action, { [attribute]: !query[attribute] });
};

const deletePreviousQuery = (state, action) => {
  const { queryIdx } = findQuery(state.queries, action.payload.queryId);

  return {
    ...state,
    queries: [
      ...state.queries.slice(0, queryIdx),
      ...state.queries.slice(queryIdx + 1)
    ]
  };
};

const findUniqueTags = queries => {
  const uniqueTags = new Set();

  queries.forEach(query => {
    if (query.tags) query.tags.forEach(tag => uniqueTags.add(tag));
  });

  return Array.from(uniqueTags);
};

const findNewTags = tags => {
  if (!tags) return [];

  let uniqueTags = new Set();

  tags.forEach(tag => uniqueTags.add(tag));

  return Array.from(uniqueTags);
};

const findUniqueNames = queries => {
  const uniqueNames = new Set();

  queries.filter(q => !!q.label).forEach(q => uniqueNames.add(q.label));

  return Array.from(uniqueNames);
};

const updateUniqueNames = (existingNames, newName) => {
  return existingNames.includes(newName)
    ? existingNames
    : [newName, ...existingNames];
};

const previousQueriesReducer = (state = initialState, action) => {
  switch (action.type) {
    case LOAD_PREVIOUS_QUERIES_START:
      return { ...state, loading: true };
    case LOAD_PREVIOUS_QUERIES_SUCCESS:
      return {
        ...state,
        loading: false,
        queries: sortQueries(action.payload.data),
        tags: findUniqueTags(action.payload.data),
        names: findUniqueNames(action.payload.data)
      };
    case LOAD_PREVIOUS_QUERIES_ERROR:
      return { ...state, loading: false, error: action.payload.message };
    case LOAD_PREVIOUS_QUERY_START:
    case RENAME_PREVIOUS_QUERY_START:
    case RETAG_PREVIOUS_QUERY_START:
    case TOGGLE_SHARE_PREVIOUS_QUERY_START:
    case DELETE_PREVIOUS_QUERY_START:
      return updatePreviousQuery(state, action, { loading: true });
    case LOAD_PREVIOUS_QUERY_SUCCESS:
      return updatePreviousQuery(state, action, {
        loading: false,
        error: null,
        ...action.payload.data
      });
    case RENAME_PREVIOUS_QUERY_SUCCESS:
      return {
        ...updatePreviousQuery(state, action, {
          loading: false,
          error: null,
          label: action.payload.label
        }),
        names: updateUniqueNames(state.names, action.payload.label)
      };
    case RETAG_PREVIOUS_QUERY_SUCCESS:
      return {
        ...updatePreviousQuery(state, action, {
          loading: false,
          error: null,
          tags: action.payload.tags
        }),
        tags: findNewTags([...state.tags, ...action.payload.tags])
      };
    case TOGGLE_SHARE_PREVIOUS_QUERY_SUCCESS:
      return updatePreviousQuery(state, action, {
        loading: false,
        error: null,
        shared: action.payload.shared
      });
    case DELETE_PREVIOUS_QUERY_SUCCESS:
      return deletePreviousQuery(state, action);
    case LOAD_PREVIOUS_QUERY_ERROR:
    case RENAME_PREVIOUS_QUERY_ERROR:
    case RETAG_PREVIOUS_QUERY_ERROR:
    case TOGGLE_SHARE_PREVIOUS_QUERY_ERROR:
    case DELETE_PREVIOUS_QUERY_ERROR:
      return updatePreviousQuery(state, action, {
        loading: false,
        error: action.payload.message
      });
    case TOGGLE_EDIT_PREVIOUS_QUERY_LABEL:
      return toggleQueryAttribute(state, action, "editingLabel");
    case TOGGLE_EDIT_PREVIOUS_QUERY_TAGS:
      return toggleQueryAttribute(state, action, "editingTags");
    default:
      return state;
  }
};

export default previousQueriesReducer;
