import { combineReducers } from "redux";

import conceptTreesOpen, {
  ConceptTreesOpenStateT,
} from "../concept-trees-open/reducer";
import conceptTrees, { ConceptTreesStateT } from "../concept-trees/reducer";
import datasets, { DatasetStateT } from "../dataset/reducer";
import entityHistory, { EntityHistoryStateT } from "../entity-history/reducer";
import type { Form } from "../external-forms/config-types";
import {
  activeFormReducer,
  availableFormsReducer,
} from "../external-forms/reducer";
import panes, { PanesStateT } from "../pane/reducer";
import preview, { PreviewStateT } from "../preview/reducer";
import projectItemsFilter, {
  ProjectItemsFilterStateT,
} from "../previous-queries/filter/reducer";
import previousQueriesFolderFilter, {
  PreviousQueriesFolderFilterStateT,
} from "../previous-queries/folder-filter/reducer";
import previousQueries, {
  PreviousQueriesStateT,
} from "../previous-queries/list/reducer";
import projectItemsSearch, {
  ProjectItemsSearchStateT,
} from "../previous-queries/search/reducer";
import projectItemsTypeFilter, {
  ProjectItemsTypeFilterStateT,
} from "../previous-queries/type-filter/reducer";
import createQueryRunnerReducer, {
  QueryRunnerStateT,
} from "../query-runner/reducer";
import snackMessage, { SnackMessageStateT } from "../snack-message/reducer";
import queryReducer, {
  StandardQueryStateT,
} from "../standard-query-editor/queryReducer";
import selectedSecondaryIdsReducer, {
  SelectedSecondaryIdStateT,
} from "../standard-query-editor/selectedSecondaryIdReducer";
import startup, { StartupStateT } from "../startup/reducer";
import timebasedQueryReducer, {
  TimebasedQueryStateT,
} from "../timebased-query-editor/reducer";
import tooltip, { TooltipStateT } from "../tooltip/reducer";
import uploadConceptListModal, {
  UploadConceptListModalStateT,
} from "../upload-concept-list-modal/reducer";
import user, { UserStateT } from "../user/reducer";

export type StateT = {
  conceptTrees: ConceptTreesStateT;
  conceptTreesOpen: ConceptTreesOpenStateT;
  datasets: DatasetStateT;
  tooltip: TooltipStateT;
  panes: PanesStateT;
  uploadConceptListModal: UploadConceptListModalStateT;
  user: UserStateT;
  startup: StartupStateT;
  previousQueries: PreviousQueriesStateT;
  projectItemsSearch: ProjectItemsSearchStateT;
  projectItemsFilter: ProjectItemsFilterStateT;
  projectItemsTypeFilter: ProjectItemsTypeFilterStateT;
  previousQueriesFolderFilter: PreviousQueriesFolderFilterStateT;
  preview: PreviewStateT;
  snackMessage: SnackMessageStateT;
  editorV2QueryRunner: QueryRunnerStateT;
  queryEditor: {
    query: StandardQueryStateT;
    selectedSecondaryId: SelectedSecondaryIdStateT;
    queryRunner: QueryRunnerStateT;
  };
  timebasedQueryEditor: {
    timebasedQuery: TimebasedQueryStateT;
    timebasedQueryRunner: QueryRunnerStateT;
  };
  externalForms: {
    activeForm: string | null;
    queryRunner: QueryRunnerStateT;
    availableForms: {
      [formName: string]: Form;
    };
  };
  entityHistory: EntityHistoryStateT;
};

const buildAppReducer = () => {
  return combineReducers({
    startup,
    conceptTrees,
    conceptTreesOpen,
    uploadConceptListModal,
    datasets,
    tooltip,
    panes,
    previousQueries,
    projectItemsSearch,
    projectItemsFilter,
    projectItemsTypeFilter,
    previousQueriesFolderFilter,
    snackMessage,
    preview,
    user,
    entityHistory,
    editorV2QueryRunner: createQueryRunnerReducer("editorV2"),
    queryEditor: combineReducers({
      query: queryReducer,
      selectedSecondaryId: selectedSecondaryIdsReducer,
      queryRunner: createQueryRunnerReducer("standard"),
    }),
    timebasedQueryEditor: combineReducers({
      timebasedQuery: timebasedQueryReducer,
      timebasedQueryRunner: createQueryRunnerReducer("timebased"),
    }),
    externalForms: combineReducers({
      activeForm: activeFormReducer,
      availableForms: availableFormsReducer,
      queryRunner: createQueryRunnerReducer("externalForms"),
    }),
  });
};

export default buildAppReducer;
