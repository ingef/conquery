import styled from "@emotion/styled";
import React, { FC, useEffect, useRef } from "react";
import { useTranslation } from "react-i18next";

import type { PostPrefixForSuggestionsParams } from "../api/api";
import { CurrencyConfigT, DatasetIdT, SelectOptionT } from "../api/types";
import { Heading3 } from "../headings/Headings";
import { nodeIsConceptQueryNode } from "../model/node";
import { sortSelects } from "../model/select";
import {
  ConceptQueryNodeType,
  StandardQueryNodeT,
} from "../standard-query-editor/types";
import InputCheckbox from "../ui-components/InputCheckbox";
import InputMultiSelect from "../ui-components/InputMultiSelect";
import type { ModeT } from "../ui-components/InputRange";

import ContentCell from "./ContentCell";
import TableView from "./TableView";

const Column = styled("div")`
  width: 100%;
  display: flex;
  flex-direction: column;
`;

const Row = styled("div")`
  max-width: 300px;
  margin-bottom: 10px;
`;

const SectionHeading = styled(Heading3)`
  margin: 10px 10px 0;
`;

const CommonSettingsContainer = styled("div")`
  margin: 15px 10px;
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
  datasetId: DatasetIdT;
  currencyConfig: CurrencyConfigT;
  selectedTableIdx: number | null;
  onShowDescription: (filterIdx: number) => void;
  onSelectSelects: (value: SelectOptionT[]) => void;
  onSelectTableSelects: (tableIdx: number, value: SelectOptionT[]) => void;
  onToggleTimestamps?: () => void;
  onToggleSecondaryIdExclude?: () => void;
  onSetFilterValue: (tableIdx: number, filterIdx: number, value: any) => void;
  onSwitchFilterMode: (
    tableIdx: number,
    filterIdx: number,
    mode: ModeT,
  ) => void;
  onLoadFilterSuggestions: (
    params: PostPrefixForSuggestionsParams,
    tableIdx: number,
    filterIdx: number,
  ) => void;
  onSetDateColumn: (tableIdx: number, value: string | null) => void;
}

const ContentColumn: FC<PropsT> = ({
  node,
  datasetId,
  currencyConfig,
  selectedTableIdx,
  onLoadFilterSuggestions,
  onSetDateColumn,
  onSetFilterValue,
  onSwitchFilterMode,
  onShowDescription,
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
        <CommonSettingsContainer>
          {onToggleTimestamps && (
            <Row>
              <InputCheckbox
                label={t("queryNodeEditor.excludeTimestamps")}
                tooltip={t("help.excludeTimestamps")}
                tooltipLazy
                input={{
                  value: node.excludeTimestamps,
                  onChange: () => onToggleTimestamps(),
                }}
              />
            </Row>
          )}
          {onToggleSecondaryIdExclude && (
            <Row>
              <InputCheckbox
                label={t("queryNodeEditor.excludeFromSecondaryIdQuery")}
                tooltip={t("help.excludeFromSecondaryIdQuery")}
                tooltipLazy
                input={{
                  value: node.excludeFromSecondaryIdQuery,
                  onChange: () => onToggleSecondaryIdExclude(),
                }}
              />
            </Row>
          )}
        </CommonSettingsContainer>
        {nodeIsConceptQueryNode(node) && node.selects && (
          <ContentCell headline={t("queryNodeEditor.commonSelects")}>
            <InputMultiSelect
              input={{
                onChange: onSelectSelects,
                value: node.selects
                  .filter(({ selected }) => !!selected)
                  .map(({ id, label }) => ({ value: id, label: label })),
              }}
              options={sortSelects(node.selects).map((select) => ({
                value: select.id,
                label: select.label,
              }))}
            />
          </ContentCell>
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
              datasetId={datasetId}
              currencyConfig={currencyConfig}
              tableIdx={idx}
              onShowDescription={onShowDescription}
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
