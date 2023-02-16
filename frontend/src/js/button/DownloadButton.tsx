import styled from "@emotion/styled";
import { IconName } from "@fortawesome/fontawesome-svg-core";
import { ReactNode, useContext, forwardRef } from "react";

import { ResultUrlWithLabel } from "../api/types";
import { AuthTokenContext } from "../authorization/AuthTokenProvider";
import { getEnding } from "../query-runner/DownloadResultsDropdownButton";

import IconButton, { IconButtonPropsT } from "./IconButton";

const SxIconButton = styled(IconButton)`
  white-space: nowrap;
`;

const Link = styled("a")`
  line-height: 1;
`;

const fileTypeToIcon: Record<string, IconName> = {
  ZIP: "file-archive",
  XLSX: "file-excel",
  PDF: "file-pdf",
  CSV: "file-csv",
};
function getFileIcon(label: string): IconName {
  // Editor Requests
  if (label in fileTypeToIcon) {
    return fileTypeToIcon[label];
  }

  // Forms
  if (label.includes(".")) {
    const ext = getEnding(label);
    if (ext in fileTypeToIcon) {
      return fileTypeToIcon[ext];
    }
  }
  return "file-download";
}

interface Props extends Omit<IconButtonPropsT, "icon" | "onClick"> {
  resultUrl: ResultUrlWithLabel;
  className?: string;
  children?: ReactNode;
  onClick?: () => void;
}

const DownloadButton = forwardRef<HTMLAnchorElement, Props>(
  ({ resultUrl, className, children, onClick, ...restProps }, ref) => {
    const { authToken } = useContext(AuthTokenContext);

    const href = `${resultUrl.url}?access_token=${encodeURIComponent(
      authToken,
    )}&charset=ISO_8859_1`;

    return (
      <Link href={href} className={className} ref={ref}>
        <SxIconButton
          {...restProps}
          icon={getFileIcon(resultUrl.label)}
          onClick={onClick}
        >
          {children}
        </SxIconButton>
      </Link>
    );
  },
);

export default DownloadButton;
