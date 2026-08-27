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
import type { TFunction } from "i18next";
import { type Ref, useState } from "react";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";
import type { ResultUrlWithLabel, SecondaryId } from "../../api/types";
import type { StateT } from "../../app/reducers";
import DownloadButton from "../../button/DownloadButton";
import IconButton from "../../button/IconButton";
import { Highlighter } from "../../common/components/Highlighter";
import { formatDate } from "../../common/helpers/dateHelper";
import { exists } from "../../common/helpers/exists";
import { useFormLabelByType } from "../../external-forms/stateSelectors";
import FaIcon from "../../icon/FaIcon";
import FormSymbol from "../../symbols/FormSymbol";
import QuerySymbol from "../../symbols/QuerySymbol";
import WithTooltip from "../../tooltip/WithTooltip";
import { useUpdateFormConfig, useUpdateQuery } from "./actions";
import { DeleteProjectItemButton } from "./DeleteProjectItemButton";
import { isFormConfig } from "./helpers";
import ProjectItemLabel from "./ProjectItemLabel";
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

const SxFaIcon = styled(FaIcon)`
  opacity: 0.7;
`;

const FoldersButton = styled(IconButton)`
  margin-right: 10px;
`;

const getTopLeftLabel = (
  item: ProjectItemT,
  formLabel: string | null | undefined,
  t: TFunction,
) => {
  if (isFormConfig(item)) return formLabel!;
  if (exists(item.numberOfResults)) {
    return `${item.numberOfResults} ${t("previousQueries.results")}`;
  }
  return t("previousQuery.notExecuted");
};

const HighlightedText = ({
  text,
  highlightedWords,
}: {
  text: string;
  highlightedWords: string[];
}) =>
  highlightedWords.length > 0 ? (
    <Highlighter searchWords={highlightedWords} textToHighlight={text} />
  ) : (
    text
  );

const FoldersTooltip = ({ folders }: { folders: string[] }) => {
  const { t } = useTranslation();
  return (
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
  );
};

const ShareButton = ({
  isShared,
  onClick,
}: {
  isShared: boolean;
  onClick: () => void;
}) => {
  const { t } = useTranslation();
  return (
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
        title="share"
        data-test-id="share"
        onClick={onClick}
      />
    </WithTooltip>
  );
};

const useRenameProjectItem = (item: ProjectItemT) => {
  const { t } = useTranslation();
  const { updateQuery } = useUpdateQuery();
  const { updateFormConfig } = useUpdateFormConfig();

  return (label: string) => {
    if (isFormConfig(item)) {
      updateFormConfig(item.id, { label }, t("formConfig.renameError"));
    } else {
      updateQuery(item.id, { label }, t("previousQuery.renameError"));
    }
  };
};

const ResultsLabel = ({
  label,
  resultUrl,
}: {
  label: string;
  resultUrl: ResultUrlWithLabel | null;
}) => {
  const { t } = useTranslation();
  if (!resultUrl) return <NonBreakingText>{label}</NonBreakingText>;
  return (
    <WithTooltip text={t("previousQuery.downloadResults")}>
      <SxDownloadButton tight small bare simpleIcon resultUrl={resultUrl}>
        {label}
      </SxDownloadButton>
    </WithTooltip>
  );
};

const deriveFlags = (item: ProjectItemT, loadedSecondaryIds: SecondaryId[]) => {
  const isForm = isFormConfig(item);
  const isShared = item.shared || (item.groups && item.groups.length > 0);
  const secondaryId =
    !isForm && item.secondaryId && item.queryType === "SECONDARY_ID_QUERY"
      ? loadedSecondaryIds.find((secId) => item.secondaryId === secId.id)
      : null;

  return {
    isShared,
    isSystem: !!item.system || (!item.own && !isShared),
    mayEdit: item.own || isShared,
    secondaryId,
    resultUrl:
      !isForm && item.resultUrls.length > 0 ? item.resultUrls[0] : null,
    hasNoDates: !isForm && !item.containsDates,
  };
};

const ProjectItem = ({
  ref,
  item,
  onIndicateShare,
  onIndicateEditFolders,
}: {
  ref?: Ref<HTMLDivElement>;

  item: ProjectItemT;
  onIndicateShare: () => void;
  onIndicateEditFolders: () => void;
}) => {
  const { t } = useTranslation();
  const highlightedWords = useSelector<StateT, string[]>(
    (state) => state.projectItemsSearch.words,
  );

  const loadedSecondaryIds = useSelector<StateT, SecondaryId[]>(
    (state) => state.conceptTrees.secondaryIds,
  );

  const isForm = isFormConfig(item);
  const formLabel = useFormLabelByType(isForm ? item.formType : null);
  const topLeftLabel = getTopLeftLabel(item, formLabel, t);

  const dateFormat = `${t("inputDateRange.dateFormat")} HH:mm`;
  const executedAtDate = parseISO(item.createdAt);
  const executedAt = formatDate(executedAtDate, dateFormat);

  const label = item.label || item.id.toString();
  const { isShared, isSystem, mayEdit, secondaryId, resultUrl, hasNoDates } =
    deriveFlags(item, loadedSecondaryIds);

  const [isEditingLabel, setIsEditingLabel] = useState<boolean>(false);

  const folders = item.tags;

  const onRenameLabel = useRenameProjectItem(item);

  return (
    <Root ref={ref}>
      {isForm ? <SxFormSymbol /> : <SxQuerySymbol />}
      <Content own={!!item.own} system={isSystem}>
        <TopInfos>
          <TopLeft>
            <WithTooltip html={<FoldersTooltip folders={folders} />}>
              <FoldersButton
                icon={folders.length === 0 ? faFolderRegular : faFolder}
                tight
                small
                bare
                onClick={onIndicateEditFolders}
                disabled={!mayEdit}
              />
            </WithTooltip>
            <div className="flex items-center gap-2">
              <ResultsLabel label={topLeftLabel} resultUrl={resultUrl} />
              {hasNoDates && (
                <WithTooltip text={t("previousQuery.hasNoDates")}>
                  <SxFaIcon red icon={faCalendar} />
                </WithTooltip>
              )}
            </div>
          </TopLeft>
          <TopRight>
            {executedAt}
            {secondaryId && (
              <WithTooltip
                text={`${t("queryEditor.secondaryId")}: ${secondaryId.label}`}
              >
                <IconButton icon={faMicroscope} bare onClick={() => {}} />
              </WithTooltip>
            )}
            {item.own && (
              <ShareButton isShared={!!isShared} onClick={onIndicateShare} />
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
            <HighlightedText
              text={item.ownerName}
              highlightedWords={highlightedWords}
            />
          </OwnerName>
        </LabelRow>
      </Content>
    </Root>
  );
};

export default ProjectItem;
