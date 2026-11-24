import type { DNDType } from "../common/constants/dndTypes";

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
