import { FormConfigT } from "./reducer";

const configHasTag = (config: FormConfigT, searchTerm: string) => {
  return (
    !!config.tags &&
    config.tags.some(tag => {
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

const configHasFilterType = (config: FormConfigT, filter: string) => {
  if (filter === "all") return true;

  // Checks config.own, config.shared or config.system
  if (config[filter]) return true;

  // Special case for a "system"-config:
  // it's simply not shared and not self-created (own)
  if (filter === "system" && !config.shared && !config.own) return true;

  return false;
};

export const selectFormConfigs = (
  formConfigs: FormConfigT[],
  search: string[],
  filter: string
) => {
  if (search.length === 0 && filter === "all") return formConfigs;

  return formConfigs.filter(config => {
    return (
      configHasFilterType(config, filter) &&
      search.every(searchTerm => {
        return (
          configHasId(config, searchTerm) ||
          configHasLabel(config, searchTerm) ||
          configHasTag(config, searchTerm)
        );
      })
    );
  });
};
