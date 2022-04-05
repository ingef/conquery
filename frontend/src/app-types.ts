import type { StateT as BaseStateT } from "./js/app/reducers";
import type { ExternalFormsStateT } from "./js/external-forms";
import type { StandardQueryEditorStateT } from "./js/standard-query-editor";

export interface StateT extends BaseStateT {
  queryEditor: StandardQueryEditorStateT;
  externalForms: ExternalFormsStateT;
}
