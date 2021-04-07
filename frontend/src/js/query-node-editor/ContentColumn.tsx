import React, { FC, useEffect, useRef } from "react";
import styled from "@emotion/styled";
import { useTranslation } from "react-i18next";

import {
  ConceptQueryNodeType,
  StandardQueryNodeT,
} from "../standard-query-editor/types";
import InputCheckbox from "../form-components/InputCheckbox";
import { isConceptQueryNode } from "../model/query";
import InputMultiSelect from "../form-components/InputMultiSelect";
import { CurrencyConfigT, DatasetIdT, SelectOptionT } from "../api/types";
import { sortSelects } from "../model/select";
import type { ModeT } from "../form-components/InputRange";
import type { PostPrefixForSuggestionsParams } from "../api/api";
import { Heading3 } from "../headings/Headings";

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
    mode: ModeT
  ) => void;
  onLoadFilterSuggestions: (
    params: PostPrefixForSuggestionsParams,
    tableIdx: number,
    filterIdx: number
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

  const tables = isConceptQueryNode(node) ? node.tables : [];

  const itemsRef = useRef<(HTMLDivElement | null)[]>(new Array(tables.length));

  useEffect(() => {
    if (
      selectedTableIdx !== null &&
      itemsRef.current &&
      itemsRef.current[selectedTableIdx]
    ) {
      itemsRef.current[selectedTableIdx]?.scrollIntoView({
        behavior: "smooth",
      });
    }
  }, [selectedTableIdx]);

  return (
    <Column>
      <ContentCell>
        <SectionHeading>{t("queryNodeEditor.properties")}</SectionHeading>
        <CommonSettingsContainer>
          {onToggleTimestamps && (
            <Row>
              <InputCheckbox
                label={t("queryNodeEditor.excludeTimestamps")}
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
                input={{
                  value: node.excludeFromSecondaryIdQuery,
                  onChange: () => onToggleSecondaryIdExclude(),
                }}
              />
            </Row>
          )}
        </CommonSettingsContainer>
        {node.selects && (
          <ContentCell headline={t("queryNodeEditor.selects")}>
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
      </ContentCell>
      {tables.map((table, idx) => {
        if (table.exclude) {
          return null;
        }

        return (
          <ContentCell ref={(instance) => (itemsRef.current[idx] = instance)}>
            <SectionHeading>{table.label}</SectionHeading>
            <TableView
              node={
                node as ConceptQueryNodeType /* otherwise there won't be tables */
              }
              datasetId={datasetId}
              currencyConfig={currencyConfig}
              selectedInputTableIdx={idx}
              onShowDescription={onShowDescription}
              onSelectTableSelects={onSelectTableSelects}
              onSetDateColumn={onSetDateColumn}
              onSetFilterValue={onSetFilterValue}
              onSwitchFilterMode={onSwitchFilterMode}
              onLoadFilterSuggestions={onLoadFilterSuggestions}
            />
          </ContentCell>
        );
      })}
    </Column>
  );
};

export default ContentColumn;
