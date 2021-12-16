import styled from "@emotion/styled";
import { FC, ReactElement } from "react";

import FaIcon from "../icon/FaIcon";

import WithTooltip from "./WithTooltip";

interface PropsT {
  text?: string;
  html?: ReactElement;
  className?: string;
  noIcon?: boolean;
  wide?: boolean;
}

const SxFaIcon = styled(FaIcon)`
  transition: ${({ theme }) => theme.transitionTime};
  &:hover {
    color: ${({ theme }) => theme.col.black};
  }
`;

const SxWithTooltip = styled(WithTooltip)`
  display: inline-block;
  padding: 0 7px;
`;

const InfoTooltip: FC<PropsT> = ({ className, text, html, noIcon, wide }) => {
  return (
    <SxWithTooltip className={className} text={text} html={html} wide={wide}>
      {!noIcon && <SxFaIcon gray regular icon="question-circle" />}
    </SxWithTooltip>
  );
};

export default InfoTooltip;
