import { Tree } from "./types";

export const findNodeById = (tree: Tree, id: string): Tree | undefined => {
  if (tree.id === id) {
    return tree;
  }
  if (tree.children) {
    for (const child of tree.children.items) {
      const found = findNodeById(child, id);
      if (found) {
        return found;
      }
    }
  }
  return undefined;
};
