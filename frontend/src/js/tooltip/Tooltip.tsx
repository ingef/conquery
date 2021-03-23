import React from "react";
import styled from "@emotion/styled";
import type { Dispatch } from "redux-thunk";
import { useTranslation } from "react-i18next";
import { connect } from "react-redux";
import Markdown from "react-markdown/with-html";

import Highlighter from "react-highlight-words";

import IconButton from "../button/IconButton";
import FaIcon from "../icon/FaIcon";
import type { SearchT } from "../concept-trees/reducer";

import ActivateTooltip from "./ActivateTooltip";
import {
  toggleDisplayTooltip as toggleTooltip,
  toggleAdditionalInfos as toggleInfos,
} from "./actions";
import type { AdditionalInfosType } from "./reducer";
import TooltipEntries from "./TooltipEntries";

const Root = styled("div")`
  width: 100%;
  height: 100%;
  padding: 50px 0 10px;
  position: relative;
  display: flex;
  flex-direction: column;
  background: ${({ theme }) =>
    `linear-gradient(135deg, ${theme.col.bgAlt}, ${theme.col.bg});`};
`;

const Header = styled("h2")`
  background-color: white;
  height: 47px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  border-bottom: 1px solid #ccc;
  margin: 0 0 5px;
  padding: 0 20px;
  font-size: ${({ theme }) => theme.font.sm};
  letter-spacing: 1px;
  line-height: 38px;
  text-transform: uppercase;
  color: ${({ theme }) => theme.col.blueGrayDark};
`;
const Content = styled("div")`
  padding: 10px 20px;
  width: 100%;
  flex-grow: 1;
  overflow-y: auto;
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
  top: 58px;
  right: 0;
  border-right: 0;
  border-top-right-radius: 0;
  border-bottom-right-radius: 0;
`;

type PropsType = {
  additionalInfos: AdditionalInfosType;
  displayTooltip: boolean;
  toggleAdditionalInfos: boolean;
  onToggleDisplayTooltip: Function;
  onToggleAdditionalInfos: Function;
  search: SearchT;
};

const Tooltip = (props: PropsType) => {
  const { t } = useTranslation();
  if (!props.displayTooltip) return <ActivateTooltip />;

  const {
    additionalInfos,
    toggleAdditionalInfos,
    onToggleDisplayTooltip,
    onToggleAdditionalInfos,
  } = props;

  const {
    label,
    description,
    isFolder,
    infos,
    matchingEntries,
    dateRange,
  } = additionalInfos;

  const searchHighlight = (text) => {
    return (
      <Highlighter
        searchWords={props.search.words || []}
        autoEscape={true}
        textToHighlight={text || ""}
      />
    );
  };

  const renderers = {
    text: ({ value, children, nodeKey }) => searchHighlight(value),
  };

  return (
    <Root>
      <StyledIconButton
        small
        frame
        onClick={onToggleDisplayTooltip}
        icon="angle-left"
      />
      <Header>{t("tooltip.headline")}</Header>
      <Content>
        <TooltipEntries
          matchingEntries={matchingEntries}
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
                  escapeHtml={true}
                  renderers={renderers}
                  source={info.value}
                />
              </PieceOfInfo>
            ))}
        </Infos>
      </Content>
    </Root>
  );
};

const mapStateToProps = (state) => {
  return {
    additionalInfos: state.tooltip.additionalInfos,
    displayTooltip: state.tooltip.displayTooltip,
    toggleAdditionalInfos: state.tooltip.toggleAdditionalInfos,
    search: state.conceptTrees.search,
  };
};

const mapDispatchToProps = (dispatch: Dispatch) => ({
  onToggleDisplayTooltip: () => dispatch(toggleTooltip()),
  onToggleAdditionalInfos: () => dispatch(toggleInfos()),
});

export default connect(mapStateToProps, mapDispatchToProps)(Tooltip);
