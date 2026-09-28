import { type DOMAttributes, memo } from "react";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";
import { useAppTheme } from "../../app-theme-context";
import { C2 } from "../../ui-components/Typography";

import type { TreeChildrenTime } from "../types";
import {
  useGetNodeLabel,
  useGetTranslatedTimestamp,
  useTranslatedInterval,
  useTranslatedOperator,
} from "../util";

const container = tv({
  base: ["mx-auto", "inline-flex flex-col", "select-none"],
});

const row = tv({
  base: ["flex items-center", "gap-[5px]"],
});

export const TimeConnection = memo(
  ({
    conditions,
    onDoubleClick,
  }: {
    conditions: TreeChildrenTime;
    onDoubleClick: DOMAttributes<HTMLElement>["onDoubleClick"];
  }) => {
    const { t } = useTranslation();
    const { palette } = useAppTheme();
    const getNodeLabel = useGetNodeLabel();
    const getTranslatedTimestamp = useGetTranslatedTimestamp();

    const aTimestamp = getTranslatedTimestamp(conditions.timestamps[0]);
    const bTimestamp = getTranslatedTimestamp(conditions.timestamps[1]);
    const a = getNodeLabel(conditions.items[0]);
    const b = getNodeLabel(conditions.items[1]);
    const operator = useTranslatedOperator(conditions.operator);
    const interval = useTranslatedInterval(conditions.interval);

    return (
      // biome-ignore lint/a11y/noStaticElementInteractions: TODO double-click opens the time modal, emotion had hidden this
      <div className={container()} onDoubleClick={onDoubleClick}>
        <div className={row()}>
          <span style={{ color: palette[0] }}>
            <C2 as="span" strong>
              {aTimestamp}
            </C2>
          </span>
          <C2 as="span">{t("editorV2.dateRangeFrom")}</C2>
          <C2 as="span" strong tone="primary">
            {a}
          </C2>
        </div>
        <div className={row()}>
          {conditions.operator !== "WHILE" && (
            <span style={{ color: palette[1] }}>
              <C2 as="span" strong>
                {interval}
              </C2>
            </span>
          )}
          <span style={{ color: palette.at(-2) }}>
            <C2 as="span" strong>
              {operator}
            </C2>
          </span>
        </div>
        <div className={row()}>
          <span style={{ color: palette[0] }}>
            <C2 as="span" strong>
              {bTimestamp}
            </C2>
          </span>
          <C2 as="span">{t("editorV2.dateRangeFrom")}</C2>
          <C2 as="span" strong tone="primary">
            {b}
          </C2>
        </div>
      </div>
    );
  },
);
