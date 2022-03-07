import { FormConfigT } from "js/external-forms/form-configs/reducer";

import type { ProjectItemT } from "./ProjectItem";

export const isFormConfig = (item: ProjectItemT): item is FormConfigT =>
  "formType" in item;
