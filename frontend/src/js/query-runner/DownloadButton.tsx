import {
  DownloadIcon,
  FileArchiveIcon,
  FileBracesIcon,
  FileDownIcon,
  FileSpreadsheetIcon,
  FileTextIcon,
  type LucideIcon,
  SheetIcon,
} from "lucide-react";
import { type ReactNode, type Ref, useContext } from "react";
import { tv } from "tailwind-variants";
import type { ResultUrlWithLabel } from "../api/types";
import { AuthTokenContext } from "../authorization/AuthTokenProvider";
import { Button } from "../ui-components/Button";
import { Icon } from "../ui-components/Icon";
import { getEnding } from "./DownloadResultsDropdownButton";

const link = tv({ base: "leading-none" });

interface FileIcon {
  icon: LucideIcon;
  color?: string;
}

const fileTypeToFileIcon: Record<string, FileIcon> = {
  ZIP: { icon: FileArchiveIcon, color: "var(--color-filetype-zip)" },
  XLSX: { icon: FileSpreadsheetIcon, color: "var(--color-filetype-xlsx)" },
  PDF: { icon: FileTextIcon, color: "var(--color-filetype-pdf)" },
  CSV: { icon: SheetIcon, color: "var(--color-filetype-csv)" },
  JSON: { icon: FileBracesIcon, color: "var(--color-filetype-json)" },
};

export function getFileIcon(url: string): FileIcon {
  if (url.includes(".")) {
    const ext = getEnding(url);

    if (ext in fileTypeToFileIcon) {
      return fileTypeToFileIcon[ext];
    }
  }

  return { icon: FileDownIcon };
}

interface Props {
  resultUrl: ResultUrlWithLabel;
  className?: string;
  children?: ReactNode;
  simpleIcon?: boolean;
  showColoredIcon?: boolean;
}

const DownloadButton = ({
  ref,
  simpleIcon,
  resultUrl,
  className,
  children,
  showColoredIcon,
}: Props & { ref?: Ref<HTMLAnchorElement> }) => {
  const { authToken } = useContext(AuthTokenContext);

  const href = `${resultUrl.url}?access_token=${encodeURIComponent(authToken)}`;

  const { icon, color } = getFileIcon(resultUrl.url);

  return (
    <a href={href} className={link({ className })} ref={ref}>
      <Button intent="link">
        <Icon
          icon={simpleIcon ? DownloadIcon : icon}
          style={{ color: showColoredIcon ? color : undefined }}
        />
        {children}
      </Button>
    </a>
  );
};

export default DownloadButton;
