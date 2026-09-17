import {
  EllipsisIcon,
  FolderIcon,
  FolderOpenIcon,
  type LucideIcon,
  MinusIcon,
} from "lucide-react";

import type { NodeIconT } from "../model/node";

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
  className?: string;
}) => {
  const NodeTypeIcon = icons[icon];

  return (
    <NodeTypeIcon data-filled={filled.includes(icon)} className={className} />
  );
};
