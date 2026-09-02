import { Fragment, type ReactElement } from "react";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";

import { KeyboardKey } from "../common/components/KeyboardKey";
import WithTooltip from "../ui-components/WithTooltip";

const keyTooltip = tv({
  base: ["flex items-center", "gap-[5px]", "px-[15px] py-2"],
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
    <WithTooltip
      html={
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
      }
    >
      {children}
    </WithTooltip>
  );
};
