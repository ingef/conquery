import { Fragment, memo } from "react";
import { tv } from "tailwind-variants";
import type { EntityInfo } from "../api/types";
import { C2 } from "../ui-components/Typography";

const grid = tv({
  base: [
    "inline-grid grid-cols-[1fr_auto]",
    "gap-x-5 gap-y-0",
    "[place-items:center_start]",
  ],
});

const value = tv({
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
          <C2>{info.label}</C2>
          <div className={value({ blurred })}>
            <C2>{info.value}</C2>
          </div>
        </Fragment>
      ))}
    </div>
  );
};

export default memo(EntityInfos);
