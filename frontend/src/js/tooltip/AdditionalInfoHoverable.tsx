import type { ReactNode } from "react";
import { useDispatch } from "react-redux";
import { tv } from "tailwind-variants";
import type { ConceptT } from "../api/types";
import { isEmpty } from "../common/helpers/commonHelper";
import { getNodeIcon } from "../model/node";
import { displayAdditionalInfos, toggleAdditionalInfos } from "./actions";
import type { AdditionalInfosType } from "./reducer";

const hoverableRoot = tv({ base: "cursor-pointer" });

// Allowlist the data we pass (especially: don't pass all children)
const getAdditionalInfos = (
  node: ConceptT,
  root?: ConceptT,
): AdditionalInfosType => ({
  label: node.label,
  description: node.description,
  matchingEntries: node.matchingEntries,
  matchingEntities: node.matchingEntities,
  dateRange: node.dateRange,
  infos: node.additionalInfos,
  icon: getNodeIcon(node, {
    isStructNode: !root?.detailsAvailable,
  }),
  rootLabel: root?.label,
  rootIcon: root ? getNodeIcon(root) : undefined,
});

const AdditionalInfoHoverable = ({
  node,
  className,
  children,
  root,
}: {
  children: ReactNode;
  className?: string;
  node: ConceptT;
  root: ConceptT;
}) => {
  const dispatch = useDispatch();

  const onDisplayAdditionalInfos = () => {
    if (!node.additionalInfos && isEmpty(node.matchingEntries)) return;
    dispatch(
      displayAdditionalInfos({
        additionalInfos: getAdditionalInfos(node, root),
      }),
    );
  };

  const onToggleAdditionalInfos = () => {
    if (!node.additionalInfos && isEmpty(node.matchingEntries)) return;

    dispatch(toggleAdditionalInfos());
    dispatch(
      displayAdditionalInfos({
        additionalInfos: getAdditionalInfos(node, root),
      }),
    );
  };

  return (
    // biome-ignore lint/a11y/noStaticElementInteractions: TODO hover/click info area, not a button
    // biome-ignore lint/a11y/useKeyWithClickEvents: see above
    <div
      className={hoverableRoot({ className })}
      onMouseEnter={onDisplayAdditionalInfos}
      onClick={onToggleAdditionalInfos}
    >
      {children}
    </div>
  );
};

export default AdditionalInfoHoverable;
