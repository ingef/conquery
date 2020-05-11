import { FormConfigT } from "./reducer";
import { useSelector } from "react-redux";
import { StateT } from "app-types";
import { useActiveFormType } from "../stateSelectors";

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

const configHasFilterType = (
  config: FormConfigT,
  filter: string,
  { activeFormType }: { activeFormType: string | null }
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

export const useFilteredFormConfigs = () => {
  const formConfigs = useSelector<StateT, FormConfigT[]>(
    (state) => state.formConfigs.data
  );
  const search = useSelector<StateT, string[]>(
    (state) => state.formConfigsSearch
  );
  const filter = useSelector<StateT, string>(
    (state) => state.formConfigsFilter
  );

  const activeFormType = useActiveFormType();

  if (search.length === 0 && filter === "all") return formConfigs;

  return formConfigs.filter((config) => {
    return (
      configHasFilterType(config, filter, { activeFormType }) &&
      search.every((searchTerm) => {
        return (
          configHasId(config, searchTerm) ||
          configHasLabel(config, searchTerm) ||
          configHasTag(config, searchTerm)
        );
      })
    );
  });
};

const labelContainsAnySearch = (label: string, searches: string[]) =>
  searches.some(
    (search) => label.toLowerCase().indexOf(search.toLowerCase()) !== -1
  );

export const useIsLabelHighlighted = (label: string) => {
  const formConfigsSearch = useSelector<StateT, string[]>(
    (state) => state.formConfigsSearch
  );

  return labelContainsAnySearch(label, formConfigsSearch);
};
