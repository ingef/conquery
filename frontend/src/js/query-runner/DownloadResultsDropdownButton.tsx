import { faCaretDown, faDownload } from "@fortawesome/free-solid-svg-icons";
import { memo, useEffect, useMemo, useState } from "react";
import { tv } from "tailwind-variants";

import type { ResultUrlWithLabel } from "../api/types";
import DownloadButton from "../button/DownloadButton";
import IconButton from "../button/IconButton";
import WithTooltip from "../tooltip/WithTooltip";
import { getUserSettings, storeUserSettings } from "../user/userSettings";

const frame = tv({
  base: [
    "flex items-center justify-center",
    "rounded",
    "border border-gray-500",
    "transition-opacity duration-100",
  ],
  variants: {
    noborder: { true: "border-none" },
  },
});

const list = tv({
  base: ["flex flex-col", "gap-px", "p-2", "max-h-[60vh]", "overflow-y-auto"],
});

const downloadButton = tv({
  base: ["[&_button]:w-full", "[&_button]:px-[14px] [&_button]:py-2"],
});

const dropdownOpenButton = tv({ base: "px-2 py-[9px]" });

const separator = tv({ base: ["h-[33px] w-px", "bg-gray-500"] });

const popperOptions = {
  modifiers: [
    {
      name: "preventOverflow",
      options: {
        padding: 20,
      },
    },
  ],
};

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

  const dropdown = useMemo(() => {
    return (
      <div className={list()}>
        {resultUrls.map((resultUrl) => {
          const ending = getEnding(resultUrl.url);

          return (
            <DownloadButton
              className={downloadButton()}
              key={resultUrl.url}
              resultUrl={resultUrl}
              onClick={() => setFileChoice({ label: resultUrl.label, ending })}
              bgHover
              showColoredIcon
            >
              {truncate(resultUrl.label)}
            </DownloadButton>
          );
        })}
      </div>
    );
  }, [resultUrls]);

  return (
    <div className={frame({ noborder: tiny })}>
      {!tiny && (
        <>
          <DownloadButton
            className={downloadButton()}
            bgHover
            resultUrl={urlChoice}
            showColoredIcon
          >
            {truncChosenLabel}
          </DownloadButton>
          <div className={separator()} />
        </>
      )}
      <WithTooltip text={tooltip} hideOnClick>
        <WithTooltip
          html={dropdown}
          interactive
          arrow={false}
          trigger="click"
          popperOptions={popperOptions}
        >
          <IconButton
            className={dropdownOpenButton()}
            bgHover
            icon={tiny ? faDownload : faCaretDown}
          />
        </WithTooltip>
      </WithTooltip>
    </div>
  );
};

export default memo(DownloadResultsDropdownButton);
