import type { StateT as BaseStateT } from "./js/app/reducers";
import { StandardQueryEditorStateT } from "js/standard-query-editor";
import { TimebasedQueryEditorStateT } from "js/timebased-query-editor";
import { ExternalFormsStateT } from "js/external-forms";

export interface StateT extends BaseStateT {
  queryEditor: StandardQueryEditorStateT;
  timebasedQueryEditor: TimebasedQueryEditorStateT;
  externalForms: ExternalFormsStateT;
}
