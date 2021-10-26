import styled from "@emotion/styled";
import { StateT } from "app-types";
import { FC, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";

import { SecondaryId } from "../api/types";
import { exists } from "../common/helpers/exists";
import FaIcon from "../icon/FaIcon";
import ToggleButton from "../ui-components/ToggleButton";

import { setSelectedSecondaryId } from "./actions";
import type { StandardQueryStateT } from "./queryReducer";
import type { SelectedSecondaryIdStateT } from "./selectedSecondaryIdReducer";

const Headline = styled.h3<{ active?: boolean }>`
  font-size: ${({ theme }) => theme.font.sm};
  margin: 0;
  text-transform: uppercase;
  transition: color ${({ theme }) => theme.transitionTime};
  color: ${({ theme, active }) =>
    active ? theme.col.blueGrayDark : theme.col.gray};
`;

const SxFaIcon = styled(FaIcon)<{ active?: boolean }>`
  transition: color ${({ theme }) => theme.transitionTime};
  color: ${({ theme, active }) =>
    active ? theme.col.blueGrayDark : theme.col.gray};
`;

const SecondaryIdSelector: FC = () => {
  const { t } = useTranslation();
  const query = useSelector<StateT, StandardQueryStateT>(
    (state) => state.queryEditor.query,
  );

  const selectedSecondaryId = useSelector<StateT, SelectedSecondaryIdStateT>(
    (state) => state.queryEditor.selectedSecondaryId,
  );
  const loadedSecondaryIds = useSelector<StateT, SecondaryId[]>(
    (state) => state.conceptTrees.secondaryIds,
  );

  const dispatch = useDispatch();
  const onSetSelectedSecondaryId = (id: string | null) => {
    dispatch(
      setSelectedSecondaryId({ secondaryId: id === "standard" ? null : id }),
    );
  };

  const availableSecondaryIds = Array.from(
    new Set(
      query.flatMap((group) =>
        group.elements.flatMap((el) => {
          if (el.isPreviousQuery) {
            return el.availableSecondaryIds || [];
          } else {
            return el.tables
              .filter((table) => !table.exclude)
              .flatMap((table) => table.supportedSecondaryIds)
              .filter(exists);
          }
        }),
      ),
    ),
  )
    .map((id) => loadedSecondaryIds.find((secId) => secId.id === id))
    .filter(exists);

  useEffect(() => {
    if (
      !!selectedSecondaryId &&
      (availableSecondaryIds.length === 0 ||
        !availableSecondaryIds.map((id) => id.id).includes(selectedSecondaryId))
    ) {
      onSetSelectedSecondaryId(null);
    }
  }, [JSON.stringify(availableSecondaryIds)]);

  const options = [
    {
      value: "standard",
      label: t("queryEditor.secondaryIdStandard") as string,
    },
    ...availableSecondaryIds.map((id) => ({
      label: id.label,
      value: id.id,
      description: id.description,
    })),
  ];

  if (options.length < 2) {
    return null;
  }

  return (
    <div>
      <Headline active={!!selectedSecondaryId}>
        <SxFaIcon active={!!selectedSecondaryId} left icon="microscope" />
        {t("queryEditor.secondaryId")}
      </Headline>
      <ToggleButton
        input={{
          value: selectedSecondaryId || "standard",
          onChange: onSetSelectedSecondaryId,
        }}
        options={options}
      />
    </div>
  );
};

export default SecondaryIdSelector;
