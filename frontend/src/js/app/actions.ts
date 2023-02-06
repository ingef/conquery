import type { ConceptTreesOpenActions } from "../concept-trees-open/actions";
import type { ConceptTreeActions } from "../concept-trees/actions";
import type { DatasetActions } from "../dataset/actions";
import type { EntityHistoryActions } from "../entity-history/actions";
import type { ExternalFormActions } from "../external-forms/actions";
import type { PaneActions } from "../pane/actions";
import type { PreviewActions } from "../preview/actions";
import type { ProjectItemsFilterActions } from "../previous-queries/filter/actions";
import type { FolderFilterActions } from "../previous-queries/folder-filter/actions";
import type { PreviousQueryListActions } from "../previous-queries/list/actions";
import type { ProjectItemsSearchActions } from "../previous-queries/search/actions";
import type { ProjectItemsTypeFilterActions } from "../previous-queries/type-filter/actions";
import type { QueryGroupModalActions } from "../query-group-modal/actions";
import type { QueryRunnerActions } from "../query-runner/actions";
import type { QueryUploadConceptListModalActions } from "../query-upload-concept-list-modal/actions";
import type { SnackMessageActions } from "../snack-message/actions";
import type { StandardQueryEditorActions } from "../standard-query-editor/actions";
import type { StartupActions } from "../startup/actions";
import type { TimebasedActions } from "../timebased-query-editor/actions";
import type { TooltipActions } from "../tooltip/actions";
import type { UploadConceptListModalActions } from "../upload-concept-list-modal/actions";
import type { UserActions } from "../user/actions";

type ReduxInitAction = { type: "@@INIT" };

export type Action =
  | ReduxInitAction
  | FolderFilterActions
  | ProjectItemsSearchActions
  | ProjectItemsFilterActions
  | ProjectItemsTypeFilterActions
  | StandardQueryEditorActions
  | QueryGroupModalActions
  | QueryUploadConceptListModalActions
  | UploadConceptListModalActions
  | SnackMessageActions
  | PreviousQueryListActions
  | TimebasedActions
  | StartupActions
  | TooltipActions
  | ExternalFormActions
  | QueryRunnerActions
  | PaneActions
  | DatasetActions
  | PreviewActions
  | ConceptTreeActions
  | ConceptTreesOpenActions
  | UserActions
  | EntityHistoryActions;
