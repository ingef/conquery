import styled from "@emotion/styled";
import { FC } from "react";
import { useTranslation } from "react-i18next";

import type { ConceptIdT } from "../api/types";
import { getConceptById } from "../concept-trees/globalTreeStoreHelper";
import { Heading3, Heading4 } from "../headings/Headings";
import type { NodeResetConfig } from "../model/node";
import type {
  DragItemConceptTreeNode,
  StandardQueryNodeT,
} from "../standard-query-editor/types";

import ConceptDropzone from "./ConceptDropzone";
import ConceptEntry from "./ConceptEntry";
import MenuColumnItem from "./MenuColumnItem";

const FixedColumn = styled("div")<{ isEmpty?: boolean }>`
  display: flex;
  flex-direction: column;
  width: ${({ isEmpty }) => (isEmpty ? "200px" : "270px")};
  overflow: hidden;
  flex-shrink: 0;
  flex-grow: 1;

  &:first-of-type {
    border-right: 1px solid ${({ theme }) => theme.col.grayLight};
  }
`;

const Padded = styled("div")`
  padding: 0 15px 15px;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
`;

const HeadingBetween = styled(Heading4)`
  margin: 15px 15px 0;
`;
const Heading4Highlighted = styled(Heading4)`
  color: ${({ theme }) => theme.col.blueGrayDark};
  font-weight: 700;
  margin: 10px 0 5px;
`;

const DimmedNote = styled(Heading3)`
  color: ${({ theme }) => theme.col.grayLight};
  padding: 15px;
  font-weight: 400;
`;

const CommonSettingsLabel = styled(Heading3)`
  padding: 15px 15px 0;
  margin: 0;
  cursor: pointer;
  &:hover {
    text-decoration: underline;
  }
`;

interface PropsT {
  className?: string;

  node: StandardQueryNodeT;
  selectedTableIdx: number | null;
  showTables: boolean;
  allowlistedTables?: string[];
  blocklistedTables?: string[];

  onCommonSettingsClick: () => void;
  onDropConcept: (node: DragItemConceptTreeNode) => void;
  onRemoveConcept: (conceptId: ConceptIdT) => void;
  onToggleTable: (tableIdx: number, isExcluded: boolean) => void;
  onSelectTable: (tableIdx: number) => void;
  onResetTable: (tableIdx: number, config: NodeResetConfig) => void;
}

const MenuColumn: FC<PropsT> = ({
  className,
  node,
  selectedTableIdx,
  showTables,
  blocklistedTables,
  allowlistedTables,
  onCommonSettingsClick,
  onDropConcept,
  onRemoveConcept,
  onToggleTable,
  onSelectTable,
  onResetTable,
}) => {
  const { t } = useTranslation();
  const isOnlyOneTableIncluded =
    !node.isPreviousQuery &&
    node.tables.filter((table) => !table.exclude).length === 1;

  const rootConcept = !node.isPreviousQuery ? getConceptById(node.tree) : null;

  const isEmpty =
    node.isPreviousQuery ||
    (!showTables &&
      (!rootConcept ||
        !rootConcept.children ||
        rootConcept.children.length === 0));

  return (
    <FixedColumn className={className} isEmpty={isEmpty}>
      {isEmpty && (
        <DimmedNote>{t("queryNodeEditor.emptyMenuColumn")}</DimmedNote>
      )}
      {!node.isPreviousQuery && showTables && (
        <div>
          <CommonSettingsLabel onClick={onCommonSettingsClick}>
            {t("queryNodeEditor.properties")}
          </CommonSettingsLabel>
          <HeadingBetween>
            {t("queryNodeEditor.conceptNodeTables")}
          </HeadingBetween>
          {node.tables.map((table, tableIdx) => (
            <MenuColumnItem
              key={tableIdx}
              table={table}
              isActive={selectedTableIdx === tableIdx}
              isOnlyOneTableIncluded={isOnlyOneTableIncluded}
              blocklistedTables={blocklistedTables}
              allowlistedTables={allowlistedTables}
              onClick={() => {
                if (!table.exclude) {
                  onSelectTable(tableIdx);
                }
              }}
              onToggleTable={(value) => onToggleTable(tableIdx, value)}
              onResetTable={(config: NodeResetConfig) =>
                onResetTable(tableIdx, config)
              }
            />
          ))}
        </div>
      )}
      {!node.isPreviousQuery &&
        rootConcept &&
        rootConcept.children &&
        rootConcept.children.length > 0 && (
          <>
            <HeadingBetween>
              {t("queryNodeEditor.dropMoreConcepts")}
            </HeadingBetween>
            <Padded>
              <Heading4Highlighted>{rootConcept.label}</Heading4Highlighted>
              <div>
                <ConceptDropzone node={node} onDropConcept={onDropConcept} />
              </div>
              <div>
                {node.ids.map((conceptId) => (
                  <ConceptEntry
                    key={conceptId}
                    node={getConceptById(conceptId)}
                    conceptId={conceptId}
                    canRemoveConcepts={node.ids.length > 1}
                    onRemoveConcept={onRemoveConcept}
                  />
                ))}
              </div>
            </Padded>
          </>
        )}
    </FixedColumn>
  );
};

export default MenuColumn;
