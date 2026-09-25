import {
  GridList as RacGridList,
  GridListItem as RacGridListItem,
  type GridListItemProps as RacGridListItemProps,
  type GridListProps as RacGridListProps,
} from "react-aria-components";
import { tv } from "tailwind-variants";

const list = tv({ base: ["flex flex-col", "gap-2", "outline-none"] });

const item = tv({
  base: [
    "flex items-center",
    "gap-1",
    "h-[30px] px-2",
    "rounded",
    "cursor-pointer",
    "outline-none",
    "transition-[background-color] duration-100",
    "data-hovered:bg-gray-50",
    "data-selected:bg-primary-50 data-selected:text-primary-500",
    "data-selected:data-hovered:bg-primary-100",
    "data-focus-visible:outline-2 data-focus-visible:-outline-offset-2 data-focus-visible:outline-primary-500",
    "data-disabled:cursor-not-allowed data-disabled:opacity-40",
  ],
});

export interface GridListProps<T extends object>
  extends Omit<RacGridListProps<T>, "className" | "style"> {
  "aria-label": string;
}

/**
 * A list of 30 px rows that may hold controls, on react-aria-components:
 * arrow keys move between rows and into a row's controls, `selectionMode`
 * with `selectedKeys` / `onSelectionChange` marks the current row.
 *
 *   <GridList aria-label="Sources" selectionMode="single" selectedKeys={[id]}>
 *     <GridListItem id="a" textValue="Region A">
 *       <CheckboxField aria-label="Include region A" … />
 *       <C2 truncate>Region A</C2>
 *     </GridListItem>
 *   </GridList>
 *
 * Rows are keyed by `id`; `textValue` names a row whose children are not text.
 */
export const GridList = <T extends object>(props: GridListProps<T>) => (
  <RacGridList className={list()} {...props} />
);

export interface GridListItemProps
  extends Omit<RacGridListItemProps, "className" | "style"> {}

export const GridListItem = (props: GridListItemProps) => (
  <RacGridListItem className={item()} {...props} />
);
