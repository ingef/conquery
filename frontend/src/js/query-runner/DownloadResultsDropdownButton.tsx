import { faCaretDown, faDownload } from "@fortawesome/free-solid-svg-icons";
import { memo, useContext, useEffect, useMemo, useState } from "react";
import { MenuTrigger, Button as RacButton } from "react-aria-components";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";
import type { ResultUrlWithLabel } from "../api/types";
import { AuthTokenContext } from "../authorization/AuthTokenProvider";
import { getFileIcon } from "../button/DownloadButton";
import { Icon } from "../ui-components/Icon";
import { Menu, MenuItem } from "../ui-components/Menu";
import { Tooltip, TooltipTrigger } from "../ui-components/Tooltip";
import { getUserSettings, storeUserSettings } from "../user/userSettings";

// a split button: the chosen format downloads, the caret opens the list
const frame = tv({
  base: ["inline-flex items-stretch", "h-[30px]", "rounded", "overflow-hidden"],
  variants: {
    bordered: { true: "border border-gray-500" },
  },
});

const part = tv({
  base: [
    "inline-flex items-center",
    "gap-[10px]",
    "h-full",
    "text-sm font-medium text-gray-800 whitespace-nowrap",
    "cursor-pointer",
    "hover:bg-gray-50",
  ],
  variants: {
    caret: { true: "px-2", false: "px-[14px]" },
  },
});

const separator = tv({ base: ["w-px self-stretch", "bg-gray-500"] });

interface FileChoice {
  label: string;
  ending: string;
}

export const getEnding = (url: string) =>
  url.split(".").reverse()[0].toUpperCase();

function getResultUrl(
  resultUrls: ResultUrlWithLabel[],
  fileChoice: FileChoice,
): ResultUrlWithLabel {
  return (
    resultUrls.find(({ label }) => label === fileChoice.label) ||
    resultUrls.find(({ url }) => getEnding(url) === fileChoice.ending) ||
    resultUrls[0]
  );
}

function truncate(label: string) {
  return label.length > 40 ? `${label.slice(0, 37)}…` : label;
}

const getInitialEndingChoice = (resultUrls: ResultUrlWithLabel[]) => {
  const { preferredDownloadEnding: ending, preferredDownloadLabel: label } =
    getUserSettings();
  return getResultUrl(resultUrls, { label: label || "", ending: ending || "" });
};

const DownloadResultsDropdownButton = ({
  resultUrls,
  tiny,
  tooltip,
}: {
  resultUrls: ResultUrlWithLabel[];
  tiny?: boolean;
  tooltip?: string;
}) => {
  const { t } = useTranslation();
  const { authToken } = useContext(AuthTokenContext);
  const [fileChoice, setFileChoice] = useState<FileChoice>(() => {
    const initial = getInitialEndingChoice(resultUrls);
    return { label: initial.label, ending: getEnding(initial.url) };
  });

  useEffect(() => {
    storeUserSettings({
      preferredDownloadEnding: fileChoice.ending,
      preferredDownloadLabel: fileChoice.label,
    });
  }, [fileChoice]);

  const urlChoice = useMemo(() => {
    return getResultUrl(resultUrls, fileChoice);
  }, [resultUrls, fileChoice]);

  const truncChosenLabel = useMemo(() => {
    return truncate(fileChoice.label);
  }, [fileChoice]);

  return (
    <div className={frame({ bordered: !tiny })}>
      {!tiny && (
        <>
          <a
            href={`${urlChoice.url}?access_token=${encodeURIComponent(authToken)}`}
          >
            <RacButton className={part({ caret: false })}>
              <Icon
                icon={getFileIcon(urlChoice.url).icon}
                style={{ color: getFileIcon(urlChoice.url).color }}
              />
              {truncChosenLabel}
            </RacButton>
          </a>
          <div className={separator()} />
        </>
      )}
      <TooltipTrigger>
        <MenuTrigger>
          <RacButton aria-label={tooltip} className={part({ caret: true })}>
            <Icon icon={tiny ? faDownload : faCaretDown} />
          </RacButton>
          <Menu
            aria-label={t("previousQuery.downloadResults")}
            onAction={(key) => {
              const chosen = resultUrls.find(({ url }) => url === key);
              if (chosen) {
                setFileChoice({
                  label: chosen.label,
                  ending: getEnding(chosen.url),
                });
              }
            }}
          >
            {resultUrls.map((resultUrl) => {
              const { icon, color } = getFileIcon(resultUrl.url);

              return (
                <MenuItem
                  key={resultUrl.url}
                  id={resultUrl.url}
                  href={`${resultUrl.url}?access_token=${encodeURIComponent(authToken)}`}
                  textValue={resultUrl.label}
                >
                  <Icon icon={icon} style={{ color }} />
                  {truncate(resultUrl.label)}
                </MenuItem>
              );
            })}
          </Menu>
        </MenuTrigger>
        <Tooltip>{tooltip}</Tooltip>
      </TooltipTrigger>
    </div>
  );
};

export default memo(DownloadResultsDropdownButton);
