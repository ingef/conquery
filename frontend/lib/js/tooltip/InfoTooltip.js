// @flow

import React from "react";
import ReactTooltip from "react-tooltip";
import styled from "@emotion/styled";

import FaIcon from "../icon/FaIcon";

type PropsType = {
  text: string,
  className?: string,
  noIcon?: boolean,
  place?: string
};

const Root = styled("div")`
  display: inline-block;
  padding: 0 10px;

  div[data-id="tooltip"] {
    text-transform: initial;
  }
`;

const InfoTooltip = ({ className, text, noIcon, place }: PropsType) => {
  return (
    <Root className={className}>
      {!noIcon && <FaIcon data-tip={text} icon="question-circle-o" />}
      <ReactTooltip place={place} type="info" effect="solid" multiline={true} />
    </Root>
  );
};

InfoTooltip.defaultProps = {
  place: "right"
};

export default InfoTooltip;
