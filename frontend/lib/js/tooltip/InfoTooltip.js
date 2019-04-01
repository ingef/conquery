// @flow

import React from "react";
import styled from "@emotion/styled";

import FaIcon from "../icon/FaIcon";

import WithTooltip from "./WithTooltip";

type PropsType = {
  text: string,
  className?: string,
  noIcon?: boolean,
  place?: string
};

const Root = styled(WithTooltip)`
  display: inline-block;
  padding: 0 10px;
`;

const InfoTooltip = ({ className, text, noIcon, place }: PropsType) => {
  return (
    <Root className={className} place={place || "right"}>
      {!noIcon && <FaIcon data-tip={text} icon="question-circle-o" />}
    </Root>
  );
};

export default InfoTooltip;
