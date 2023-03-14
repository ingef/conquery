import styled from "@emotion/styled";
import { useMemo } from "react";
import {
  faFolder,
  faMinus,
  faThumbtack,
} from "@fortawesome/free-solid-svg-icons";
import Highlighter from "react-highlight-words";
import { useTranslation } from "react-i18next";
import Markdown from "react-markdown";
import { useDispatch, useSelector } from "react-redux";
import remarkGfm from "remark-gfm";

import type { StateT } from "../app/reducers";
import IconButton from "../button/IconButton";
import type { SearchT } from "../concept-trees/reducer";
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
  margin-right: 5px;
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

const IndentRoot = styled("div")<{ depth: number }>`
  padding-left: ${({ depth }) => depth * 15 + "px"};
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

const Tooltip = () => {
  const { t } = useTranslation();

  const additionalInfos = useSelector<StateT, AdditionalInfosType>(
    (state) => state.tooltip.additionalInfos,
  );
  const displayTooltip = useSelector<StateT, boolean>(
    (state) => state.tooltip.displayTooltip,
  );
  const toggleAdditionalInfos = useSelector<StateT, boolean>(
    (state) => state.tooltip.toggleAdditionalInfos,
  );
  const search = useSelector<StateT, SearchT>(
    (state) => state.conceptTrees.search,
  );

  const dispatch = useDispatch();
  const onToggleAdditionalInfos = () => dispatch(toggleInfos());

  const {
    label,
    description,
    isFolder,
    infos,
    matchingEntries,
    matchingEntities,
    dateRange,
    parent,
  } = additionalInfos;

  const hasChild = useMemo(() => {
    return parent !== label && parent;
  }, [parent, label]);

  if (!displayTooltip) return <ActivateTooltip />;
  const searchHighlight = (text: string) => {
    return (
      <Highlighter
        searchWords={search.words || []}
        autoEscape={true}
        textToHighlight={text || ""}
      />
    );
  };

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
          <PinnedLabel>
            <TypeIcon icon={isFolder ? faFolder : faMinus} />
            <Label>
              {parent
                ? searchHighlight(parent)
                : label
                ? searchHighlight(label)
                : t("tooltip.placeholder")}
            </Label>
            {toggleAdditionalInfos && (
              <TackIconButton
                bare
                active
                onClick={onToggleAdditionalInfos}
                icon={faThumbtack}
              />
            )}
          </PinnedLabel>
          {hasChild ? (
            <IndentRoot depth={1}>
              <PinnedLabel>
                <TypeIcon icon="caret-right" />
                <TypeIcon icon={isFolder ? "folder" : "minus"} />
                <Label>
                  {label ? searchHighlight(label) : t("tooltip.placeholder")}
                </Label>
              </PinnedLabel>
            </IndentRoot>
          ) : null}
          {description && (
            <Description>{searchHighlight(description)}</Description>
          )}
        </Head>
        <Infos>
          {infos &&
            infos.map((info, i) => (
              <PieceOfInfo key={info.key + i}>
                <InfoHeadline>{searchHighlight(info.key)}</InfoHeadline>
                <Markdown
                  remarkPlugins={[remarkGfm]}
                  components={
                    {
                      // TODO: Won't work anymore with the latest react-markdown, because
                      // node is now a ReactElement, not a string.
                      // Try to use another package for highlighting that doesn't depend on a string
                      // or just highlight ourselves
                      // p: ({ node }) => searchHighlight(node)
                    }
                  }
                >
                  {info.value}
                </Markdown>
              </PieceOfInfo>
            ))}
        </Infos>
      </Content>
    </Root>
  );
};

export default Tooltip;
