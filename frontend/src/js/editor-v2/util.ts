import { useCallback } from "react";
import { useTranslation } from "react-i18next";

import { ConnectionKind, Tree, TreeChildrenTime } from "./types";

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

const getNodeLabel = (
  node: Tree,
  getTranslatedConnection: ReturnType<typeof useGetTranslatedConnection>,
): string => {
  if (node.data?.label) {
    return node.data.label;
  } else if (node.children) {
    return node.children.items
      .map((n) => getNodeLabel(n, getTranslatedConnection))
      .join(" " + getTranslatedConnection(node.children.connection) + " ");
  } else {
    return "";
  }
};

export const useGetNodeLabel = (): ((node: Tree) => string) => {
  const getTranslatedConnection = useGetTranslatedConnection();

  return useCallback(
    (node: Tree) => getNodeLabel(node, getTranslatedConnection),
    [getTranslatedConnection],
  );
};

export const useGetTranslatedConnection = () => {
  const { t } = useTranslation();

  return useCallback(
    (connection: ConnectionKind | undefined) => {
      if (connection === "and") {
        return t("editorV2.and");
      } else if (connection === "or") {
        return t("editorV2.or");
      } else if (connection === "time") {
        return t("editorV2.time");
      } else {
        return "";
      }
    },
    [t],
  );
};

export const useGetTranslatedTimestamp = () => {
  const { t } = useTranslation();

  return useCallback(
    (timestamp: "ALL" | "ANY" | "EARLIEST" | "LATEST") => {
      if (timestamp === "ALL") {
        return t("editorV2.ALL");
      } else if (timestamp === "ANY") {
        return t("editorV2.ANY");
      } else if (timestamp === "EARLIEST") {
        return t("editorV2.EARLIEST");
      } else if (timestamp === "LATEST") {
        return t("editorV2.LATEST");
      } else {
        return "";
      }
    },
    [t],
  );
};

export const useTranslatedOperator = (
  operator: "BEFORE" | "AFTER" | "WHILE",
) => {
  const { t } = useTranslation();

  if (operator === "BEFORE") {
    return t("editorV2.BEFORE");
  } else if (operator === "AFTER") {
    return t("editorV2.AFTER");
  } else if (operator === "WHILE") {
    return t("editorV2.WHILE");
  }
};

export const useTranslatedInterval = (
  interval: TreeChildrenTime["interval"],
) => {
  const { t } = useTranslation();

  if (!interval) return t("editorV2.intervalSome");

  const { min, max } = interval;

  if (min === null && max === null) return t("editorV2.intervalSome");
  if (min !== null && max === null)
    return t("editorV2.intervalMinDays", { days: min });
  if (min === null && max !== null)
    return t("editorV2.intervalMaxDays", { days: max });
  if (min !== null && max !== null)
    return t("editorV2.intervalMinMaxDays", { minDays: min, maxDays: max });

  return t("editorV2.intervalSome");
};
