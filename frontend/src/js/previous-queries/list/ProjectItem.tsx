import styled from "@emotion/styled";
import {
  faCalendar,
  faFolder as faFolderRegular,
  faUser as faUserRegular,
} from "@fortawesome/free-regular-svg-icons";
import {
  faFolder,
  faMicroscope,
  faUser,
} from "@fortawesome/free-solid-svg-icons";
import { parseISO } from "date-fns";
import { forwardRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";

import type { SecondaryId } from "../../api/types";
import type { StateT } from "../../app/reducers";
import DownloadButton from "../../button/DownloadButton";
import IconButton from "../../button/IconButton";
import { formatDate } from "../../common/helpers/dateHelper";
import { exists } from "../../common/helpers/exists";
import { useFormLabelByType } from "../../external-forms/stateSelectors";
import FaIcon from "../../icon/FaIcon";
import FormSymbol from "../../symbols/FormSymbol";
import QuerySymbol from "../../symbols/QuerySymbol";
import WithTooltip from "../../tooltip/WithTooltip";

import Highlighter from "react-highlight-words";
import { DeleteProjectItemButton } from "./DeleteProjectItemButton";
import ProjectItemLabel from "./ProjectItemLabel";
import { useUpdateFormConfig, useUpdateQuery } from "./actions";
import { isFormConfig } from "./helpers";
import type { FormConfigT, PreviousQueryT } from "./reducer";

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
  gap: 10px;
  flex-shrink: 0;
  margin-left: 5px;
`;
const TopLeft = styled("div")`
  display: flex;
  align-items: center;
`;

const TooltipText = styled("div")`
  font-weight: 400;
  font-size: ${({ theme }) => theme.font.md};
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  padding: 8px 14px;
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

const Row = styled("div")`
  display: flex;
  align-items: flex-start;
  gap: 5px;
`;

const SxFaIcon = styled(FaIcon)`
  opacity: 0.7;
`;

const FoldersButton = styled(IconButton)`
  margin-right: 10px;
`;

const ProjectItem = forwardRef<
  HTMLDivElement,
  {
    item: ProjectItemT;
    onIndicateShare: () => void;
    onIndicateEditFolders: () => void;
  }
>(function ProjectItemComponent(
  { item, onIndicateShare, onIndicateEditFolders },
  ref,
) {
  const { t } = useTranslation();
  const highlightedWords = useSelector<StateT, string[]>(
    (state) => state.projectItemsSearch.words,
  );

  const loadedSecondaryIds = useSelector<StateT, SecondaryId[]>(
    (state) => state.conceptTrees.secondaryIds,
  );

  const { updateQuery } = useUpdateQuery();
  const { updateFormConfig } = useUpdateFormConfig();

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
  const mayEdit = item.own || isShared;

  const secondaryId =
    !isFormConfig(item) && item.secondaryId
      ? loadedSecondaryIds.find((secId) => item.secondaryId === secId.id)
      : null;

  const [isEditingLabel, setIsEditingLabel] = useState<boolean>(false);

  const folders = item.tags;

  const onRenameLabel = (label: string) => {
    if (isFormConfig(item)) {
      updateFormConfig(item.id, { label }, t("formConfig.renameError"));
    } else {
      updateQuery(item.id, { label }, t("previousQuery.renameError"));
    }
  };

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
                icon={folders.length === 0 ? faFolderRegular : faFolder}
                tight
                small
                bare
                onClick={onIndicateEditFolders}
                disabled={!mayEdit}
              />
            </WithTooltip>
            {!isFormConfig(item) && item.resultUrls.length > 0 ? (
              <Row>
                <WithTooltip text={t("previousQuery.downloadResults")}>
                  <SxDownloadButton
                    tight
                    small
                    bare
                    simpleIcon
                    resultUrl={item.resultUrls[0]}
                  >
                    {topLeftLabel}
                  </SxDownloadButton>
                </WithTooltip>
                {!item.containsDates && (
                  <WithTooltip text={t("previousQuery.hasNoDates")}>
                    <SxFaIcon red icon={faCalendar} />
                  </WithTooltip>
                )}
              </Row>
            ) : (
              <NonBreakingText>{topLeftLabel}</NonBreakingText>
            )}
          </TopLeft>
          <TopRight>
            {executedAt}
            {secondaryId &&
              !isFormConfig(item) &&
              item.queryType === "SECONDARY_ID_QUERY" && (
                <WithTooltip
                  text={`${t("queryEditor.secondaryId")}: ${secondaryId.label}`}
                >
                  <IconButton icon={faMicroscope} bare onClick={() => {}} />
                </WithTooltip>
              )}
            {item.own && (
              <WithTooltip
                html={
                  <TooltipText>
                    {isShared ? t("common.shared") : t("common.share")}
                  </TooltipText>
                }
              >
                <IconButton
                  icon={isShared ? faUser : faUserRegular}
                  bare
                  onClick={onIndicateShare}
                />
              </WithTooltip>
            )}
            {item.own && <DeleteProjectItemButton item={item} />}
          </TopRight>
        </TopInfos>
        <LabelRow>
          <ProjectItemLabel
            mayEdit={mayEdit}
            label={label}
            selectTextOnMount={true}
            onSubmit={onRenameLabel}
            highlightedWords={highlightedWords}
            isEditing={isEditingLabel}
            setIsEditing={setIsEditingLabel}
          />
          <OwnerName>
            {highlightedWords.length > 0 ? (
              <Highlighter
                searchWords={highlightedWords}
                autoEscape
                textToHighlight={item.ownerName}
              />
            ) : (
              item.ownerName
            )}
          </OwnerName>
        </LabelRow>
      </Content>
    </Root>
  );
});

export default ProjectItem;
