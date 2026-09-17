import {
  EllipsisIcon,
  FolderIcon,
  FolderOpenIcon,
  type LucideIcon,
  MinusIcon,
} from "lucide-react";
import type { ClassValue } from "tailwind-variants";

import type { NodeIconT } from "../model/node";
import { Icon } from "../ui-components/Icon";

const icons: Record<NodeIconT, LucideIcon> = {
  leaf: MinusIcon,
  folder: FolderIcon,
  folderOpen: FolderOpenIcon,
  structFolder: FolderIcon,
  structFolderOpen: FolderOpenIcon,
  pending: EllipsisIcon,
};

// a concept's folder is filled, a folder that only structures the tree is an outline
const filled: NodeIconT[] = ["folder", "folderOpen"];

export const NodeIcon = ({
  icon,
  className,
}: {
  icon: NodeIconT;
  className?: ClassValue;
}) => (
  <Icon
    icon={icons[icon]}
    filled={filled.includes(icon)}
    className={className}
  />
);
