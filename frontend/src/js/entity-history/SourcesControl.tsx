import { memo } from "react";

import { SelectOptionT } from "../api/types";
import InputMultiSelect from "../ui-components/InputMultiSelect/InputMultiSelect";

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
  return (
    <div className={className}>
      <InputMultiSelect
        options={options}
        value={sourcesFilter}
        onChange={setSourcesFilter}
      />
    </div>
  );
};

export default memo(SourcesControl);
