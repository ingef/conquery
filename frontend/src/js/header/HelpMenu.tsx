import {
  faBook,
  faInfoCircle,
  faPaperPlane,
  faQuestion,
} from "@fortawesome/free-solid-svg-icons";
import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";

import { useAbout } from "../app/About";
import IconButton from "../button/IconButton";
import WithTooltip from "../tooltip/WithTooltip";

const list = tv({ base: ["flex flex-col", "gap-[2px]", "p-2"] });

interface Props {
  contactEmail?: string;
  manualUrl?: string;
}

// Skidding makes Dropdown align the right edge with the button,
// might need to adjust this when adding more content.
const dropdownOffset: [number, number] = [-47, 5]; // [skidding, distance] / default [0, 10]

export const HelpMenu = ({ contactEmail, manualUrl }: Props) => {
  const { t } = useTranslation();
  const { setOpen } = useAbout();

  const Dropdown = useMemo(
    () => (
      <div className={list()}>
        <a
          href={`mailto:${contactEmail}`}
          rel="noopener noreferrer"
          data-test-id="help-email"
        >
          <IconButton
            className="w-full"
            bgHover
            fixedIconWidth={14}
            icon={faPaperPlane}
          >
            {t("common.contact")}
          </IconButton>
        </a>
        <a
          href={manualUrl}
          target="_blank"
          rel="noopener noreferrer"
          data-test-id="help-manual"
        >
          <IconButton
            className="w-full"
            bgHover
            fixedIconWidth={14}
            icon={faBook}
          >
            {t("common.manual")}
          </IconButton>
        </a>
        <IconButton
          className="w-full"
          bgHover
          fixedIconWidth={14}
          icon={faInfoCircle}
          onClick={() => setOpen(true)}
        >
          {t("common.version")}
        </IconButton>
      </div>
    ),
    [t, manualUrl, contactEmail, setOpen],
  );
  return (
    <WithTooltip
      interactive
      trigger="click"
      arrow={false}
      html={Dropdown}
      offset={dropdownOffset}
      hideOnClick
    >
      <IconButton
        className="px-3 py-[7px]"
        icon={faQuestion}
        frame
        data-test-id="help-menu"
      />
    </WithTooltip>
  );
};
