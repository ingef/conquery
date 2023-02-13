import { getType } from "typesafe-actions";

import type { QueryIdT, UserGroupIdT } from "../../api/types";
import { Action } from "../../app/actions";

import {
  addFolder,
  deleteFormConfigSuccess,
  deleteQuerySuccess,
  loadFormConfigsSuccess,
  loadQueriesSuccess,
  loadQuerySuccess,
  patchFormConfigSuccess,
  patchQuerySuccess,
  removeFolder,
} from "./actions";

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

export interface PreviousQueryT {
  id: string;
  label: string;
  numberOfResults: number | null;
  createdAt: string;
  tags: string[];
  own: boolean;
  ownerName: string;
  system?: boolean;
  resultUrls: string[];
  shared: boolean;
  isPristineLabel?: boolean;
  canExpand?: boolean;
  groups?: UserGroupIdT[];
  queryType: "CONCEPT_QUERY" | "SECONDARY_ID_QUERY";
  secondaryId?: string | null;
}

export interface PreviousQueriesStateT {
  localFolders: string[];
  queries: PreviousQueryT[];
  formConfigs: FormConfigT[];
}

const initialState: PreviousQueriesStateT = {
  localFolders: [],
  queries: [],
  formConfigs: [],
};

const findItem = <T extends PreviousQueryT | FormConfigT>(
  items: T[],
  itemId: string | number,
) => {
  const idx = items.findIndex((i) => i.id === itemId);

  return {
    item: idx === -1 ? undefined : items[idx],
    itemIdx: idx,
  };
};

const updatePreviousQuery = (
  state: PreviousQueriesStateT,
  { payload: { id } }: { payload: { id: QueryIdT } },
  attributes: Partial<PreviousQueryT>,
) => {
  const { item, itemIdx } = findItem(state.queries, id);

  if (!item) return state;

  return {
    ...state,
    queries: [
      ...state.queries.slice(0, itemIdx),
      {
        ...item,
        ...attributes,
        shared: attributes.groups ? attributes.groups.length > 0 : false,
      },
      ...state.queries.slice(itemIdx + 1),
    ],
  };
};

const updateFormConfig = (
  state: PreviousQueriesStateT,
  { payload: { id } }: { payload: { id: FormConfigT["id"] } },
  attributes: Partial<FormConfigT>,
) => {
  const { item, itemIdx } = findItem(state.formConfigs, id);

  if (!item) return state;

  return {
    ...state,
    formConfigs: [
      ...state.formConfigs.slice(0, itemIdx),
      {
        ...item,
        ...attributes,
        shared: attributes.groups ? attributes.groups.length > 0 : false,
      },
      ...state.formConfigs.slice(itemIdx + 1),
    ],
  };
};

const sortProjectItems = <T extends PreviousQueryT | FormConfigT>(
  queries: T[],
) => {
  return queries.sort((a, b) => {
    return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
  });
};

const deletePreviousQuery = (
  state: PreviousQueriesStateT,
  { queryId }: { queryId: QueryIdT },
) => {
  const { itemIdx } = findItem(state.queries, queryId);

  return {
    ...state,
    queries: [
      ...state.queries.slice(0, itemIdx),
      ...state.queries.slice(itemIdx + 1),
    ],
  };
};

const deleteFormConfig = (
  state: PreviousQueriesStateT,
  { configId }: { configId: FormConfigT["id"] },
) => {
  const { itemIdx } = findItem(state.formConfigs, configId);

  return {
    ...state,
    formConfigs: [
      ...state.formConfigs.slice(0, itemIdx),
      ...state.formConfigs.slice(itemIdx + 1),
    ],
  };
};

const removeLocalFolder = (
  state: PreviousQueriesStateT,
  { folderName }: { folderName: string },
) => {
  const { localFolders } = state;
  const idx = localFolders.findIndex((f) => f === folderName);

  return idx === -1
    ? state
    : {
        ...state,
        localFolders: [
          ...localFolders.slice(0, idx),
          ...localFolders.slice(idx + 1),
        ],
      };
};

const previousQueriesReducer = (
  state: PreviousQueriesStateT = initialState,
  action: Action,
): PreviousQueriesStateT => {
  switch (action.type) {
    case getType(loadQueriesSuccess):
      return {
        ...state,
        queries: sortProjectItems(action.payload.data),
      };
    case getType(loadFormConfigsSuccess):
      return {
        ...state,
        formConfigs: sortProjectItems(action.payload.data),
      };
    case getType(loadQuerySuccess):
    case getType(patchQuerySuccess):
      return updatePreviousQuery(state, action, action.payload.data);
    case getType(patchFormConfigSuccess):
      return updateFormConfig(state, action, action.payload.data);
    // TODO: VALIDATE THAT THIS WAS EVEN USEFUL
    // localFolders: state.localFolders.filter(
    //   (folder) => !action.payload.tags.includes(folder),
    // ),
    // TODO: VALIDATE THAT THIS IS DONE ALREADY BY THE BACKEND AND "UPDATE" IS ENOUGH
    // case getType(shareQuerySuccess):
    //   return updatePreviousQuery(state, action, {
    //     groups: action.payload.groups,
    //     ...(!action.payload.groups || action.payload.groups.length === 0
    //       ? { shared: false }
    //       : { shared: true }),
    //   });
    case getType(deleteQuerySuccess):
      return deletePreviousQuery(state, action.payload);
    case getType(deleteFormConfigSuccess):
      return deleteFormConfig(state, action.payload);
    case getType(addFolder):
      return {
        ...state,
        localFolders: [...state.localFolders, action.payload.folderName],
      };
    case getType(removeFolder):
      return removeLocalFolder(state, action.payload);
    default:
      return state;
  }
};

export default previousQueriesReducer;
