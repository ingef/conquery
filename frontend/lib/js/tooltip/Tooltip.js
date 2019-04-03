// @flow

import React from "react";
import styled from "@emotion/styled";
import type { Dispatch } from "redux-thunk";
import T from "i18n-react";
import { connect } from "react-redux";
import Markdown from "react-markdown";
import Highlighter from "react-highlight-words";

import IconButton from "../button/IconButton";
import type { SearchType } from "../category-trees/reducer";

import ActivateTooltip from "./ActivateTooltip";
import { toggleDisplayTooltip } from "./actions";
import type { AdditionalInfosType } from "./reducer";
import TooltipEntries from "./TooltipEntries";

type PropsType = {
  additionalInfos: AdditionalInfosType,
  displayTooltip: boolean,
  toggleAdditionInfos: boolean,
  toggleDisplayTooltip: Function,
  search: SearchType
};

const StyledIconButton = styled(IconButton)`
  position: absolute;
  top: 0;
  right: 20px;
  border-top: 0;
  border-top-left-radius: 0;
  border-top-right-radius: 0;
`;

const Tooltip = (props: PropsType) => {
  if (!props.displayTooltip) return <ActivateTooltip />;

  const { additionalInfos, toggleDisplayTooltip, toggleAdditionInfos } = props;
  const {
    label,
    description,
    infos,
    matchingEntries,
    dateRange
  } = additionalInfos;

  const searchHighlight = text => {
    return (
      <Highlighter
        searchWords={props.search.words || []}
        autoEscape={true}
        textToHighlight={text || ""}
      />
    );
  };

  return (
    <div className="tooltip">
      {toggleAdditionInfos && <i className="tooltip__tack fa fa-thumb-tack" />}
      <div className="tooltip__left">
        <div>
          {!label && !description && (
            <p className="tooltip__placeholder">
              {T.translate("tooltip.placeholder")}
            </p>
          )}
          <h3 className="tooltip__headline">
            {searchHighlight(label)}{" "}
            {description && <span> - {searchHighlight(description)}</span>}
          </h3>
          {infos &&
            infos.map((info, i) => (
              <div className="tooltip-info" key={i}>
                <h3 className="tooltip-info__key">
                  {searchHighlight(info.key)}
                </h3>
                <Markdown
                  className="tooltip-info__value"
                  source={info.value}
                  escapeHtml={true}
                  renderers={{ text: searchHighlight }}
                />
              </div>
            ))}
        </div>
      </div>
      <TooltipEntries
        className="tooltip__right"
        matchingEntries={matchingEntries}
        dateRange={dateRange}
      />
      <StyledIconButton
        small
        frame
        onClick={toggleDisplayTooltip}
        icon="angle-down"
      />
    </div>
  );
};

const mapStateToProps = state => {
  return {
    additionalInfos: state.tooltip.additionalInfos,
    displayTooltip: state.tooltip.displayTooltip,
    toggleAdditionInfos: state.tooltip.toggleAdditionInfos,
    search: state.categoryTrees.search
  };
};

const mapDispatchToProps = (dispatch: Dispatch) => ({
  toggleDisplayTooltip: () => dispatch(toggleDisplayTooltip())
});

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(Tooltip);
