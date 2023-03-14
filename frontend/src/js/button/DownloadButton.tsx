import styled from "@emotion/styled";
import { IconProp } from "@fortawesome/fontawesome-svg-core";
import {
  faFileArchive,
  faFileCsv,
  faFileDownload,
  faFileExcel,
  faFilePdf,
} from "@fortawesome/free-solid-svg-icons";
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

const fileTypeToIcon: Record<string, IconProp> = {
  ZIP: faFileArchive,
  XLSX: faFileExcel,
  PDF: faFilePdf,
  CSV: faFileCsv,
};
function getFileIcon(url: string): IconProp {
  // Forms
  if (url.includes(".")) {
    const ext = getEnding(url);
    if (ext in fileTypeToIcon) {
      return fileTypeToIcon[ext];
    }
  }
  return faFileDownload;
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
          icon={getFileIcon(resultUrl.url)}
          onClick={onClick}
        >
          {children}
        </SxIconButton>
      </Link>
    );
  },
);

export default DownloadButton;
