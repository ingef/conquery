import styled from "@emotion/styled";
import type { StateT } from "app-types";
import { parseISO } from "date-fns";
import { FormConfigT } from "js/external-forms/form-configs/reducer";
import { useFormLabelByType } from "js/external-forms/stateSelectors";
import FormSymbol from "js/symbols/FormSymbol";
import QuerySymbol from "js/symbols/QuerySymbol";
import { forwardRef, useState } from "react";
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

import ProjectItemLabel from "./ProjectItemLabel";
import { useRemoveQuery, useRenameQuery } from "./actions";
import { isFormConfig } from "./helpers";
import { PreviousQueryT } from "./reducer";

export type ProjectItemT = PreviousQueryT | FormConfigT;

const Root = styled("div")<{ own?: boolean; system?: boolean }>`
  margin: 0;
  cursor: pointer;
  border-radius: ${({ theme }) => theme.borderRadius};
  border: 1px solid ${({ theme }) => theme.col.grayLight};
  background-color: ${({ theme }) => theme.col.bg};
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.2);
  overflow: hidden;
  display: flex;
  align-items: center;

  &:hover {
    border-color: ${({ theme }) => theme.col.blueGray};
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

const SxQuerySymbol = styled(QuerySymbol)`
  flex-shrink: 0;
`;

const SxFormSymbol = styled(FormSymbol)`
  flex-shrink: 0;
`;

const TopRight = styled("div")`
  display: flex;
  align-items: center;
  flex-shrink: 0;
  margin-left: 5px;
`;
const TopLeft = styled("div")`
  display: flex;
  align-items: center;
`;

const TooltipText = styled("div")`
  font-weight: 400;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
`;

const ActiveFolders = styled("ul")`
  margin: 6px 0 0;
  text-align: left;
  padding-left: 18px;
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

const Content = styled("div")<{ own?: boolean; system?: boolean }>`
  flex-grow: 1;
  flex-shrink: 1;
  padding: 4px 10px;
  overflow: hidden;
  border-left: ${({ theme, own, system }) =>
    own
      ? `5px solid ${theme.col.blueGrayDark}`
      : system
      ? `5px solid ${theme.col.grayLight}`
      : `1px solid ${theme.col.grayLight}`};
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
  item: ProjectItemT;
  datasetId: DatasetIdT;
  onIndicateDeletion: () => void;
  onIndicateShare: () => void;
  onIndicateEditFolders: () => void;
}

const ProjectItem = forwardRef<HTMLDivElement, PropsT>(
  function PreviousQueryComponent(
    {
      item,
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
    const removeQuery = useRemoveQuery(item.id);

    const mayDeleteQueryRightAway =
      item.tags.length === 0 && !!item.isPristineLabel;
    const onDeleteClick = mayDeleteQueryRightAway
      ? removeQuery
      : onIndicateDeletion;

    const formLabel = useFormLabelByType(
      isFormConfig(item) ? item.formType : null,
    );
    const topLeftLabel = isFormConfig(item)
      ? formLabel!
      : exists(item.numberOfResults)
      ? `${item.numberOfResults} ${t("previousQueries.results")}`
      : t("previousQuery.notExecuted");

    const dateFormat = `${t("inputDateRange.dateFormat")} HH:mm`;
    const executedAtDate = parseISO(item.createdAt);
    const executedAt = formatDate(executedAtDate, dateFormat);

    const isShared = item.shared || (item.groups && item.groups.length > 0);
    const label = item.label || item.id.toString();
    const mayEditQuery = item.own || isShared;

    const secondaryId =
      !isFormConfig(item) && item.secondaryId
        ? loadedSecondaryIds.find((secId) => item.secondaryId === secId.id)
        : null;

    const [isEditingLabel, setIsEditingLabel] = useState<boolean>(false);

    const folders = item.tags;

    return (
      <Root ref={ref}>
        {isFormConfig(item) ? <SxFormSymbol /> : <SxQuerySymbol />}
        <Content
          own={!!item.own}
          system={!!item.system || (!item.own && !isShared)}
        >
          <TopInfos>
            <TopLeft>
              <WithTooltip
                html={
                  <TooltipText>
                    {t("previousQuery.editFolders")}
                    {folders.length > 0 && (
                      <ActiveFolders>
                        {folders.map((f) => (
                          <li key={f}>{f}</li>
                        ))}
                      </ActiveFolders>
                    )}
                  </TooltipText>
                }
              >
                <FoldersButton
                  icon={"folder"}
                  regular={folders.length === 0}
                  tight
                  small
                  bare
                  onClick={onIndicateEditFolders}
                  disabled={!mayEditQuery}
                />
              </WithTooltip>
              {!isFormConfig(item) && item.resultUrls.length > 0 ? (
                <WithTooltip text={t("previousQuery.downloadResults")}>
                  <SxDownloadButton tight small bare url={item.resultUrls[0]}>
                    {topLeftLabel}
                  </SxDownloadButton>
                </WithTooltip>
              ) : (
                <NonBreakingText>{topLeftLabel}</NonBreakingText>
              )}
            </TopLeft>
            <TopRight>
              {executedAt}
              {secondaryId &&
                !isFormConfig(item) &&
                item.queryType === "SECONDARY_ID_QUERY" && (
                  <StyledWithTooltip
                    text={`${t("queryEditor.secondaryId")}: ${
                      secondaryId.label
                    }`}
                  >
                    <IconButton icon="microscope" bare onClick={() => {}} />
                  </StyledWithTooltip>
                )}
              {item.own && (
                <StyledWithTooltip
                  html={
                    <TooltipText>
                      {isShared ? t("common.shared") : t("common.share")}
                    </TooltipText>
                  }
                >
                  <IconButton
                    icon="user"
                    regular={!isShared}
                    bare
                    onClick={onIndicateShare}
                  />
                </StyledWithTooltip>
              )}
              {!isFormConfig(item) && item.loading ? (
                <StyledFaIcon icon="spinner" />
              ) : (
                item.own && (
                  <StyledWithTooltip text={t("common.delete")}>
                    <IconButton icon="times" bare onClick={onDeleteClick} />
                  </StyledWithTooltip>
                )
              )}
            </TopRight>
          </TopInfos>
          <LabelRow>
            <ProjectItemLabel
              mayEditQuery={mayEditQuery}
              loading={!isFormConfig(item) && !!item.loading}
              label={label}
              selectTextOnMount={true}
              onSubmit={(label) => renameQuery(datasetId, item.id, label)}
              isEditing={isEditingLabel}
              setIsEditing={setIsEditingLabel}
            />
            <OwnerName>{item.ownerName}</OwnerName>
          </LabelRow>
          {!isFormConfig(item) && !!item.error && (
            <StyledErrorMessage message={item.error} />
          )}
        </Content>
      </Root>
    );
  },
);

export default ProjectItem;
