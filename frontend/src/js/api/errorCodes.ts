import { TFunction } from "react-i18next";
import { getExternalSupportedErrorMessage } from "../environment";

export function getErrorMessage(
  t: TFunction,
  code: string,
  context?: Record<string, string>
): string | null {
  const externalErrorMessage = getExternalSupportedErrorMessage(
    t,
    code,
    context
  );

  return externalErrorMessage || null;
}
