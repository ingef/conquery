import { useTheme } from "@emotion/react";
import styled from "@emotion/styled";
import { IconProp } from "@fortawesome/fontawesome-svg-core";
import {
  faDownload,
  faFileArchive,
  faFileCsv,
  faFileDownload,
  faFileExcel,
  faFilePdf,
} from "@fortawesome/free-solid-svg-icons";
import { ReactNode, useContext, forwardRef, useMemo } from "react";

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

interface FileIcon {
  icon: IconProp;
  color?: string;
}

function useFileIcon(url: string): FileIcon {
  const theme = useTheme();

  const fileTypeToFileIcon: Record<string, FileIcon> = useMemo(
    () => ({
      ZIP: { icon: faFileArchive, color: theme.col.fileTypes.zip },
      XLSX: { icon: faFileExcel, color: theme.col.fileTypes.xlsx },
      PDF: { icon: faFilePdf, color: theme.col.fileTypes.pdf },
      CSV: { icon: faFileCsv, color: theme.col.fileTypes.csv },
    }),
    [theme],
  );

  if (url.includes(".")) {
    const ext = getEnding(url);

    if (ext in fileTypeToFileIcon) {
      return fileTypeToFileIcon[ext];
    }
  }

  return { icon: faFileDownload };
}

interface Props extends Omit<IconButtonPropsT, "icon" | "onClick"> {
  resultUrl: ResultUrlWithLabel;
  className?: string;
  children?: ReactNode;
  simpleIcon?: boolean;
  onClick?: () => void;
  showColoredIcon?: boolean;
}

const DownloadButton = forwardRef<HTMLAnchorElement, Props>(
  (
    {
      simpleIcon,
      resultUrl,
      className,
      children,
      onClick,
      showColoredIcon,
      ...restProps
    },
    ref,
  ) => {
    const { authToken } = useContext(AuthTokenContext);

    const href = `${resultUrl.url}?access_token=${encodeURIComponent(
      authToken,
    )}&charset=ISO_8859_1`;

    const { icon, color } = useFileIcon(resultUrl.url);

    return (
      <Link href={href} className={className} ref={ref}>
        <SxIconButton
          {...restProps}
          large
          icon={simpleIcon ? faDownload : icon}
          onClick={onClick}
          iconColor={showColoredIcon ? color : undefined}
        >
          {children}
        </SxIconButton>
      </Link>
    );
  },
);

export default DownloadButton;
