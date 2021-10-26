import { useMemo } from "react";
import { useTranslation } from "react-i18next";

import type { SelectOptionT } from "../../api/types";
import { exists } from "../../common/helpers/exists";

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

    const matchesQuery = (option: SelectOptionT) => {
      const lowerInputValue = inputValue.toLowerCase();
      const lowerLabel = option.label.toLowerCase();

      return (
        lowerLabel.includes(lowerInputValue) ||
        String(option.value).toLowerCase().includes(lowerInputValue)
      );
    };

    const regularOptions = options.filter(
      (option) => stillSelectable(option) && matchesQuery(option),
    );

    return [...creatableOption, ...regularOptions];
  }, [options, selectedItems, inputValue, creatable, t]);
};
