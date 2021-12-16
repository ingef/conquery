import type { ExternalFormActions } from "../external-forms/actions";
import type { PreviousQueriesFilterActions } from "../previous-queries/filter/actions";
import type { FolderFilterActions } from "../previous-queries/folderFilter/actions";
import type { PreviousQueryListActions } from "../previous-queries/list/actions";
import type { QueriesSearchActions } from "../previous-queries/search/actions";
import type { QueryGroupModalActions } from "../query-group-modal/actions";
import type { QueryUploadConceptListModalActions } from "../query-upload-concept-list-modal/actions";
import type { SnackMessageActions } from "../snack-message/actions";
import type { StandardQueryEditorActions } from "../standard-query-editor/actions";
import type { StartupActions } from "../startup/actions";
import type { TimebasedActions } from "../timebased-query-editor/actions";
import type { TooltipActions } from "../tooltip/actions";
import type { UploadConceptListModalActions } from "../upload-concept-list-modal/actions";

type ReduxInitAction = { type: "@@INIT" };

export type Action =
  | ReduxInitAction
  | FolderFilterActions
  | QueriesSearchActions
  | PreviousQueriesFilterActions
  | StandardQueryEditorActions
  | QueryGroupModalActions
  | QueryUploadConceptListModalActions
  | UploadConceptListModalActions
  | SnackMessageActions
  | PreviousQueryListActions
  | TimebasedActions
  | StartupActions
  | TooltipActions
  | ExternalFormActions;
