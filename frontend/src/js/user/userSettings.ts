// Putting localStorage into an own constant to type annotate it.
const localStorage: Storage = window.localStorage;

interface UserSettings {
  showEditorV2: boolean;
  arePreviousQueriesFoldersOpen: boolean;
  preferredDownloadEnding?: string; // Usually CSV or XLSX
  preferredDownloadLabel?: string; // Label of the preferred Download format (e.g. "All files")
}

const initialState: UserSettings = {
  showEditorV2: false,
  arePreviousQueriesFoldersOpen: false,
  preferredDownloadEnding: undefined,
  preferredDownloadLabel: undefined,
};

export const getUserSettings = (): UserSettings => {
  const storedSettings = localStorage.getItem("userSettings");

  return storedSettings ? JSON.parse(storedSettings) : initialState;
};

export const storeUserSettings = (
  nextSettings: Partial<UserSettings>,
): void => {
  const settings = getUserSettings();

  localStorage.setItem(
    "userSettings",
    JSON.stringify({ ...settings, ...nextSettings }),
  );
};
