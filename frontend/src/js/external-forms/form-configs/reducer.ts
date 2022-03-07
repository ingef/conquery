import { UserGroupIdT } from "../../api/types";

import {
  LOAD_CONFIGS_SUCCESS,
  LOAD_CONFIGS_ERROR,
  PATCH_CONFIG_SUCCESS,
  DELETE_CONFIG_SUCCESS,
} from "./actionTypes";

export interface BaseFormConfigT {
  formType: string;
  values: Record<string, any>;
  label: string;
}

export interface FormConfigT extends BaseFormConfigT {
  id: string;
  tags: string[];
  createdAt: string; // Datetime
  own: boolean;
  shared: boolean;
  system: boolean;
  ownerName: string;
  isPristineLabel?: boolean;
  groups?: UserGroupIdT[];
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
  names: [],
};

const sortConfigs = (configs: FormConfigT[]) => {
  return configs.sort((a, b) => {
    return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
  });
};

const findConfig = (configs: FormConfigT[], configId: string | number) => {
  const config = configs.find((c) => c.id === configId);

  return {
    config,
    idx: config ? configs.indexOf(config) : -1,
  };
};

const findUniqueTags = (configs: FormConfigT[]) => {
  const uniqueTags = new Set<string>();

  configs.forEach((config) => {
    if (config.tags) config.tags.forEach((tag) => uniqueTags.add(tag));
  });

  return Array.from(uniqueTags);
};

const findUniqueNames = (queries: FormConfigT[]) => {
  const uniqueNames = new Set<string>();

  queries.filter((q) => !!q.label).forEach((q) => uniqueNames.add(q.label));

  return Array.from(uniqueNames);
};

const updateFormConfig = (
  configs: FormConfigT[],
  { id, values }: { id: string; values: Partial<FormConfigT> },
) => {
  const config = configs.find((conf) => conf.id === id);

  if (!config) {
    return configs;
  }

  const idx = configs.indexOf(config);

  return [
    ...configs.slice(0, idx),
    {
      ...config,
      ...values,
    },
    ...configs.slice(idx + 1),
  ];
};

const formConfigs = (
  state: FormConfigsStateT = initialState,
  action: any,
): FormConfigsStateT => {
  switch (action.type) {
    case LOAD_CONFIGS_SUCCESS:
      return {
        ...state,
        data: sortConfigs(action.payload.data),
        error: false,
        tags: findUniqueTags(action.payload.data),
        names: findUniqueNames(action.payload.data),
      };
    case PATCH_CONFIG_SUCCESS:
      const data = updateFormConfig(state.data, action.payload);

      return {
        ...state,
        data,
        error: false,
        tags: findUniqueTags(data),
        names: findUniqueNames(data),
      };
    case DELETE_CONFIG_SUCCESS:
      const { idx } = findConfig(state.data, action.payload.configId);

      return {
        ...state,
        data: [...state.data.slice(0, idx), ...state.data.slice(idx + 1)],
      };
    case LOAD_CONFIGS_ERROR:
      return {
        ...state,
        error: true,
      };
    default:
      return state;
  }
};

export default formConfigs;
