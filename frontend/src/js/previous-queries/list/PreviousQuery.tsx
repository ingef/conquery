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
import { formatDate, useFormatDateDistance } from "../../common/helpers";
import { exists } from "../../common/helpers/exists";
import ErrorMessage from "../../error-message/ErrorMessage";
import EditableTags from "../../form-components/EditableTags";
import FaIcon from "../../icon/FaIcon";
import WithTooltip from "../../tooltip/WithTooltip";

import PreviousQueriesLabel from "./PreviousQueriesLabel";
import PreviousQueryTags from "./PreviousQueryTags";
import { useRenamePreviousQuery, useRetagPreviousQuery } from "./actions";
import { PreviousQueryT } from "./reducer";
import { useDeletePreviousQuery } from "./useDeletePreviousQuery";

const Root = styled("div")<{ own?: boolean; system?: boolean }>`
  margin: 0;
  padding: 4px 10px;
  cursor: pointer;
  border-radius: ${({ theme }) => theme.borderRadius};
  border: 1px solid ${({ theme }) => theme.col.grayLight};
  background-color: ${({ theme }) => theme.col.bg};
  box-shadow: 0 0 2px 0 rgba(0, 0, 0, 0.2);

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
const MiddleRow = styled("div")`
  display: flex;
  width: 100%;
  justify-content: space-between;
  line-height: 24px;
  margin-bottom: 2px;
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

const SxPreviousQueryTags = styled(PreviousQueryTags)`
  display: flex;
  align-items: center;
`;

interface PropsT {
  query: PreviousQueryT;
  datasetId: DatasetIdT;
  onIndicateDeletion: () => void;
  onIndicateShare: () => void;
}

const PreviousQuery = React.forwardRef<HTMLDivElement, PropsT>(
  function PreviousQueryComponent(
    { query, datasetId, onIndicateDeletion, onIndicateShare },
    ref,
  ) {
    const { t } = useTranslation();
    const availableTags = useSelector<StateT, string[]>(
      (state) => state.previousQueries.tags,
    );

    const loadedSecondaryIds = useSelector<StateT, SecondaryId[]>(
      (state) => state.conceptTrees.secondaryIds,
    );

    const formatDateDistance = useFormatDateDistance();

    const renamePreviousQuery = useRenamePreviousQuery();
    const retagPreviousQuery = useRetagPreviousQuery();
    const onDeletePreviousQuery = useDeletePreviousQuery(query.id);

    const onRenamePreviousQuery = (label: string) =>
      renamePreviousQuery(datasetId, query.id, label);

    const onRetagPreviousQuery = (tags: string[]) =>
      retagPreviousQuery(query.id, tags);

    const mayDeleteQueryRightAway =
      query.tags.length === 0 && query.isPristineLabel;
    const onDeleteClick = mayDeleteQueryRightAway
      ? onDeletePreviousQuery
      : onIndicateDeletion;

    const peopleFoundText = exists(query.numberOfResults)
      ? `${query.numberOfResults} ${t("previousQueries.results")}`
      : t("previousQuery.notExecuted");

    const dateFormat = `${t("inputDateRange.dateFormat")} HH:mm`;
    const executedAtDate = parseISO(query.createdAt);
    const executedAt = formatDate(executedAtDate, dateFormat);
    const executedAtRelative = formatDateDistance(
      executedAtDate,
      new Date(),
      true,
    );
    const isShared = query.shared || (query.groups && query.groups.length > 0);
    const label = query.label || query.id.toString();
    const mayEditQuery = query.own || isShared;

    const secondaryId = query.secondaryId
      ? loadedSecondaryIds.find((secId) => query.secondaryId === secId.id)
      : null;

    const [isEditingTags, setIsEditingTags] = useState<boolean>(false);
    const [isEditingLabel, setIsEditingLabel] = useState<boolean>(false);

    return (
      <Root
        ref={ref}
        own={!!query.own}
        system={!!query.system || (!query.own && !isShared)}
      >
        <TopInfos>
          <TopLeft>
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
            <WithTooltip text={executedAtRelative}>{executedAt}</WithTooltip>
            {mayEditQuery &&
              !isEditingTags &&
              (!query.tags || query.tags.length === 0) && (
                <StyledWithTooltip text={t("common.addTag")}>
                  <IconButton
                    icon="tags"
                    bare
                    onClick={() => setIsEditingTags(true)}
                  />
                </StyledWithTooltip>
              )}
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
        <MiddleRow>
          <PreviousQueriesLabel
            mayEditQuery={mayEditQuery}
            loading={!!query.loading}
            label={label}
            selectTextOnMount={true}
            onSubmit={onRenamePreviousQuery}
            isEditing={isEditingLabel}
            setIsEditing={setIsEditingLabel}
          />
          <OwnerName>{query.ownerName}</OwnerName>
        </MiddleRow>
        {mayEditQuery ? (
          <EditableTags
            tags={query.tags.sort()}
            loading={!!query.loading}
            onSubmit={onRetagPreviousQuery}
            isEditing={isEditingTags}
            setIsEditing={setIsEditingTags}
            tagComponent={<SxPreviousQueryTags tags={query.tags} />}
            availableTags={availableTags}
          />
        ) : (
          <SxPreviousQueryTags tags={query.tags} />
        )}
        {!!query.error && <StyledErrorMessage message={query.error} />}
      </Root>
    );
  },
);

export default PreviousQuery;
