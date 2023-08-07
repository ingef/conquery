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
    (timestamp: "every" | "some" | "earliest" | "latest") => {
      if (timestamp === "every") {
        return t("editorV2.every");
      } else if (timestamp === "some") {
        return t("editorV2.some");
      } else if (timestamp === "earliest") {
        return t("editorV2.earliest");
      } else if (timestamp === "latest") {
        return t("editorV2.latest");
      } else {
        return "";
      }
    },
    [t],
  );
};

export const useTranslatedOperator = (
  operator: "before" | "after" | "while",
) => {
  const { t } = useTranslation();

  if (operator === "before") {
    return t("editorV2.before");
  } else if (operator === "after") {
    return t("editorV2.after");
  } else if (operator === "while") {
    return t("editorV2.while");
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
