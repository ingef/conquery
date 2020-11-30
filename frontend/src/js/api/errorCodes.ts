import { getExternalSupportedErrorCodes } from "../environment";

export type SupportedErrorCodesT =
  | "EXAMPLE_ERROR"
  | "EXAMPLE_ERROR_INTERPOLATED";

const SUPPORTED_ERROR_CODES = new Map<SupportedErrorCodesT, string>([
  ["EXAMPLE_ERROR", "errorCodes.EXAMPLE_ERROR"],
  ["EXAMPLE_ERROR_INTERPOLATED", "errorCodes.EXAMPLE_ERROR_INTERPOLATED"],
]);

export function getErrorCodeMessageKey(
  code: SupportedErrorCodesT
): string | null {
  const externalErrorCodes = getExternalSupportedErrorCodes();

  return externalErrorCodes[code] || SUPPORTED_ERROR_CODES.get(code) || null;
}
