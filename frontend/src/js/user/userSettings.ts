// Putting localStorage into an own constant to type annotate it.
const localStorage: Storage = window.localStorage;

interface UserSettings {
  arePreviousQueriesFoldersOpen: boolean;
  preferredDownloadEnding?: string; // Usually CSV or XLSX
  preferredDownloadLabel?: string; // Label of the prefered Download format (e.g. "All files")
}

const initialState: UserSettings = {
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
