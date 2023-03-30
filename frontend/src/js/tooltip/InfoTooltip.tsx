import styled from "@emotion/styled";
import { faQuestionCircle } from "@fortawesome/free-regular-svg-icons";
import { FC, ReactElement } from "react";

import FaIcon from "../icon/FaIcon";

import WithTooltip from "./WithTooltip";

interface PropsT {
  text?: string;
  html?: ReactElement;
  className?: string;
  wide?: boolean;
}

const SxFaIcon = styled(FaIcon)`
  transition: ${({ theme }) => theme.transitionTime};
  &:hover {
    color: ${({ theme }) => theme.col.black};
  }
`;

const SpanContainer = styled("span")`
  display: inline-block;
  padding: 0 7px;
`;

const InfoTooltip: FC<PropsT> = ({ className, text, html, wide }) => {
  return (
    <WithTooltip text={text} html={html} wide={wide}>
      <SpanContainer className={className}>
        <SxFaIcon gray icon={faQuestionCircle} />
      </SpanContainer>
    </WithTooltip>
  );
};

export default InfoTooltip;
