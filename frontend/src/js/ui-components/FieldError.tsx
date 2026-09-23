import { FieldError as RacFieldError } from "react-aria-components";
import { tv } from "tailwind-variants";

import { textStyle } from "./Typography";

const fieldError = tv({
  base: ["mt-1", textStyle({ size: 2, tone: "danger", strong: true })],
});

/** the error message below a field; react-aria renders it only while the field is invalid */
export const FieldError = ({ children }: { children?: string }) => (
  <RacFieldError className={fieldError()}>{children}</RacFieldError>
);
