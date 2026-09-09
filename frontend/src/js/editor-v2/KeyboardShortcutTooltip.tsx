import { Fragment, type ReactElement } from "react";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";

import { KeyboardKey } from "../common/components/KeyboardKey";
import { Tooltip, TooltipTrigger } from "../ui-components/Tooltip";

const keyTooltip = tv({
  base: ["flex items-center", "gap-[5px]"],
});

export const KeyboardShortcutTooltip = ({
  keyname,
  children,
}: {
  keyname: string;
  children: ReactElement;
}) => {
  const { t } = useTranslation();
  const keynames = keyname.split("+");

  return (
    <TooltipTrigger>
      {children}
      <Tooltip>
        <div className={keyTooltip()}>
          {t("common.shortcut")}:{" "}
          <div className="flex items-center gap-[2px]">
            {keynames.map((keyPart, i) => (
              <Fragment key={keyPart}>
                <KeyboardKey>{keyPart}</KeyboardKey>
                {i < keynames.length - 1 && "+"}
              </Fragment>
            ))}
          </div>
        </div>
      </Tooltip>
    </TooltipTrigger>
  );
};
