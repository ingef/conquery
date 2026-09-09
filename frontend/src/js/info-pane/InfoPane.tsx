import {
  faThumbtack,
  type IconDefinition,
} from "@fortawesome/free-solid-svg-icons";
import { type ReactNode, useMemo } from "react";
import { useTranslation } from "react-i18next";
import Markdown from "react-markdown";
import { useDispatch, useSelector } from "react-redux";
import remarkFlexibleMarkers from "remark-flexible-markers";
import remarkGfm from "remark-gfm";
import { tv } from "tailwind-variants";
import type { StateT } from "../app/reducers";
import { Highlighter } from "../common/components/Highlighter";
import { Icon } from "../ui-components/Icon";
import { ToggleButton } from "../ui-components/ToggleButton";
import { toggleAdditionalInfos as toggleInfos } from "./actions";
import InfoPaneCollapsed from "./InfoPaneCollapsed";
import { InfoPaneHeader } from "./InfoPaneHeader";
import MatchingStats from "./MatchingStats";
import type { AdditionalInfosType } from "./reducer";

const root = tv({
  base: [
    "h-full w-full",
    "pt-[40px] pb-[10px]",
    "relative",
    "flex flex-col",
    "bg-bg-100",
  ],
});

const content = tv({
  base: [
    "px-5 pt-[18px] pb-[10px]",
    "w-full",
    "grow",
    "overflow-y-auto overflow-x-hidden",
    "[-webkit-overflow-scrolling:touch]",
  ],
});

const head = tv({
  base: [
    "px-5 py-[10px]",
    "bg-white",
    "my-5 -mx-5",
    "shadow-[0_0_3px_0_rgba(0,0,0,0.2)]",
  ],
});

const typeIcon = tv({ base: ["mt-px", "mr-[6px]", "text-primary-500"] });

const pinnedLabel = tv({
  base: [
    "flex flex-row items-start",
    "gap-[5px]",
    "m-0",
    "leading-[1.2]",
    "text-sm",
  ],
});

const descriptionText = tv({
  base: ["mt-[5px] mb-[2px]", "text-xs", "leading-[1.3]", "uppercase"],
});

const indentRoot = tv({ base: ["pl-[15px]", "mt-[5px] mb-3"] });

const pieceOfInfo = tv({
  base: [
    "mb-[15px]",
    // Markdown
    "text-xs",
    "[&_a]:underline",
    "[&_p]:leading-[1.3] [&_p]:my-[5px]",
    "[&_table]:border-collapse",
    "[&_td]:border [&_td]:border-gray-500 [&_td]:p-[5px]",
    "[&_th]:border [&_th]:border-gray-500 [&_th]:p-[5px]",
  ],
});

const infoHeadline = tv({
  base: ["m-0", "text-xs", "font-bold", "leading-[1.3]"],
});

const matchingStats = tv({
  base: ["grid grid-cols-[auto_1fr]", "gap-3", "items-center"],
});

const HighlightedText = ({
  text,
  words = [],
}: {
  words: string[];
  text: string;
}) => {
  return <Highlighter searchWords={words} textToHighlight={text || ""} />;
};

const ConceptLabel = ({
  label,
  conceptIcon,
  tackIcon,
}: {
  label?: string;
  conceptIcon?: IconDefinition;
  tackIcon?: ReactNode;
}) => {
  const wordsRaw = useSelector<StateT, string[] | null>(
    (state) => state.conceptTrees.search.words,
  );
  const words = useMemo(() => wordsRaw || [], [wordsRaw]);
  const { t } = useTranslation();

  return (
    <p className={pinnedLabel()}>
      {conceptIcon && <Icon icon={conceptIcon} className={typeIcon()} />}
      <span className="grow">
        {label ? (
          <HighlightedText words={words} text={label} />
        ) : (
          t("infoPane.placeholder")
        )}
      </span>
      {tackIcon}
    </p>
  );
};

const mark = (text: string, regex: RegExp | null): string => {
  if (!regex) return text;
  return text.replace(regex, "==$&==");
};

const InfoPane = () => {
  const wordsRaw = useSelector<StateT, string[] | null>(
    (state) => state.conceptTrees.search.words,
  );
  const words = useMemo(() => wordsRaw || [], [wordsRaw]);

  const {
    label,
    description,
    infos,
    matchingEntries,
    matchingEntities,
    dateRange,
    icon,
    rootLabel,
    rootIcon,
  } = useSelector<StateT, AdditionalInfosType>(
    (state) => state.infoPane.additionalInfos,
  );
  const isOpen = useSelector<StateT, boolean>((state) => state.infoPane.isOpen);
  const toggleAdditionalInfos = useSelector<StateT, boolean>(
    (state) => state.infoPane.toggleAdditionalInfos,
  );

  const highlightRegex = useMemo(() => {
    return words.length > 0
      ? new RegExp(words.filter((word) => word.length > 0).join("|"), "gi")
      : null;
  }, [words]);

  const dispatch = useDispatch();
  const onToggleAdditionalInfos = () => dispatch(toggleInfos());

  if (!isOpen) return <InfoPaneCollapsed />;

  const mainLabel = rootLabel || label;
  const mainIcon = rootIcon || icon;

  const differentRootLabel = !!rootLabel && rootLabel !== label;

  return (
    <div className={root()}>
      <InfoPaneHeader />
      <div className={content()}>
        <MatchingStats
          className={matchingStats()}
          matchingEntries={matchingEntries}
          matchingEntities={matchingEntities}
          dateRange={dateRange}
        />
        <div className={head()}>
          <ConceptLabel
            label={mainLabel}
            conceptIcon={mainIcon}
            tackIcon={
              toggleAdditionalInfos && (
                <ToggleButton
                  intent="tertiary"
                  size="sm"
                  isSelected
                  onChange={onToggleAdditionalInfos}
                >
                  <Icon icon={faThumbtack} />
                </ToggleButton>
              )
            }
          />
          {differentRootLabel && (
            <div className={indentRoot()}>
              <ConceptLabel label={label} conceptIcon={icon} />
            </div>
          )}
          {description && (
            <p className={descriptionText()}>
              <HighlightedText words={words} text={description} />
            </p>
          )}
        </div>
        <div className="w-full overflow-x-auto">
          {infos?.map((info, i) => (
            <div className={pieceOfInfo()} key={info.key + i}>
              <h4 className={infoHeadline()}>
                <HighlightedText words={words} text={info.key} />
              </h4>
              <Markdown remarkPlugins={[remarkGfm, remarkFlexibleMarkers]}>
                {mark(info.value, highlightRegex)}
              </Markdown>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

export default InfoPane;
