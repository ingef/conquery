import { DateRangeT } from "../api/types";

export interface Tree {
  id: string;
  parentId?: string;
  negation?: boolean;
  dateRestriction?: DateRangeT;
  data?: any;
  children?: {
    connection: "and" | "or" | "time";
    direction: "horizontal" | "vertical";
    items: Tree[];
  };
}
