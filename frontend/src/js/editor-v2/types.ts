import { DateRangeT } from "../api/types";
import {
  DragItemConceptTreeNode,
  DragItemQuery,
} from "../standard-query-editor/types";

export type ConnectionKind = "and" | "or" | "before";
export type DirectionKind = "horizontal" | "vertical";

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
    connection: ConnectionKind;
    direction: DirectionKind;
    items: Tree[];
  };
}

export interface EditorV2Query {
  tree?: Tree;
}
