import type { DateRangeT } from "../api/types";
import type {
  DragItemConceptTreeNode,
  DragItemQuery,
} from "../standard-query-editor/types";

export type ConnectionKind = "and" | "or" | "time";
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
  children?: TreeChildren;
}

export interface TreeChildrenBase {
  direction: DirectionKind;
  items: Tree[];
}

export interface TreeChildrenAnd extends TreeChildrenBase {
  connection: "and";
}
export interface TreeChildrenOr extends TreeChildrenBase {
  connection: "or";
}

export const TIME_OPERATORS: ("BEFORE" | "AFTER" | "WHILE")[] = [
  "BEFORE",
  "AFTER",
  "WHILE",
] as const;
export const TIME_TIMESTAMPS: ("ALL" | "ANY" | "EARLIEST" | "LATEST")[] = [
  "ALL",
  "ANY",
  "EARLIEST",
  "LATEST",
] as const;

export type TimeTimestamp = (typeof TIME_TIMESTAMPS)[number];
export type TimeOperator = (typeof TIME_OPERATORS)[number];

export interface TreeChildrenTime extends TreeChildrenBase {
  connection: "time";
  operator: TimeOperator;
  timestamps: TimeTimestamp[]; // items.length
  interval?: {
    min: number | null;
    max: number | null;
  };
}
export type TreeChildren = TreeChildrenAnd | TreeChildrenOr | TreeChildrenTime;

export interface EditorV2Query {
  tree?: Tree;
}
