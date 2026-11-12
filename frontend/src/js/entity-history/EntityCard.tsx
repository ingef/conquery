import { tv } from "tailwind-variants";

import type { EntityInfo, TimeStratifiedInfo } from "../api/types";
import { Card } from "../ui-components/Card";

import EntityInfos from "./EntityInfos";
import { TabbableTimeStratifiedInfos } from "./TabbableTimeStratifiedInfos";

const container = tv({
  base: ["grid grid-cols-2 items-center", "gap-[10px]"],
});

export const EntityCard = ({
  blurred,
  className,
  infos,
  timeStratifiedInfos,
}: {
  blurred?: boolean;
  className?: string;
  infos: EntityInfo[];
  timeStratifiedInfos: TimeStratifiedInfo[];
}) => {
  return (
    <div className={className}>
      <Card>
        <div className={container()}>
          <div className="flex flex-col items-start gap-[10px]">
            <EntityInfos blurred={blurred} infos={infos} />
          </div>
          {timeStratifiedInfos.length > 0 && (
            <TabbableTimeStratifiedInfos infos={timeStratifiedInfos} />
          )}
        </div>
      </Card>
    </div>
  );
};
