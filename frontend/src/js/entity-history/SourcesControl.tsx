import { memo } from "react";
import { useTranslation } from "react-i18next";

import type { SelectOptionT } from "../api/types";
import { ComboBoxMultiField } from "../ui-components/ComboBoxMultiField";

interface Props {
  className?: string;
  options: SelectOptionT[];
  sourcesFilter: SelectOptionT[];
  setSourcesFilter: (value: SelectOptionT[]) => void;
}

const SourcesControl = ({
  className,
  options,
  sourcesFilter,
  setSourcesFilter,
}: Props) => {
  const { t } = useTranslation();

  return (
    <div className={className}>
      <ComboBoxMultiField
        aria-label={t("history.sources")}
        options={options}
        value={sourcesFilter}
        onChange={setSourcesFilter}
      />
    </div>
  );
};

export default memo(SourcesControl);
