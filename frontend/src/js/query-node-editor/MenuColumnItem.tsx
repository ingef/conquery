import styled from "@emotion/styled";
import type { FC } from "react";
import { useTranslation } from "react-i18next";

import IconButton from "../button/IconButton";
import type { NodeResetConfig } from "../model/node";
import { tableHasNonDefaultSettings, tableIsDisabled } from "../model/table";
import type { TableWithFilterValueT } from "../standard-query-editor/types";
import WithTooltip from "../tooltip/WithTooltip";

const Container = styled("div")<{ disabled?: boolean }>`
  font-size: ${({ theme }) => theme.font.md};
  line-height: 21px;
  padding: 8px 15px;
  font-weight: 700;
  color: ${({ theme, disabled }) =>
    disabled ? theme.col.gray : theme.col.black};
  width: 100%;
  text-align: left;
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
  background-color: transparent;
  cursor: pointer;

  &:hover {
    text-decoration: underline;
  }
`;

const SxWithTooltip = styled(WithTooltip)`
  display: flex !important;
`;

const SxIconButton = styled(IconButton)`
  font-size: ${({ theme }) => theme.font.lg};
  line-height: ${({ theme }) => theme.font.lg};
  padding: 0;

  svg {
    font-size: ${({ theme }) => theme.font.lg};
    line-height: ${({ theme }) => theme.font.lg};
  }
`;
const ResetButton = styled(IconButton)`
  padding: 0;
`;

const Label = styled("span")`
  padding-left: 10px;
  line-height: ${({ theme }) => theme.font.lg};
`;

const Row = styled("div")`
  display: flex;
  align-items: center;
`;

interface PropsT {
  table: TableWithFilterValueT;
  isActive: boolean;
  isOnlyOneTableIncluded: boolean;
  blocklistedTables?: string[];
  allowlistedTables?: string[];
  onClick: () => void;
  onToggleTable: (value: boolean) => void;
  onResetTable: (config: NodeResetConfig) => void;
}

const MenuColumnItem: FC<PropsT> = ({
  table,
  isOnlyOneTableIncluded,
  blocklistedTables,
  allowlistedTables,
  onClick,
  onToggleTable,
  onResetTable,
}) => {
  const { t } = useTranslation();
  const isDisabled = tableIsDisabled(
    table,
    blocklistedTables,
    allowlistedTables,
  );

  const includable = table.exclude;
  const excludable = !isOnlyOneTableIncluded;

  const isFilterActive = tableHasNonDefaultSettings(table);

  return (
    <Container disabled={isDisabled} onClick={onClick}>
      <Row>
        <SxIconButton
          regular
          icon={includable ? "square" : "check-square"}
          disabled={isDisabled || (!includable && !excludable)}
          onClick={(event) => {
            // To prevent selecting the table as well, see above
            event.stopPropagation();

            if (isDisabled) {
              return;
            }

            if (includable || excludable) {
              onToggleTable(!table.exclude);
            }
          }}
        />
        <Label>{table.label}</Label>
      </Row>
      {isFilterActive && (
        <SxWithTooltip text={t("queryNodeEditor.resetSettings")}>
          <ResetButton
            secondary
            icon="undo"
            onClick={(event) => {
              // To prevent selecting the table as well, see above
              event.stopPropagation();

              if (isDisabled) {
                return;
              }

              onResetTable({ useDefaults: true });
            }}
          />
        </SxWithTooltip>
      )}
    </Container>
  );
};

export default MenuColumnItem;
