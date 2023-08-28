import { css } from "@emotion/react";
import styled from "@emotion/styled";
import { forwardRef, memo } from "react";
import ReactMarkdown from "react-markdown";

import type { SelectOptionT } from "../../api/types";

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

  opacity: ${({ disabled }) => (disabled ? 0.4 : 1)};
  cursor: ${({ disabled }) => (disabled ? "not-allowed" : "pointer")};

  /* to style react-markdown */
  p {
    margin: 0;
  }
`;

interface StyleProps {
  active?: boolean;
  disabled?: boolean;
}

interface Props extends StyleProps {
  option: SelectOptionT;
}

const SelectListOption = forwardRef<HTMLDivElement, Props>(
  ({ option, ...props }, ref) => {
    const label = option.label || String(option.value);

    return (
      <Container {...props} disabled={option.disabled} ref={ref}>
        {option.displayLabel ? (
          option.displayLabel
        ) : (
          <ReactMarkdown>{label}</ReactMarkdown>
        )}
      </Container>
    );
  },
);

export default memo(SelectListOption);
