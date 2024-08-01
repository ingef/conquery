import { DNDType } from "../common/constants/dndTypes";

export const EDITOR_DROP_TYPES = [
  DNDType.CONCEPT_TREE_NODE,
  DNDType.PREVIOUS_QUERY,
  DNDType.PREVIOUS_SECONDARY_ID_QUERY,
];

export const HOTKEYS = {
  expand: { keyname: "x" },
  negate: { keyname: "n" },
  editDates: { keyname: "d" },
  delete: { keyname: ["backspace", "del"] },
  flip: { keyname: "f" },
  rotateConnector: { keyname: "c" },
  editTimeConnection: { keyname: "t" },
  reset: { keyname: "shift+backspace" },
  editQueryNode: { keyname: "Enter" },
};
