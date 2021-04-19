import styled from "@emotion/styled";
import React, { FC } from "react";

import FaIcon from "../icon/FaIcon";

import WithTooltip from "./WithTooltip";

interface PropsT {
  text: string;
  className?: string;
  noIcon?: boolean;
}

const Root = styled(WithTooltip)`
  display: inline-block;
  padding: 0 10px;
`;

const InfoTooltip: FC<PropsT> = ({ className, text, noIcon }) => {
  return (
    <Root className={className} text={text}>
      {!noIcon && <FaIcon regular icon="question-circle" />}
    </Root>
  );
};

export default InfoTooltip;
