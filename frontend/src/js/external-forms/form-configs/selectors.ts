import { exists } from "../../common/helpers/exists";
import type { ProjectItemsFilterStateT } from "../../previous-queries/filter/reducer";
import { FormConfigT } from "../../previous-queries/list/reducer";

const configHasOwner = (config: FormConfigT, searchTerm: string) => {
  return (
    !!config.ownerName &&
    config.ownerName.toLowerCase().indexOf(searchTerm.toLowerCase()) !== -1
  );
};

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
  filter: ProjectItemsFilterStateT,
) => {
  if (filter === "all") return true;

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

export const configMatchesSearch = (
  config: FormConfigT,
  searchTerm: string | null,
) =>
  !exists(searchTerm) ||
  configHasId(config, searchTerm) ||
  configHasLabel(config, searchTerm) ||
  configHasTag(config, searchTerm) ||
  configHasOwner(config, searchTerm);

export const selectFormConfigs = (
  formConfigs: FormConfigT[],
  searchTerm: string | null,
  filter: ProjectItemsFilterStateT,
  folders: string[],
  noFoldersActive: boolean,
) => {
  const noFilterSet =
    (!searchTerm || searchTerm.length === 0) &&
    filter === "all" &&
    folders.length === 0 &&
    !noFoldersActive;

  if (noFilterSet) {
    return formConfigs;
  }

  return formConfigs.filter(
    (config) =>
      configHasFilterType(config, filter) &&
      configMatchesFolderFilter(config, folders, noFoldersActive) &&
      configMatchesSearch(config, searchTerm),
  );
};
