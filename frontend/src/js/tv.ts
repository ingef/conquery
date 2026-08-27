import { createTV } from "tailwind-variants";

/**
 * Every class list goes through this `tv`, never through the bare import:
 * the class merger classifies unknown `text-*` classes as colours, so a
 * custom font size would be dropped when a colour class is merged in.
 */
export const tv = createTV({
  twMergeConfig: {
    extend: {
      classGroups: {
        "font-size": ["text-tiny"],
      },
    },
  },
});
