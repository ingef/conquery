import { FieldError as RacFieldError } from "react-aria-components";
import { tv } from "tailwind-variants";

const fieldError = tv({ base: ["mt-1", "text-sm font-bold text-red"] });

/** the error message below a field; react-aria renders it only while the field is invalid */
export const FieldError = ({ children }: { children?: string }) => (
  <RacFieldError className={fieldError()}>{children}</RacFieldError>
);
