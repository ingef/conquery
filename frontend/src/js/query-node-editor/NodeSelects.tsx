import { memo, useMemo } from "react";
import { useTranslation } from "react-i18next";

import { SelectOptionT, SelectorResultType } from "../api/types";
import { isSelectDisabled, sortSelects } from "../model/select";
import { SelectedSelectorT } from "../standard-query-editor/types";
import InputMultiSelect from "../ui-components/InputMultiSelect/InputMultiSelect";

import ContentCell from "./ContentCell";

interface Props {
  selects: SelectedSelectorT[];
  onSelectSelects: (value: SelectOptionT[]) => void;
  allowlistedSelects?: SelectorResultType[];
  blocklistedSelects?: SelectorResultType[];
}

const NodeSelects = ({
  selects,
  blocklistedSelects,
  allowlistedSelects,
  onSelectSelects,
}: Props) => {
  const { t } = useTranslation();
  const options = useMemo(
    () =>
      sortSelects(selects).map((select) => ({
        value: select.id,
        label: select.label,
        disabled: isSelectDisabled(select, {
          blocklistedSelects,
          allowlistedSelects,
        }),
      })),
    [selects, allowlistedSelects, blocklistedSelects],
  );

  const value = useMemo(
    () =>
      selects
        .filter(({ selected }) => !!selected)
        .map(({ id, label }) => ({ value: id, label: label })),
    [selects],
  );

  return (
    <ContentCell headline={t("queryNodeEditor.commonSelects")}>
      <InputMultiSelect
        onChange={onSelectSelects}
        value={value}
        options={options}
      />
    </ContentCell>
  );
};

export default memo(NodeSelects);
