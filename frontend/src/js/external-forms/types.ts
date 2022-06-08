import type { DatasetT } from "../api/types";
import { DNDType } from "../common/constants/dndTypes";

export interface ExternalFormPropsType {
  selectedDatasetId: DatasetT["id"];
}

export interface DragItemFormConfig {
  dragContext: {
    width: number;
    height: number;
  };
  type: DNDType.FORM_CONFIG;
  id: string;
  label: string;
  tags: string[];
  own: boolean;
  shared: boolean;
}
