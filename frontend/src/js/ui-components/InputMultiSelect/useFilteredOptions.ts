import { useMemo } from "react";
import { useTranslation } from "react-i18next";

import type { SelectOptionT } from "../../api/types";
import { exists } from "../../common/helpers/exists";
import { optionMatchesQuery } from "../InputSelect/optionMatchesQuery";

export const useFilteredOptions = ({
  options,
  selectedItems,
  inputValue,
  creatable,
}: {
  options: SelectOptionT[];
  selectedItems: SelectOptionT[];
  inputValue: string;
  creatable?: boolean;
}) => {
  const { t } = useTranslation();

  return useMemo(() => {
    const creatableOption =
      creatable && inputValue.length > 0
        ? [
            {
              label: `${t("common.create")}: "${inputValue}"`,
              value: inputValue,
              disabled: false,
            },
          ]
        : [];

    const stillSelectable = (option: SelectOptionT) =>
      !exists(selectedItems.find((item) => item.value === option.value));

    const regularOptions = options.filter(
      (option) =>
        stillSelectable(option) && optionMatchesQuery(option, inputValue),
    );

    return [...creatableOption, ...regularOptions];
  }, [options, selectedItems, inputValue, creatable, t]);
};
