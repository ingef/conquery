import type { ProjectItemT } from "./ProjectItem";
import type { FormConfigT } from "./reducer";

export const isFormConfig = (item: ProjectItemT): item is FormConfigT =>
  "formType" in item;
