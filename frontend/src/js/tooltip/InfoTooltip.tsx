import styled from "@emotion/styled";
import React, { FC, ReactElement } from "react";

import FaIcon from "../icon/FaIcon";

import WithTooltip from "./WithTooltip";

interface PropsT {
  text?: string;
  html?: ReactElement;
  className?: string;
  noIcon?: boolean;
  wide?: boolean;
}

const SxWithTooltip = styled(WithTooltip)`
  display: inline-block;
  padding: 0 7px;
`;

const InfoTooltip: FC<PropsT> = ({ className, text, html, noIcon, wide }) => {
  return (
    <SxWithTooltip className={className} text={text} html={html} wide={wide}>
      {!noIcon && <FaIcon regular icon="question-circle" />}
    </SxWithTooltip>
  );
};

export default InfoTooltip;
