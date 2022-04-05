import type { StateT as BaseStateT } from "./js/app/reducers";
import type { ExternalFormsStateT } from "./js/external-forms";

export interface StateT extends BaseStateT {
  externalForms: ExternalFormsStateT;
}
