import React from "react";
import { useSelector, useDispatch } from "react-redux";
import styled from "@emotion/styled";
import { css } from "@emotion/react";

import T from "i18n-react";
import { parseISO } from "date-fns";

import ErrorMessage from "../../error-message/ErrorMessage";
import { isEmpty } from "../../common/helpers/commonHelper";

import DownloadButton from "../../button/DownloadButton";
import IconButton from "../../button/IconButton";
import FaIcon from "../../icon/FaIcon";
import WithTooltip from "../../tooltip/WithTooltip";

import EditableTags from "../../form-components/EditableTags";

import { canDownloadResult } from "../../user/selectors";

import {
  renamePreviousQuery,
  retagPreviousQuery,
  toggleEditPreviousQueryLabel,
  toggleEditPreviousQueryTags,
} from "./actions";

import PreviousQueryTags from "./PreviousQueryTags";
import { formatDateDistance } from "../../common/helpers";
import { PreviousQueryT } from "./reducer";
import PreviousQueriesLabel from "./PreviousQueriesLabel";
import type { DatasetIdT } from "../../api/types";
import type { StateT } from "app-types";

const Root = styled("div")<{ own?: boolean; system?: boolean }>`
  margin: 0;
  padding: 5px 10px;
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
`;
const TopInfos = styled(Gray)`
  line-height: 24px;
`;

const TopRight = styled("div")`
  float: right;
`;
const SharedIndicator = styled("span")`
  margin-left: 10px;
  color: ${({ theme }) => theme.col.blueGray};
`;
const MiddleRow = styled("div")`
  display: flex;
  width: 100%;
  justify-content: space-between;
  line-height: 24px;
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

interface PropsT {
  query: PreviousQueryT;
  datasetId: DatasetIdT;
  onIndicateDeletion: () => void;
  onIndicateShare: () => void;
}

const PreviousQuery = React.forwardRef<HTMLDivElement, PropsT>(
  function PreviousQueryComponent(
    { query, datasetId, onIndicateDeletion, onIndicateShare },
    ref
  ) {
    const availableTags = useSelector<StateT, string[]>(
      (state) => state.previousQueries.tags
    );
    const userCanDownloadResults = useSelector<StateT, boolean>((state) =>
      canDownloadResult(state)
    );

    const dispatch = useDispatch();

    const onRenamePreviousQuery = (label: string) =>
      dispatch(renamePreviousQuery(datasetId, query.id, label));

    const onRetagPreviousQuery = (tags: string[]) =>
      dispatch(retagPreviousQuery(datasetId, query.id, tags));

    const onToggleEditPreviousQueryLabel = () =>
      dispatch(toggleEditPreviousQueryLabel(query.id));

    const onToggleEditPreviousQueryTags = () =>
      dispatch(toggleEditPreviousQueryTags(query.id));

    const peopleFound = isEmpty(query.numberOfResults)
      ? T.translate("previousQuery.notExecuted")
      : `${query.numberOfResults} ${T.translate("previousQueries.results")}`;
    const executedAt = formatDateDistance(
      parseISO(query.createdAt),
      new Date(),
      true
    );
    const label = query.label || query.id.toString();
    const mayEditQuery = query.own || query.shared;

    return (
      <Root
        ref={ref}
        own={!!query.own}
        shared={!!query.shared}
        system={!!query.system || (!query.own && !query.shared)}
      >
        <TopInfos>
          <div>
            {!!query.resultUrl && userCanDownloadResults ? (
              <WithTooltip text={T.translate("previousQuery.downloadResults")}>
                <DownloadButton tight bare url={query.resultUrl}>
                  {peopleFound}
                </DownloadButton>
              </WithTooltip>
            ) : (
              peopleFound
            )}
            {query.own && query.shared && (
              <SharedIndicator onClick={onIndicateShare}>
                {T.translate("common.shared")}
              </SharedIndicator>
            )}
            <TopRight>
              {executedAt}
              {mayEditQuery &&
                !query.editingTags &&
                (!query.tags || query.tags.length === 0) && (
                  <StyledWithTooltip text={T.translate("common.addTag")}>
                    <IconButton
                      icon="tags"
                      bare
                      onClick={onToggleEditPreviousQueryTags}
                    />
                  </StyledWithTooltip>
                )}
              {query.own && !query.shared && (
                <StyledWithTooltip text={T.translate("common.share")}>
                  <IconButton icon="upload" bare onClick={onIndicateShare} />
                </StyledWithTooltip>
              )}
              {query.loading ? (
                <StyledFaIcon icon="spinner" />
              ) : (
                query.own && (
                  <StyledWithTooltip text={T.translate("common.delete")}>
                    <IconButton
                      icon="times"
                      bare
                      onClick={onIndicateDeletion}
                    />
                  </StyledWithTooltip>
                )
              )}
            </TopRight>
          </div>
        </TopInfos>
        <MiddleRow>
          <PreviousQueriesLabel
            mayEditQuery={mayEditQuery}
            loading={!!query.loading}
            label={label}
            selectTextOnMount={true}
            editing={!!query.editingLabel}
            onSubmit={onRenamePreviousQuery}
            onToggleEdit={onToggleEditPreviousQueryLabel}
          />
          <Gray>{query.ownerName}</Gray>
        </MiddleRow>
        {mayEditQuery ? (
          <EditableTags
            tags={query.tags}
            editing={!!query.editingTags}
            loading={!!query.loading}
            onSubmit={onRetagPreviousQuery}
            onToggleEdit={onToggleEditPreviousQueryTags}
            tagComponent={<PreviousQueryTags tags={query.tags} />}
            availableTags={availableTags}
          />
        ) : (
          <PreviousQueryTags tags={query.tags} />
        )}
        {!!query.error && <StyledErrorMessage message={query.error} />}
      </Root>
    );
  }
);

export default PreviousQuery;
