import { css } from "@emotion/react";
import styled from "@emotion/styled";
import type { StateT } from "app-types";
import { parseISO } from "date-fns";
import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";

import type { DatasetIdT, SecondaryId } from "../../api/types";
import DownloadButton from "../../button/DownloadButton";
import IconButton from "../../button/IconButton";
import { formatDate } from "../../common/helpers";
import { exists } from "../../common/helpers/exists";
import ErrorMessage from "../../error-message/ErrorMessage";
import FaIcon from "../../icon/FaIcon";
import WithTooltip from "../../tooltip/WithTooltip";

import PreviousQueriesLabel from "./PreviousQueriesLabel";
import { useRemoveQuery, useRenameQuery } from "./actions";
import { PreviousQueryT } from "./reducer";

const Root = styled("div")<{ own?: boolean; system?: boolean }>`
  margin: 0;
  padding: 4px 10px;
  cursor: pointer;
  border-radius: ${({ theme }) => theme.borderRadius};
  border: 1px solid ${({ theme }) => theme.col.grayLight};
  background-color: ${({ theme }) => theme.col.bg};
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.2);
  overflow: hidden;

  border-left: ${({ theme, own, system }) =>
    own
      ? `4px solid ${theme.col.orange}`
      : system
      ? `4px solid ${theme.col.blueGrayDark}`
      : `1px solid ${theme.col.grayLight}`};

  &:hover {
    ${({ theme, own, system }) =>
      !own &&
      !system &&
      css`
        border-left-color: ${theme.col.blueGray};
      `};
    border-top-color: ${({ theme }) => theme.col.blueGray};
    border-right-color: ${({ theme }) => theme.col.blueGray};
    border-bottom-color: ${({ theme }) => theme.col.blueGray};
  }
`;

const Gray = styled("div")`
  color: ${({ theme }) => theme.col.gray};
  font-size: ${({ theme }) => theme.font.xs};
`;
const TopInfos = styled(Gray)`
  line-height: 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
`;
const OwnerName = styled(Gray)`
  flex-shrink: 0;
  padding-left: 5px;
`;

const TopRight = styled("div")`
  display: flex;
  align-items: center;
  flex-shrink: 0;
`;
const TopLeft = styled("div")`
  display: flex;
  align-items: center;
`;

const NonBreakingText = styled("span")`
  white-space: nowrap;
`;
const LabelRow = styled("div")`
  display: flex;
  width: 100%;
  justify-content: space-between;
  line-height: 24px;
  margin: 2px 0;
`;
const StyledErrorMessage = styled(ErrorMessage)`
  margin: 0;
`;

const StyledFaIcon = styled(FaIcon)`
  margin: 0 6px;
`;

const StyledWithTooltip = styled(WithTooltip)`
  margin-left: 10px;
`;

const SxDownloadButton = styled(DownloadButton)`
  white-space: nowrap;
  button {
    font-size: ${({ theme }) => theme.font.xs};
  }
`;

const FoldersButton = styled(IconButton)`
  margin-right: 10px;
`;

interface PropsT {
  query: PreviousQueryT;
  datasetId: DatasetIdT;
  onIndicateDeletion: () => void;
  onIndicateShare: () => void;
  onIndicateEditFolders: () => void;
}

const PreviousQuery = React.forwardRef<HTMLDivElement, PropsT>(
  function PreviousQueryComponent(
    {
      query,
      datasetId,
      onIndicateDeletion,
      onIndicateShare,
      onIndicateEditFolders,
    },
    ref,
  ) {
    const { t } = useTranslation();

    const loadedSecondaryIds = useSelector<StateT, SecondaryId[]>(
      (state) => state.conceptTrees.secondaryIds,
    );

    const renameQuery = useRenameQuery();
    const removeQuery = useRemoveQuery(query.id);

    const mayDeleteQueryRightAway =
      query.tags.length === 0 && query.isPristineLabel;
    const onDeleteClick = mayDeleteQueryRightAway
      ? removeQuery
      : onIndicateDeletion;

    const peopleFoundText = exists(query.numberOfResults)
      ? `${query.numberOfResults} ${t("previousQueries.results")}`
      : t("previousQuery.notExecuted");

    const dateFormat = `${t("inputDateRange.dateFormat")} HH:mm`;
    const executedAtDate = parseISO(query.createdAt);
    const executedAt = formatDate(executedAtDate, dateFormat);

    const isShared = query.shared || (query.groups && query.groups.length > 0);
    const label = query.label || query.id.toString();
    const mayEditQuery = query.own || isShared;

    const secondaryId = query.secondaryId
      ? loadedSecondaryIds.find((secId) => query.secondaryId === secId.id)
      : null;

    const [isEditingLabel, setIsEditingLabel] = useState<boolean>(false);

    return (
      <Root
        ref={ref}
        own={!!query.own}
        system={!!query.system || (!query.own && !isShared)}
      >
        <TopInfos>
          <TopLeft>
            {mayEditQuery && (
              <WithTooltip text={t("previousQuery.editFolders")}>
                <FoldersButton
                  icon="folder"
                  tight
                  small
                  bare
                  onClick={onIndicateEditFolders}
                />
              </WithTooltip>
            )}
            {query.resultUrls.length > 0 ? (
              <WithTooltip text={t("previousQuery.downloadResults")}>
                <SxDownloadButton tight small bare url={query.resultUrls[0]}>
                  {peopleFoundText}
                </SxDownloadButton>
              </WithTooltip>
            ) : (
              <NonBreakingText>{peopleFoundText}</NonBreakingText>
            )}
          </TopLeft>
          <TopRight>
            {executedAt}
            {secondaryId && query.queryType === "SECONDARY_ID_QUERY" && (
              <StyledWithTooltip
                text={`${t("queryEditor.secondaryId")}: ${secondaryId.label}`}
              >
                <IconButton icon="microscope" bare onClick={() => {}} />
              </StyledWithTooltip>
            )}
            {query.own && (
              <StyledWithTooltip
                text={isShared ? t("common.shared") : t("common.share")}
              >
                <IconButton
                  icon={isShared ? "user-friends" : "upload"}
                  bare
                  onClick={onIndicateShare}
                />
              </StyledWithTooltip>
            )}
            {query.loading ? (
              <StyledFaIcon icon="spinner" />
            ) : (
              query.own && (
                <StyledWithTooltip text={t("common.delete")}>
                  <IconButton icon="times" bare onClick={onDeleteClick} />
                </StyledWithTooltip>
              )
            )}
          </TopRight>
        </TopInfos>
        <LabelRow>
          <PreviousQueriesLabel
            mayEditQuery={mayEditQuery}
            loading={!!query.loading}
            label={label}
            selectTextOnMount={true}
            onSubmit={(label) => renameQuery(datasetId, query.id, label)}
            isEditing={isEditingLabel}
            setIsEditing={setIsEditingLabel}
          />
          <OwnerName>{query.ownerName}</OwnerName>
        </LabelRow>
        {!!query.error && <StyledErrorMessage message={query.error} />}
      </Root>
    );
  },
);

export default PreviousQuery;
