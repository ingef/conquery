import styled from "@emotion/styled";
import { Dispatch, SetStateAction } from "react";
import { useTranslation } from "react-i18next";

import { SelectOptionT } from "../api/types";
import { exists } from "../common/helpers/exists";
import { Heading3 } from "../headings/Headings";
import InputMultiSelect from "../ui-components/InputMultiSelect/InputMultiSelect";

const HeadInfo = styled("div")`
  display: grid;
  grid-template-columns: auto 200px 1fr;
  gap: 15px;
  padding-left: 10px;
`;

const SxInputMultiSelect = styled(InputMultiSelect)`
  width: 200px;
`;

const SxHeading3 = styled(Heading3)`
  flex-shrink: 0;
  margin: 0;
`;
const Subtitle = styled("div")`
  font-size: ${({ theme }) => theme.font.xs};
  color: ${({ theme }) => theme.col.gray};
  margin-top: 5px;
`;
const EntityBadge = styled("div")`
  display: flex;
  gap: 5px;
`;
const Avatar = styled(SxHeading3)`
  color: ${({ theme }) => theme.col.gray};
  font-weight: 300;
`;

interface Props {
  className?: string;
  currentEntityIndex: number;
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
  className,
  currentEntityIndex,
  currentEntityId,
  totalEvents,
  entityStatusOptions,
  entityIdsStatus,
  setEntityIdsStatus,
}: Props) => {
  const { t } = useTranslation();
  return (
    <HeadInfo className={className}>
      <div>
        <EntityBadge>
          <Avatar>#{currentEntityIndex + 1}</Avatar>
          <SxHeading3>{currentEntityId}</SxHeading3>
        </EntityBadge>
        <Subtitle>
          {totalEvents} {t("history.events", { count: totalEvents })}
        </Subtitle>
      </div>
      <SxInputMultiSelect
        creatable
        placeholder={t("history.selectStatusPlaceholder")}
        onChange={(values) => {
          console.log("onChange", values);
          setEntityIdsStatus((curr) => ({
            ...curr,
            [currentEntityId]: values,
          }));
        }}
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
