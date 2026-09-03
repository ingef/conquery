import { faMicroscope } from "@fortawesome/free-solid-svg-icons";
import { memo, useCallback, useEffect, useMemo, useRef } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";
import { tv } from "tailwind-variants";
import type { SecondaryId } from "../api/types";
import type { StateT } from "../app/reducers";
import { exists } from "../common/helpers/exists";
import { nodeIsConceptQueryNode } from "../model/node";
import { Icon } from "../ui-components/Icon";
import InfoTooltip from "../ui-components/InfoTooltip";
import ToggleButtonGroup from "../ui-components/ToggleButtonGroup";

import { setSelectedSecondaryId } from "./actions";
import type { StandardQueryStateT } from "./queryReducer";
import type { SelectedSecondaryIdStateT } from "./selectedSecondaryIdReducer";

const headline = tv({
  base: ["m-0", "text-sm", "uppercase", "transition-[color] duration-100"],
  variants: {
    active: {
      true: "text-primary-500",
      false: "text-gray-500",
    },
  },
});

const headlineIcon = tv({
  base: "transition-[color] duration-100",
  variants: {
    active: {
      true: "text-primary-500",
      false: "text-gray-500",
    },
  },
});

const SecondaryIdSelector = () => {
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

  const onSetSelectedSecondaryId = useCallback(
    (id: string | null) => {
      dispatch(
        setSelectedSecondaryId({ secondaryId: id === "standard" ? null : id }),
      );
    },
    [dispatch],
  );

  // The following is slightly complicated memoization.
  // The reason: `query` is changing frequently, e.g. with every filter change in every table.
  // but most of the changes likely won't affect the availableSecondaryIds.
  // So we only want to trigger rerenders of the selector UI
  // when the `availableSecondaryId` actually change, i.e. when a secondary id
  // is added or removed due to a change in the query,
  // e.g. when certain concepts or queries are added or removed
  const availableSecondaryIds = useMemo(
    () =>
      Array.from(
        new Set(
          query.flatMap((group) =>
            group.elements.flatMap((el) => {
              if (nodeIsConceptQueryNode(el)) {
                return el.tables
                  .filter((table) => !table.exclude)
                  .flatMap((table) => table.supportedSecondaryIds)
                  .filter(exists);
              } else {
                return el.availableSecondaryIds || [];
              }
            }),
          ),
        ),
      )
        .map((id) => loadedSecondaryIds.find((secId) => secId.id === id))
        .filter(exists),
    [query, loadedSecondaryIds],
  );

  const availableSecondaryIdsRef = useRef(availableSecondaryIds);
  availableSecondaryIdsRef.current = availableSecondaryIds;

  const availableSecondaryIdsString = JSON.stringify(availableSecondaryIds);

  // biome-ignore lint/correctness/useExhaustiveDependencies: intentionally keyed on the stringified ids, see above
  useEffect(
    function unselectSecondaryId() {
      const activeSecondaryIdNotFound =
        !!selectedSecondaryId &&
        (availableSecondaryIdsRef.current.length === 0 ||
          !availableSecondaryIdsRef.current
            .map((id) => id.id)
            .includes(selectedSecondaryId));

      if (activeSecondaryIdNotFound) {
        onSetSelectedSecondaryId(null);
      }
    },
    [
      availableSecondaryIdsString,
      onSetSelectedSecondaryId,
      selectedSecondaryId,
    ],
  );

  // biome-ignore lint/correctness/useExhaustiveDependencies: intentionally keyed on the stringified ids, see above
  const options = useMemo(
    () => [
      {
        value: "standard",
        label: t("queryEditor.secondaryIdStandard") as string,
      },
      ...availableSecondaryIdsRef.current.map((id) => ({
        label: id.label,
        value: id.id,
        description: id.description,
      })),
    ],
    // We DO want to recompute this when the availableSecondaryIds change,
    // see explanation above
    [availableSecondaryIdsString, t],
  );

  if (options.length < 2) {
    return null;
  }

  return (
    <SecondaryIdSelectorUI
      options={options}
      value={selectedSecondaryId}
      onChange={onSetSelectedSecondaryId}
    />
  );
};

const SecondaryIdSelectorUI = memo(
  ({
    options,
    value,
    onChange,
  }: {
    options: { label: string; value: string }[];
    value: string | null;
    onChange: (value: string) => void;
  }) => {
    const { t } = useTranslation();

    return (
      <div>
        <h3 className={headline({ active: !!value })}>
          <Icon
            icon={faMicroscope}
            className={[headlineIcon({ active: !!value }), "mr-[10px]"]}
          />
          {t("queryEditor.secondaryId")}
          <InfoTooltip text={t("queryEditor.secondaryIdTooltip")} />
        </h3>
        <ToggleButtonGroup
          value={value || "standard"}
          onChange={onChange}
          options={options}
        />
      </div>
    );
  },
);

export default memo(SecondaryIdSelector);
