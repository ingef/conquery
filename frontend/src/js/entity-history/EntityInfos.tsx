import { Fragment, memo } from "react";
import { tv } from "tailwind-variants";

import type { EntityInfo } from "../api/types";

const grid = tv({
  base: [
    "inline-grid grid-cols-[1fr_auto]",
    "gap-x-5 gap-y-0",
    "[place-items:center_start]",
  ],
});

const value = tv({
  base: ["text-sm", "font-normal"],
  variants: {
    blurred: { true: "blur-[6px]" },
  },
});

const EntityInfos = ({
  infos,
  blurred,
}: {
  infos: EntityInfo[];
  blurred?: boolean;
}) => {
  return (
    <div className={grid()}>
      {infos.map((info) => (
        <Fragment key={info.label}>
          <div className="text-sm">{info.label}</div>
          <div className={value({ blurred })}>{info.value}</div>
        </Fragment>
      ))}
    </div>
  );
};

export default memo(EntityInfos);
