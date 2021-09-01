import styled from "@emotion/styled";
import React, { FC, ReactElement } from "react";

import FaIcon from "../icon/FaIcon";

import WithTooltip from "./WithTooltip";

interface PropsT {
  text?: string;
  html?: ReactElement;
  className?: string;
  noIcon?: boolean;
}

const Root = styled(WithTooltip)`
  display: inline-block;
  padding: 0 7px;
`;

const InfoTooltip: FC<PropsT> = ({ className, text, html, noIcon }) => {
  return (
    <Root className={className} text={text} html={html}>
      {!noIcon && <FaIcon regular icon="question-circle" />}
    </Root>
  );
};

export default InfoTooltip;
