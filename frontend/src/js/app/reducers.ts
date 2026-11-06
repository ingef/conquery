import { combineReducers } from "redux";
import conceptTrees, {
  type ConceptTreesStateT,
} from "../concept-trees/reducer";
import conceptTreesOpen, {
  type ConceptTreesOpenStateT,
} from "../concept-trees-open/reducer";
import datasets, { type DatasetStateT } from "../dataset/reducer";
import entityHistory, {
  type EntityHistoryStateT,
} from "../entity-history/reducer";
import type { Form } from "../external-forms/config-types";
import {
  activeFormReducer,
  availableFormsReducer,
} from "../external-forms/reducer";
import infoPane, { type InfoPaneStateT } from "../info-pane/reducer";
import panes, { type PanesStateT } from "../pane/reducer";
import preview, { type PreviewStateT } from "../preview/reducer";
import projectItemsFilter, {
  type ProjectItemsFilterStateT,
} from "../previous-queries/filter/reducer";
import previousQueriesFolderFilter, {
  type PreviousQueriesFolderFilterStateT,
} from "../previous-queries/folder-filter/reducer";
import previousQueries, {
  type PreviousQueriesStateT,
} from "../previous-queries/list/reducer";
import projectItemsSearch, {
  type ProjectItemsSearchStateT,
} from "../previous-queries/search/reducer";
import projectItemsTypeFilter, {
  type ProjectItemsTypeFilterStateT,
} from "../previous-queries/type-filter/reducer";
import createQueryRunnerReducer, {
  type QueryRunnerStateT,
} from "../query-runner/reducer";
import snackMessage, {
  type SnackMessageStateT,
} from "../snack-message/reducer";
import queryReducer, {
  type StandardQueryStateT,
} from "../standard-query-editor/queryReducer";
import selectedSecondaryIdsReducer, {
  type SelectedSecondaryIdStateT,
} from "../standard-query-editor/selectedSecondaryIdReducer";
import startup, { type StartupStateT } from "../startup/reducer";
import uploadConceptListModal, {
  type UploadConceptListModalStateT,
} from "../upload-concept-list-modal/reducer";
import user, { type UserStateT } from "../user/reducer";

export type StateT = {
  conceptTrees: ConceptTreesStateT;
  conceptTreesOpen: ConceptTreesOpenStateT;
  datasets: DatasetStateT;
  infoPane: InfoPaneStateT;
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
    infoPane,
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
    externalForms: combineReducers({
      activeForm: activeFormReducer,
      availableForms: availableFormsReducer,
      queryRunner: createQueryRunnerReducer("externalForms"),
    }),
  });
};

export default buildAppReducer;
