import styled from "@emotion/styled";
import { FC, useEffect, useRef } from "react";
import { useTranslation } from "react-i18next";

import type { PostPrefixForSuggestionsParams } from "../api/api";
import type {
  PostFilterSuggestionsResponseT,
  SelectOptionT,
  SelectorResultType,
} from "../api/types";
import { Heading3 } from "../headings/Headings";
import { nodeIsConceptQueryNode } from "../model/node";
import {
  ConceptQueryNodeType,
  FilterWithValueType,
  StandardQueryNodeT,
} from "../standard-query-editor/types";
import type { ModeT } from "../ui-components/InputRange";

import CommonNodeSettings from "./CommonNodeSettings";
import ContentCell from "./ContentCell";
import NodeSelects from "./NodeSelects";
import TableView from "./TableView";

const Column = styled("div")`
  width: 100%;
  display: flex;
  flex-direction: column;
`;

const SectionHeading = styled(Heading3)`
  margin: 10px 10px 0;
`;

const ContentCellGroup = styled(ContentCell)`
  padding-bottom: 10px;
  margin-bottom: 10px;
  border-bottom: 1px solid ${({ theme }) => theme.col.grayLight};

  &:last-of-type {
    border-bottom: none;
    padding-bottom: 0;
    margin-bottom: 0;
  }
`;

interface PropsT {
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
}

const ContentColumn: FC<PropsT> = ({
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
}) => {
  const { t } = useTranslation();

  const tables = nodeIsConceptQueryNode(node) ? node.tables : [];

  const itemsRef = useRef<(HTMLDivElement | null)[]>(new Array(tables.length));

  useEffect(() => {
    if (
      selectedTableIdx !== null &&
      itemsRef.current &&
      itemsRef.current[selectedTableIdx]
    ) {
      itemsRef.current[selectedTableIdx]?.scrollIntoView({
        block: "start",
        inline: "start",
        behavior: "smooth",
      });
    }
  }, [selectedTableIdx]);

  return (
    <Column>
      <ContentCellGroup>
        <SectionHeading>{t("queryNodeEditor.properties")}</SectionHeading>
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
      </ContentCellGroup>
      {tables.map((table, idx) => {
        if (table.exclude) {
          return null;
        }

        return (
          <ContentCellGroup
            key={table.id}
            ref={(instance) => (itemsRef.current[idx] = instance)}
          >
            <SectionHeading>{table.label}</SectionHeading>
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
          </ContentCellGroup>
        );
      })}
    </Column>
  );
};

export default ContentColumn;
