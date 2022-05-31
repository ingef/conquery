import styled from "@emotion/styled";
import { useCallback, useMemo } from "react";
import { useHotkeys } from "react-hotkeys-hook";
import { useTranslation } from "react-i18next";
import ReactList from "react-list";
import { useDispatch } from "react-redux";

import { SelectOptionT } from "../api/types";
import IconButton from "../button/IconButton";
import { downloadBlob } from "../common/helpers/downloadBlob";
import WithTooltip from "../tooltip/WithTooltip";

import { NavigationHeader } from "./NavigationHeader";
import { closeHistory, useUpdateHistorySession } from "./actions";

const Root = styled("div")`
  display: grid;
  gap: 10px;
  overflow: hidden;
  background-color: ${({ theme }) => theme.col.bg};
`;

const EntityIdNav = styled("div")`
  display: grid;
  grid-template-rows: auto 12fr auto auto;
  overflow: hidden;
  padding: 0 10px 0 20px;
`;
const TopActions = styled("div")`
  display: flex;
`;

const SxNavigationHeader = styled(NavigationHeader)`
  padding: 0 10px 0 20px;
`;

const Middle = styled("div")`
  height: 100%;
  overflow-y: auto;
  border-radius: ${({ theme }) => theme.borderRadius};
  border: 1px solid ${({ theme }) => theme.col.grayLight};
  padding: 3px;
`;
const BottomActions = styled("div")`
  display: flex;
`;

const BackButton = styled(IconButton)`
  margin: 0 10px 0 20px;
`;

const SxIconButton = styled(IconButton)`
  width: 100%;
`;

const SxWithTooltip = styled(WithTooltip)`
  color: black;
  flex-shrink: 0;
  width: 100%;
`;

const Row = styled("div")<{ active?: boolean }>`
  padding: 1px 3px;
  font-size: ${({ theme }) => theme.font.xs};
  display: flex;
  align-items: center;
  background-color: ${({ active, theme }) =>
    active ? theme.col.blueGrayVeryLight : "white"};
  height: 24px;
  cursor: pointer;
  gap: 3px;

  &:hover {
    background-color: ${({ active, theme }) =>
      active ? theme.col.blueGrayVeryLight : theme.col.grayVeryLight};
  }
`;

const Statuses = styled("div")`
  display: flex;
  align-items: center;
  gap: 2px;
  margin-left: auto;
`;
const EntityStatus = styled("div")`
  border-radius: ${({ theme }) => theme.borderRadius};
  border: 2px solid ${({ theme }) => theme.col.blueGrayDark};
  background-color: white;
  padding: 1px 4px;
  font-size: ${({ theme }) => theme.font.xs};
  color: ${({ theme }) => theme.col.blueGrayDark};
  font-weight: 700;
`;
const EntityId = styled("div")<{ active?: boolean }>`
  font-weight: 700;
`;
const Number = styled("div")`
  font-size: ${({ theme }) => theme.font.xs};
  color: ${({ theme }) => theme.col.gray};
  flex-shrink: 0;
`;

interface Props {
  className?: string;
  entityIds: string[];
  entityIdsStatus: { [entityId: string]: SelectOptionT[] };
  currentEntityId: string | null;
  currentEntityIndex: number;
}

export const Navigation = ({
  className,
  entityIds,
  entityIdsStatus,
  currentEntityId,
  currentEntityIndex,
}: Props) => {
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const updateHistorySession = useUpdateHistorySession();
  const onCloseHistory = useCallback(() => {
    dispatch(closeHistory());
  }, [dispatch]);

  const goToPrev = useCallback(() => {
    const prevIdx = Math.max(0, currentEntityIndex - 1);

    updateHistorySession({ entityId: entityIds[prevIdx], years: [] });
  }, [entityIds, currentEntityIndex]);
  const goToNext = useCallback(() => {
    const nextIdx = Math.min(entityIds.length - 1, currentEntityIndex + 1);

    updateHistorySession({ entityId: entityIds[nextIdx], years: [] });
  }, [entityIds, currentEntityIndex]);

  useHotkeys("shift+up", goToPrev, [goToPrev]);
  useHotkeys("shift+down", goToNext, [goToNext]);

  const numberWidth = useMemo(() => {
    const magnitude = Math.ceil(Math.log(entityIds.length) / Math.log(10));

    return 15 + 6 * magnitude;
  }, [entityIds.length]);

  const renderItem = (index: number) => {
    const entityId = entityIds[index];

    return (
      <Row
        key={entityId}
        active={entityId === currentEntityId}
        className="scrollable-list-item"
        onClick={() => updateHistorySession({ entityId, years: [] })}
      >
        <Number style={{ width: numberWidth }}>#{index + 1}</Number>
        <EntityId>{entityId}</EntityId>
        <Statuses>
          {entityIdsStatus[entityId] &&
            entityIdsStatus[entityId].map((val) => (
              <EntityStatus key={val.value}>{val.label}</EntityStatus>
            ))}
        </Statuses>
      </Row>
    );
  };

  const markedCount = useMemo(
    () => Object.values(entityIdsStatus).filter((v) => v.length > 0).length,
    [entityIdsStatus],
  );

  return (
    <Root className={className}>
      <BackButton frame icon="chevron-left" onClick={onCloseHistory}>
        {t("common.back")}
      </BackButton>
      <SxNavigationHeader
        markedCount={markedCount}
        idsCount={entityIds.length}
      />
      <EntityIdNav>
        <TopActions>
          <SxWithTooltip
            text={`${t("history.prevButtonLabel")} (shift + ⬆)`}
            lazy
          >
            <SxIconButton icon="arrow-up" onClick={goToPrev} />
          </SxWithTooltip>
        </TopActions>
        <Middle>
          <ReactList
            itemRenderer={renderItem}
            length={entityIds.length}
            type="uniform"
          />
        </Middle>
        <BottomActions>
          <SxWithTooltip
            text={`${t("history.nextButtonLabel")} (shift + ⬇)`}
            lazy
          >
            <SxIconButton icon="arrow-down" onClick={goToNext} />
          </SxWithTooltip>
        </BottomActions>
        <BottomActions style={{ marginTop: "10px" }}>
          <SxWithTooltip text={t("history.downloadButtonLabel")}>
            <SxIconButton
              style={{ backgroundColor: "white" }}
              frame
              icon="download"
              onClick={() => {
                const idToRow = (id: string) => [
                  id,
                  entityIdsStatus[id]
                    ? entityIdsStatus[id].map((o) => o.value)
                    : "",
                ];

                const csvString = entityIds
                  .map(idToRow)
                  .map((row) => row.join(";"))
                  .join("\n");

                const blob = new Blob([csvString], { type: "application/csv" });

                downloadBlob(blob, "list.csv");
              }}
            >
              CSV
            </SxIconButton>
          </SxWithTooltip>
        </BottomActions>
      </EntityIdNav>
    </Root>
  );
};
