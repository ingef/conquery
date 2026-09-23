import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";

import type { PostPrefixForSuggestionsParams } from "../api/api";
import type {
  PostFilterSuggestionsResponseT,
  SelectOptionT,
  SelectorResultType,
} from "../api/types";
import { nodeIsConceptQueryNode } from "../model/node";
import type {
  ConceptQueryNodeType,
  FilterWithValueType,
  StandardQueryNodeT,
} from "../standard-query-editor/types";
import { H3 } from "../ui-components/Typography";
import CommonNodeSettings from "./CommonNodeSettings";
import ContentCell from "./ContentCell";
import NodeSelects from "./NodeSelects";
import TableView from "./TableView";
import { COMMON_SECTION } from "./useSectionSpy";

const sectionHeading = tv({ base: "mx-2 mt-2" });

const contentCellGroup = tv({
  base: [
    "pb-2",
    "mb-2",
    "border-b border-gray-100",
    "last-of-type:border-b-0 last-of-type:pb-0 last-of-type:mb-0",
  ],
});

const ContentColumn = ({
  node,
  registerSection,
  blocklistedSelects,
  allowlistedSelects,
  onLoadFilterSuggestions,
  onSetDateColumn,
  onSetFilterValue,
  onSelectSelects,
  onSelectTableSelects,
  onToggleTimestamps,
  onToggleSecondaryIdExclude,
}: {
  node: StandardQueryNodeT;
  /** a ref callback per section key, for the navigation to follow the scroll */
  registerSection: (key: string) => (element: HTMLElement | null) => void;
  blocklistedSelects?: SelectorResultType[];
  allowlistedSelects?: SelectorResultType[];
  onSelectSelects: (value: SelectOptionT[]) => void;
  onSelectTableSelects: (tableIdx: number, value: SelectOptionT[]) => void;
  onToggleTimestamps?: () => void;
  onToggleSecondaryIdExclude?: () => void;
  onSetFilterValue: (
    tableIdx: number,
    filterIdx: number,
    value: FilterWithValueType["value"],
  ) => void;
  onLoadFilterSuggestions: (
    params: PostPrefixForSuggestionsParams,
    tableIdx: number,
    filterIdx: number,
    config?: { returnOnly?: boolean },
  ) => Promise<PostFilterSuggestionsResponseT | null>;
  onSetDateColumn: (tableIdx: number, value: string) => void;
}) => {
  const { t } = useTranslation();

  const tables = nodeIsConceptQueryNode(node) ? node.tables : [];

  return (
    <div className="flex w-full flex-col">
      <ContentCell
        className={contentCellGroup()}
        ref={registerSection(COMMON_SECTION)}
      >
        <div className={sectionHeading()}>
          <H3>{t("queryNodeEditor.properties")}</H3>
        </div>
        {(onToggleSecondaryIdExclude || onToggleTimestamps) && (
          <CommonNodeSettings
            excludeFromSecondaryId={node.excludeFromSecondaryId}
            onToggleSecondaryIdExclude={onToggleSecondaryIdExclude}
            excludeTimestamps={node.excludeTimestamps}
            onToggleTimestamps={onToggleTimestamps}
          />
        )}
        {nodeIsConceptQueryNode(node) && node.selects && (
          <NodeSelects
            selects={node.selects}
            onSelectSelects={onSelectSelects}
            blocklistedSelects={blocklistedSelects}
            allowlistedSelects={allowlistedSelects}
          />
        )}
      </ContentCell>
      {tables.map((table, idx) => {
        if (table.exclude) {
          return null;
        }

        return (
          <ContentCell
            className={contentCellGroup()}
            key={table.id}
            ref={registerSection(String(idx))}
          >
            <div className={sectionHeading()}>
              <H3>{table.label}</H3>
            </div>
            <TableView
              node={
                node as ConceptQueryNodeType /* otherwise there won't be tables */
              }
              tableIdx={idx}
              allowlistedSelects={allowlistedSelects}
              blocklistedSelects={blocklistedSelects}
              onSelectTableSelects={onSelectTableSelects}
              onSetDateColumn={onSetDateColumn}
              onSetFilterValue={onSetFilterValue}
              onLoadFilterSuggestions={onLoadFilterSuggestions}
            />
          </ContentCell>
        );
      })}
    </div>
  );
};

export default ContentColumn;
