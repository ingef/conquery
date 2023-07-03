import styled from "@emotion/styled";

import { Heading4 } from "../../headings/Headings";

export const SmallHeading = styled(Heading4)`
  flex-shrink: 0;
  margin: 0;
  color: ${({ theme }) => theme.col.black};
  font-size: ${({ theme }) => theme.font.md};
`;
