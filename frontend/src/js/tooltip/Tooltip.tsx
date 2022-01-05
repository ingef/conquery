import styled from "@emotion/styled";
import { StateT } from "app-types";
import Highlighter from "react-highlight-words";
import { useTranslation } from "react-i18next";
import Markdown from "react-markdown";
import { useDispatch, useSelector } from "react-redux";

import IconButton from "../button/IconButton";
import type { SearchT } from "../concept-trees/reducer";
import FaIcon from "../icon/FaIcon";

import ActivateTooltip from "./ActivateTooltip";
import TooltipEntries from "./TooltipEntries";
import {
  toggleDisplayTooltip as toggleTooltip,
  toggleAdditionalInfos as toggleInfos,
} from "./actions";
import type { AdditionalInfosType } from "./reducer";

const Root = styled("div")`
  width: 100%;
  height: 100%;
  padding: 40px 0 10px;
  position: relative;
  display: flex;
  flex-direction: column;
  background: ${({ theme }) =>
    `linear-gradient(135deg, ${theme.col.bgAlt}, ${theme.col.bg});`};
`;

const Header = styled("h2")`
  background-color: white;
  height: 40px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  border-bottom: 1px solid #ccc;
  margin: 0;
  padding: 0 20px;
  font-size: ${({ theme }) => theme.font.sm};
  letter-spacing: 1px;
  line-height: 38px;
  text-transform: uppercase;
  color: ${({ theme }) => theme.col.blueGrayDark};
`;
const Content = styled("div")`
  padding: 12px 20px 10px;
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
  margin-right: 10px;
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
  line-height: 1;
  text-transform: uppercase;
`;

const Infos = styled("div")`
  width: 100%;
  overflow-x: auto;
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

const StyledIconButton = styled(IconButton)`
  position: absolute;
  padding: 6px 15px;
  top: 45px;
  right: 0;
  border-right: 0;
  border-top-right-radius: 0;
  border-bottom-right-radius: 0;
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
  const onToggleDisplayTooltip = () => dispatch(toggleTooltip());
  const onToggleAdditionalInfos = () => dispatch(toggleInfos());

  if (!displayTooltip) return <ActivateTooltip />;

  const {
    label,
    description,
    isFolder,
    infos,
    matchingEntries,
    matchingEntities,
    dateRange,
  } = additionalInfos;

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
      <StyledIconButton
        frame
        onClick={onToggleDisplayTooltip}
        icon="angle-left"
      />
      <Header>{t("tooltip.headline")}</Header>
      <Content>
        <TooltipEntries
          matchingEntries={matchingEntries}
          matchingEntities={matchingEntities}
          dateRange={dateRange}
        />
        <Head>
          <PinnedLabel>
            <TypeIcon icon={isFolder ? "folder" : "minus"} />
            <Label>
              {label ? searchHighlight(label) : t("tooltip.placeholder")}
            </Label>
            {toggleAdditionalInfos && (
              <TackIconButton
                bare
                active
                onClick={onToggleAdditionalInfos}
                icon="thumbtack"
              />
            )}
          </PinnedLabel>
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
