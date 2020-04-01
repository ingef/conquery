import {
  LOAD_CONFIGS_SUCCESS,
  LOAD_CONFIGS_ERROR,
  PATCH_CONFIG_SUCCESS,
  DELETE_CONFIG_SUCCESS
} from "./actionTypes";

export interface FormConfigT {
  id: string;
  label: string;
  createdAt: string; // Datetime
  tags: string[];
  own: boolean;
  shared: boolean;
  system: boolean;
  ownerName: string;
}

export interface FormConfigsStateT {
  error: boolean;
  data: FormConfigT[];
  tags: string[];
  names: string[];
}

const initialState: FormConfigsStateT = {
  error: false,
  data: [],
  tags: [],
  names: []
};

const sortConfigs = (configs: FormConfigT[]) => {
  return configs.sort((a, b) => {
    return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
  });
};

const deleteFormConfig = (state: FormConfigsStateT, action: Object) => {};

const findUniqueTags = (configs: FormConfigT[]) => {
  const uniqueTags = new Set<string>();

  configs.forEach(config => {
    if (config.tags) config.tags.forEach(tag => uniqueTags.add(tag));
  });

  return Array.from(uniqueTags);
};

const findUniqueNames = (queries: FormQueryT[]) => {
  const uniqueNames = new Set<string>();

  queries.filter(q => !!q.label).forEach(q => uniqueNames.add(q.label));

  return Array.from(uniqueNames);
};

const updateFormConfig = (configs: FormConfigT[], { id, values }) => {
  const config = configs.find(conf => conf.id === id);

  if (!config) {
    return configs;
  }

  const idx = configs.indexOf(config);

  return [
    ...configs.slice(0, idx),
    {
      ...config,
      ...values
    },
    ...configs.slice(idx + 1)
  ];
};

const formConfigs = (
  state: FormConfigsStateT = initialState,
  action: Object
): FormConfigsStateT => {
  switch (action.type) {
    case LOAD_CONFIGS_SUCCESS:
      return {
        ...state,
        data: sortConfigs(action.payload.data),
        error: false,
        tags: findUniqueTags(action.payload.data),
        names: findUniqueNames(action.payload.data)
      };
    case PATCH_CONFIG_SUCCESS:
      const data = updateFormConfig(state.data, action.payload);

      return {
        ...state,
        data,
        error: false,
        tags: findUniqueTags(data),
        names: findUniqueNames(data)
      };
    case DELETE_CONFIG_SUCCESS:
      const idx = findQuery(state.queries, action.payload.queryId);

      return {
        ...state,
        data: [
          ...state.data.slice(0, queryIdx),
          ...state.data.slice(queryIdx + 1)
        ]
      };
    case LOAD_CONFIGS_ERROR:
      return {
        ...state,
        error: true
      };
    default:
      return state;
  }
};

export default formConfigs;
