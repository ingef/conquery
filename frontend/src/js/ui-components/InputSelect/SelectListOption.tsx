import { css } from "@emotion/react";
import styled from "@emotion/styled";
import { forwardRef } from "react";
import ReactMarkdown from "react-markdown";

import type { SelectOptionT } from "../../api/types";

interface StyleProps {
  active?: boolean;
  disabled?: boolean;
}
interface Props extends StyleProps {
  option: SelectOptionT;
}

const Container = styled("div")<StyleProps>`
  padding: 3px 8px;
  cursor: pointer;
  color: ${({ theme }) => theme.col.black};
  font-size: ${({ theme }) => theme.font.md};
  font-weight: 300;

  transition: background-color ${({ theme }) => theme.transitionTime};

  ${({ active, theme }) =>
    active &&
    css`
      background-color: ${theme.col.blueGrayVeryLight};
    `};
  ${({ disabled }) =>
    disabled &&
    css`
      opacity: 0.5;
      cursor: not-allowed;
    `};
`;

const Markdown = styled(ReactMarkdown)`
  p {
    margin: 0;
  }
`;

const SelectListOption = forwardRef<HTMLDivElement, Props>(
  ({ option, ...props }, ref) => {
    const label = option.label || option.value;

    return (
      <Container disabled={option.disabled} {...props} ref={ref}>
        <Markdown>{label}</Markdown>
      </Container>
    );
  },
);

export default SelectListOption;
