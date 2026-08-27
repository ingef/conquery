import styled from "@emotion/styled";
import { faQuestionCircle } from "@fortawesome/free-regular-svg-icons";
import type { ReactElement } from "react";

import FaIcon from "../icon/FaIcon";

import WithTooltip from "./WithTooltip";

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

const InfoTooltip = ({
  className,
  text,
  html,
  wide,
}: {
  text?: string;
  html?: ReactElement;
  className?: string;
  wide?: boolean;
}) => {
  return (
    <WithTooltip text={text} html={html} wide={wide}>
      <SpanContainer className={className}>
        <SxFaIcon gray icon={faQuestionCircle} />
      </SpanContainer>
    </WithTooltip>
  );
};

export default InfoTooltip;
