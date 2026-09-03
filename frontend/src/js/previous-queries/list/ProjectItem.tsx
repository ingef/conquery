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
import { tv } from "tailwind-variants";
import type { ResultUrlWithLabel, SecondaryId } from "../../api/types";
import type { StateT } from "../../app/reducers";
import DownloadButton from "../../button/DownloadButton";
import { Highlighter } from "../../common/components/Highlighter";
import { formatDate } from "../../common/helpers/dateHelper";
import { exists } from "../../common/helpers/exists";
import { useFormLabelByType } from "../../external-forms/stateSelectors";
import FormSymbol from "../../symbols/FormSymbol";
import QuerySymbol from "../../symbols/QuerySymbol";
import { Button } from "../../ui-components/Button";
import { Icon } from "../../ui-components/Icon";
import {
  Tooltip,
  TooltipTarget,
  TooltipTrigger,
} from "../../ui-components/Tooltip";
import { useUpdateFormConfig, useUpdateQuery } from "./actions";
import { DeleteProjectItemButton } from "./DeleteProjectItemButton";
import { isFormConfig } from "./helpers";
import ProjectItemLabel from "./ProjectItemLabel";
import type { FormConfigT, PreviousQueryT } from "./reducer";

export type ProjectItemT = PreviousQueryT | FormConfigT;

const root = tv({
  base: [
    "flex items-center",
    "m-0",
    "cursor-pointer",
    "rounded",
    "border border-gray-100 hover:border-primary-200",
    "bg-bg-50",
    "shadow-[0_1px_2px_0_rgba(0,0,0,0.2)]",
    "overflow-hidden",
  ],
});

const topInfos = tv({
  base: [
    "flex items-center justify-between",
    "leading-5",
    "text-gray-500",
    "text-xs",
  ],
});

const ownerName = tv({
  base: ["shrink-0", "pl-[5px]", "text-gray-500", "text-xs"],
});

const tooltipText = tv({ base: ["flex flex-col items-start", "font-normal"] });

const labelRow = tv({
  base: ["flex justify-between", "w-full", "leading-6", "my-[2px]"],
});

const content = tv({
  base: [
    "grow shrink",
    "px-[10px] py-1",
    "overflow-hidden",
    "border-l border-gray-100",
  ],
  variants: {
    // later wins when both are set
    system: { true: "border-l-[5px]" },
    own: { true: "border-l-[5px] border-primary-500" },
  },
});

const downloadButton = tv({
  base: ["whitespace-nowrap", "[&_button]:text-xs"],
});

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
    <div className={tooltipText()}>
      {t("previousQuery.editFolders")}
      {folders.length > 0 && (
        <ul className="mt-[6px] pl-[18px] text-left">
          {folders.map((f) => (
            <li key={f}>{f}</li>
          ))}
        </ul>
      )}
    </div>
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
    <TooltipTrigger>
      <Button
        intent="tertiary"
        size="sm"
        aria-label={isShared ? t("common.shared") : t("common.share")}
        data-test-id="share"
        onPress={onClick}
      >
        <Icon icon={isShared ? faUser : faUserRegular} />
      </Button>
      <Tooltip>
        {
          <div className={tooltipText()}>
            {isShared ? t("common.shared") : t("common.share")}
          </div>
        }
      </Tooltip>
    </TooltipTrigger>
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
  if (!resultUrl) return <span className="whitespace-nowrap">{label}</span>;
  return (
    <TooltipTrigger>
      <DownloadButton
        className={downloadButton()}
        simpleIcon
        resultUrl={resultUrl}
      >
        {label}
      </DownloadButton>
      <Tooltip>{t("previousQuery.downloadResults")}</Tooltip>
    </TooltipTrigger>
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
    <div className={root()} ref={ref}>
      {isForm ? (
        <FormSymbol className="shrink-0" />
      ) : (
        <QuerySymbol className="shrink-0" />
      )}
      <div className={content({ own: !!item.own, system: isSystem })}>
        <div className={topInfos()}>
          <div className="flex items-center gap-[10px]">
            <TooltipTrigger>
              <Button
                intent="tertiary"
                size="sm"
                onPress={onIndicateEditFolders}
                isDisabled={!mayEdit}
              >
                <Icon
                  icon={folders.length === 0 ? faFolderRegular : faFolder}
                />
              </Button>
              <Tooltip>{<FoldersTooltip folders={folders} />}</Tooltip>
            </TooltipTrigger>
            <div className="flex items-center gap-2">
              <ResultsLabel label={topLeftLabel} resultUrl={resultUrl} />
              {hasNoDates && (
                <TooltipTrigger>
                  <TooltipTarget
                    role="img"
                    aria-label={t("previousQuery.hasNoDates")}
                    excludeFromTabOrder
                  >
                    <Icon icon={faCalendar} className="opacity-70 text-red" />
                  </TooltipTarget>
                  <Tooltip>{t("previousQuery.hasNoDates")}</Tooltip>
                </TooltipTrigger>
              )}
            </div>
          </div>
          <div className="ml-[5px] flex shrink-0 items-center gap-[10px]">
            {executedAt}
            {secondaryId && (
              <TooltipTrigger>
                <Button
                  aria-label={`${t("queryEditor.secondaryId")}: ${secondaryId.label}`}
                  intent="tertiary"
                  size="sm"
                  onPress={() => {}}
                >
                  <Icon icon={faMicroscope} />
                </Button>
                <Tooltip>{`${t("queryEditor.secondaryId")}: ${secondaryId.label}`}</Tooltip>
              </TooltipTrigger>
            )}
            {item.own && (
              <ShareButton isShared={!!isShared} onClick={onIndicateShare} />
            )}
            {item.own && <DeleteProjectItemButton item={item} />}
          </div>
        </div>
        <div className={labelRow()}>
          <ProjectItemLabel
            mayEdit={mayEdit}
            label={label}
            selectTextOnMount={true}
            onSubmit={onRenameLabel}
            highlightedWords={highlightedWords}
            isEditing={isEditingLabel}
            setIsEditing={setIsEditingLabel}
          />
          <div className={ownerName()}>
            <HighlightedText
              text={item.ownerName}
              highlightedWords={highlightedWords}
            />
          </div>
        </div>
      </div>
    </div>
  );
};

export default ProjectItem;
