import { useEffect, useRef } from "react";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";

import type { PostPrefixForSuggestionsParams } from "../api/api";
import type {
  PostFilterSuggestionsResponseT,
  SelectOptionT,
  SelectorResultType,
} from "../api/types";
import { Heading3 } from "../headings/Headings";
import { nodeIsConceptQueryNode } from "../model/node";
import type {
  ConceptQueryNodeType,
  FilterWithValueType,
  StandardQueryNodeT,
} from "../standard-query-editor/types";
import type { ModeT } from "../ui-components/InputRange";

import CommonNodeSettings from "./CommonNodeSettings";
import ContentCell from "./ContentCell";
import NodeSelects from "./NodeSelects";
import TableView from "./TableView";

// mb-0 overrides the h3 base margin from index.css
const sectionHeading = tv({
  base: ["mx-[10px] mt-[10px] mb-0"],
});

const contentCellGroup = tv({
  base: [
    "pb-[10px]",
    "mb-[10px]",
    "border-b border-gray-100",
    "last-of-type:border-b-0 last-of-type:pb-0 last-of-type:mb-0",
  ],
});

const ContentColumn = ({
  node,
  selectedTableIdx,
  blocklistedSelects,
  allowlistedSelects,
  onLoadFilterSuggestions,
  onSetDateColumn,
  onSetFilterValue,
  onSwitchFilterMode,
  onSelectSelects,
  onSelectTableSelects,
  onToggleTimestamps,
  onToggleSecondaryIdExclude,
}: {
  node: StandardQueryNodeT;
  selectedTableIdx: number | null;
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
  onSwitchFilterMode: (
    tableIdx: number,
    filterIdx: number,
    mode: ModeT,
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

  const itemsRef = useRef<(HTMLDivElement | null)[]>(new Array(tables.length));

  useEffect(() => {
    if (selectedTableIdx && itemsRef.current?.[selectedTableIdx]) {
      itemsRef.current[selectedTableIdx]?.scrollIntoView({
        block: "start",
        inline: "start",
        behavior: "smooth",
      });
    }
  }, [selectedTableIdx]);

  return (
    <div className="flex w-full flex-col">
      <ContentCell className={contentCellGroup()}>
        <Heading3 className={sectionHeading()}>
          {t("queryNodeEditor.properties")}
        </Heading3>
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
            ref={(instance) => {
              itemsRef.current[idx] = instance;
            }}
          >
            <Heading3 className={sectionHeading()}>{table.label}</Heading3>
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
              onSwitchFilterMode={onSwitchFilterMode}
              onLoadFilterSuggestions={onLoadFilterSuggestions}
            />
          </ContentCell>
        );
      })}
    </div>
  );
};

export default ContentColumn;
