import styled from "@emotion/styled";
import { faThumbtack, IconDefinition } from "@fortawesome/free-solid-svg-icons";
import { ReactNode, useMemo } from "react";
import Highlighter from "react-highlight-words";
import { useTranslation } from "react-i18next";
import Markdown from "react-markdown";
import { useDispatch, useSelector } from "react-redux";
import remarkFlexibleMarkers from "remark-flexible-markers";
import remarkGfm from "remark-gfm";

import type { StateT } from "../app/reducers";
import IconButton from "../button/IconButton";
import FaIcon from "../icon/FaIcon";

import ActivateTooltip from "./ActivateTooltip";
import TooltipEntries from "./TooltipEntries";
import { TooltipHeader } from "./TooltipHeader";
import { toggleAdditionalInfos as toggleInfos } from "./actions";
import type { AdditionalInfosType } from "./reducer";

const Root = styled("div")`
  width: 100%;
  height: 100%;
  padding: 40px 0 10px;
  position: relative;
  display: flex;
  flex-direction: column;
  background: ${({ theme }) => theme.col.bgAlt};
`;

const Content = styled("div")`
  padding: 18px 20px 10px;
  width: 100%;
  flex-grow: 1;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  overflow-x: hidden;
`;
const Head = styled("div")`
  padding: 10px 20px;
  background-color: white;
  margin: 20px -20px;
  box-shadow: 0 0 3px 0 rgba(0, 0, 0, 0.2);
`;

const StyledFaIcon = styled(FaIcon)`
  margin-top: 1px;
  color: ${({ theme }) => theme.col.blueGrayDark};
`;
const TackIconButton = styled(IconButton)`
  display: inline-flex; // To remove some height that seemed to be added
  margin-left: 5px;
`;
const TypeIcon = styled(StyledFaIcon)`
  margin-right: 6px;
`;
const PinnedLabel = styled("p")`
  display: flex;
  flex-direction: row;
  align-items: flex-start;
  margin: 0;
  line-height: 1.2;
  font-size: ${({ theme }) => theme.font.sm};
`;
const Label = styled("span")`
  flex-grow: 1;
`;
const Description = styled("p")`
  margin: 5px 0 2px;
  font-size: ${({ theme }) => theme.font.xs};
  line-height: 1.3;
  text-transform: uppercase;
`;

const Infos = styled("div")`
  width: 100%;
  overflow-x: auto;
`;

const IndentRoot = styled("div")`
  padding-left: 15px;
  margin: 5px 0 12px;
`;

const PieceOfInfo = styled("div")`
  margin-bottom: 15px;

  /* Markdown */
  font-size: ${({ theme }) => theme.font.xs};

  a {
    text-decoration: underline;
  }

  p {
    line-height: 1.3;
    margin: 5px 0;
  }

  table {
    border-collapse: collapse;
  }
  td,
  th {
    border: 1px solid ${({ theme }) => theme.col.gray};
    padding: 5px;
  }
`;

const InfoHeadline = styled("h4")`
  margin: 0;
  font-size: ${({ theme }) => theme.font.xs};
  font-weight: 700;
  line-height: 1.3;
`;

const HighlightedText = ({
  text,
  words = [],
}: {
  words: string[];
  text: string;
}) => {
  return (
    <Highlighter
      searchWords={words}
      autoEscape={true}
      textToHighlight={text || ""}
    />
  );
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
  const words = useSelector<StateT, string[]>(
    (state) => state.conceptTrees.search.words || [],
  );
  const { t } = useTranslation();

  return (
    <PinnedLabel>
      {conceptIcon && <TypeIcon icon={conceptIcon} />}
      <Label>
        {label ? (
          <HighlightedText words={words} text={label} />
        ) : (
          t("tooltip.placeholder")
        )}
      </Label>
      {tackIcon}
    </PinnedLabel>
  );
};

const mark = (text: string, regex: RegExp | null): string => {
  if (!regex) return text;
  return text.replace(regex, "==$&==");
};

const Tooltip = () => {
  const words = useSelector<StateT, string[]>(
    (state) => state.conceptTrees.search.words || [],
  );
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
    (state) => state.tooltip.additionalInfos,
  );
  const displayTooltip = useSelector<StateT, boolean>(
    (state) => state.tooltip.displayTooltip,
  );
  const toggleAdditionalInfos = useSelector<StateT, boolean>(
    (state) => state.tooltip.toggleAdditionalInfos,
  );

  const highlightRegex = useMemo(() => {
    return words.length > 0
      ? new RegExp(words.filter((word) => word.length > 0).join("|"), "gi")
      : null;
  }, [words]);

  const dispatch = useDispatch();
  const onToggleAdditionalInfos = () => dispatch(toggleInfos());

  if (!displayTooltip) return <ActivateTooltip />;

  const mainLabel = rootLabel || label;
  const mainIcon = rootIcon || icon;

  const differentRootLabel = !!rootLabel && rootLabel !== label;

  return (
    <Root>
      <TooltipHeader />
      <Content>
        <TooltipEntries
          matchingEntries={matchingEntries}
          matchingEntities={matchingEntities}
          dateRange={dateRange}
        />
        <Head>
          <ConceptLabel
            label={mainLabel}
            conceptIcon={mainIcon}
            tackIcon={
              toggleAdditionalInfos && (
                <TackIconButton
                  bare
                  active
                  onClick={onToggleAdditionalInfos}
                  icon={faThumbtack}
                />
              )
            }
          />
          {differentRootLabel && (
            <IndentRoot>
              <ConceptLabel label={label} conceptIcon={icon} />
            </IndentRoot>
          )}
          {description && (
            <Description>
              <HighlightedText words={words} text={description} />
            </Description>
          )}
        </Head>
        <Infos>
          {infos &&
            infos.map((info, i) => (
              <PieceOfInfo key={info.key + i}>
                <InfoHeadline>
                  <HighlightedText words={words} text={info.key} />
                </InfoHeadline>
                <Markdown remarkPlugins={[remarkGfm, remarkFlexibleMarkers]}>
                  {mark(info.value, highlightRegex)}
                </Markdown>
              </PieceOfInfo>
            ))}
        </Infos>
      </Content>
    </Root>
  );
};

export default Tooltip;
