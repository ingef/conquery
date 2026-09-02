import { type DOMAttributes, memo } from "react";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";
import { useAppTheme } from "../../app-theme-context";

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
  base: ["flex items-center", "gap-[5px]", "text-sm"],
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
          <span className="font-bold" style={{ color: palette[0] }}>
            {aTimestamp}
          </span>
          <span>{t("editorV2.dateRangeFrom")}</span>
          <span className="font-bold text-primary-500">{a}</span>
        </div>
        <div className={row()}>
          {conditions.operator !== "WHILE" && (
            <span className="font-bold" style={{ color: palette[1] }}>
              {interval}
            </span>
          )}
          <span className="font-bold" style={{ color: palette.at(-2) }}>
            {operator}
          </span>
        </div>
        <div className={row()}>
          <span className="font-bold" style={{ color: palette[0] }}>
            {bTimestamp}
          </span>
          <span>{t("editorV2.dateRangeFrom")}</span>
          <span className="font-bold text-primary-500">{b}</span>
        </div>
      </div>
    );
  },
);
