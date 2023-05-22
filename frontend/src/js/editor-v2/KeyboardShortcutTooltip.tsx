import styled from "@emotion/styled";
import { Fragment, ReactElement } from "react";
import { useTranslation } from "react-i18next";

import { KeyboardKey } from "../common/components/KeyboardKey";
import WithTooltip from "../tooltip/WithTooltip";

const KeyTooltip = styled("div")`
  padding: 8px 15px;
  display: flex;
  align-items: center;
  gap: 5px;
`;

const Keys = styled("div")`
  display: flex;
  align-items: center;
  gap: 2px;
`;

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
        <KeyTooltip>
          {t("common.shortcut")}:{" "}
          <Keys>
            {keynames.map((keyPart, i) => (
              <Fragment key={keyPart}>
                <KeyboardKey>{keyPart}</KeyboardKey>
                {i < keynames.length - 1 && "+"}
              </Fragment>
            ))}
          </Keys>
        </KeyTooltip>
      }
    >
      {children}
    </WithTooltip>
  );
};
