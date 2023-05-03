import { DateRangeT } from "../api/types";
import {
  DragItemConceptTreeNode,
  DragItemQuery,
} from "../standard-query-editor/types";

export interface Tree {
  id: string;
  parentId?: string;
  negation?: boolean;
  dates?: {
    restriction?: DateRangeT;
    excluded?: boolean;
  };
  data?: DragItemQuery | DragItemConceptTreeNode;
  children?: {
    connection: "and" | "or" | "before";
    direction: "horizontal" | "vertical";
    items: Tree[];
  };
}
