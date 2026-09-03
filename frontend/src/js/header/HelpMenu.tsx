import {
  faBook,
  faInfoCircle,
  faPaperPlane,
  faQuestion,
} from "@fortawesome/free-solid-svg-icons";
import { DialogTrigger } from "react-aria-components";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";

import { useAbout } from "../app/About";
import IconButton from "../button/IconButton";
import { Dialog, Popover } from "../ui-components/Popover";

const list = tv({ base: ["flex flex-col", "gap-[2px]", "p-2"] });

interface Props {
  contactEmail?: string;
  manualUrl?: string;
}

export const HelpMenu = ({ contactEmail, manualUrl }: Props) => {
  const { t } = useTranslation();
  const { setOpen } = useAbout();

  return (
    <DialogTrigger>
      <IconButton
        className="px-3 py-[7px]"
        icon={faQuestion}
        frame
        data-test-id="help-menu"
      />
      <Popover placement="bottom end" offset={5}>
        <Dialog aria-label={t("common.help")}>
          {({ close }) => (
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
                  onClick={close}
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
                  onClick={close}
                >
                  {t("common.manual")}
                </IconButton>
              </a>
              <IconButton
                className="w-full"
                bgHover
                fixedIconWidth={14}
                icon={faInfoCircle}
                onClick={() => {
                  close();
                  setOpen(true);
                }}
              >
                {t("common.version")}
              </IconButton>
            </div>
          )}
        </Dialog>
      </Popover>
    </DialogTrigger>
  );
};
