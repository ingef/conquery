import styled from "@emotion/styled";
import { Dispatch, SetStateAction } from "react";

import { SelectOptionT } from "../api/types";
import { exists } from "../common/helpers/exists";
import { Heading3 } from "../headings/Headings";
import InputMultiSelect from "../ui-components/InputMultiSelect/InputMultiSelect";

const HeadInfo = styled("div")`
  gap: 5px;
  padding: 0 10px;
`;

const SxHeading3 = styled(Heading3)`
  flex-shrink: 0;
  margin: 0;
`;

interface Props {
  currentEntityId: string;
  totalEvents: number;
  entityStatusOptions: SelectOptionT[];
  entityIdsStatus: { [id: string]: SelectOptionT[] };
  setEntityIdsStatus: Dispatch<
    SetStateAction<{
      [id: string]: SelectOptionT[];
    }>
  >;
}

export const EntityHeader = ({
  currentEntityId,
  totalEvents,
  entityStatusOptions,
  entityIdsStatus,
  setEntityIdsStatus,
}: Props) => {
  return (
    <HeadInfo>
      <SxHeading3>
        {currentEntityId} – {totalEvents} events
      </SxHeading3>
      <InputMultiSelect
        creatable
        onChange={(values) =>
          setEntityIdsStatus((curr) => ({
            ...curr,
            [currentEntityId]: values,
          }))
        }
        value={
          entityIdsStatus[currentEntityId]
            ?.map((val) =>
              entityStatusOptions.find((o) => o.value === val.value),
            )
            .filter(exists) || []
        }
        options={entityStatusOptions}
      />
    </HeadInfo>
  );
};
