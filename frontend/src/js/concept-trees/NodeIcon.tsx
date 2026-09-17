import {
  faFolderOpen as faFolderOpenRegular,
  faFolder as faFolderRegular,
} from "@fortawesome/free-regular-svg-icons";
import {
  faEllipsisH,
  faFolder,
  faFolderOpen,
  faMinus,
  type IconDefinition,
} from "@fortawesome/free-solid-svg-icons";
import type { ClassValue } from "tailwind-variants";

import type { NodeIconT } from "../model/node";
import { Icon } from "../ui-components/Icon";

const icons: Record<NodeIconT, IconDefinition> = {
  leaf: faMinus,
  folder: faFolder,
  folderOpen: faFolderOpen,
  structFolder: faFolderRegular,
  structFolderOpen: faFolderOpenRegular,
  pending: faEllipsisH,
};

export const NodeIcon = ({
  icon,
  className,
}: {
  icon: NodeIconT;
  className?: ClassValue;
}) => <Icon icon={icons[icon]} className={className} />;
