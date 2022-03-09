import { exists } from "../../common/helpers/exists";
import { FormConfigT } from "../../previous-queries/list/reducer";

const configHasTag = (config: FormConfigT, searchTerm: string) => {
  return (
    !!config.tags &&
    config.tags.some((tag) => {
      return tag.toLowerCase().indexOf(searchTerm.toLowerCase()) !== -1;
    })
  );
};

const configHasLabel = (config: FormConfigT, searchTerm: string) => {
  return (
    config.label &&
    config.label.toLowerCase().indexOf(searchTerm.toLowerCase()) !== -1
  );
};

const configHasId = (config: FormConfigT, searchTerm: string) => {
  return config.id.toString() === searchTerm;
};

export const configHasFilterType = (
  config: FormConfigT,
  filter: string,
  { activeFormType }: { activeFormType: string | null },
) => {
  if (filter === "all") return true;

  if (filter === "activeForm")
    return !!activeFormType && config.formType === activeFormType;

  // Checks config.own, config.shared or config.system
  if (config[filter]) return true;

  // Special case for a "system"-config:
  // it's simply not shared and not self-created (own)
  if (filter === "system" && !config.shared && !config.own) return true;

  return false;
};

const configHasFolder = (config: FormConfigT, folder: string) => {
  return !!config.tags && config.tags.some((tag) => tag === folder);
};
const configMatchesFolderFilter = (
  config: FormConfigT,
  folders: string[],
  noFoldersActive: boolean,
) => {
  return noFoldersActive
    ? config.tags.length === 0
    : folders.every((folder) => configHasFolder(config, folder));
};

const configMatchesSearch = (config: FormConfigT, searchTerm: string | null) =>
  !exists(searchTerm) ||
  configHasId(config, searchTerm) ||
  configHasLabel(config, searchTerm) ||
  configHasTag(config, searchTerm);

export const selectFormConfigs = (
  formConfigs: FormConfigT[],
  searchTerm: string | null,
  filter: string,
  folders: string[],
  noFoldersActive: boolean,
) => {
  if ((!searchTerm || searchTerm.length === 0) && filter === "all") {
    return formConfigs;
  }

  // TODO: Implement
  const activeFormType = null;

  return formConfigs.filter((config) => {
    return (
      configMatchesFolderFilter(config, folders, noFoldersActive) &&
      configHasFilterType(config, filter, { activeFormType }) &&
      configMatchesSearch(config, searchTerm)
    );
  });
};
