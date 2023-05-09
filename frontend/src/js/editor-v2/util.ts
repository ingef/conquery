import { useMemo } from "react";
import { useTranslation } from "react-i18next";

import { ConnectionKind, Tree } from "./types";

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

export const useTranslatedConnection = (
  connection: ConnectionKind | undefined,
) => {
  const { t } = useTranslation();

  return useMemo(() => {
    if (connection === "and") {
      return t("editorV2.and");
    } else if (connection === "or") {
      return t("editorV2.or");
    } else if (connection === "before") {
      return t("editorV2.before");
    } else {
      return "";
    }
  }, [t, connection]);
};
