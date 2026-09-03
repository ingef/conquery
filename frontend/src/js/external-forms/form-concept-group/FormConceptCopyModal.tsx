import { useEffect, useState } from "react";
import { useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";

import type { SelectOptionT } from "../../api/types";
import PrimaryButton from "../../button/PrimaryButton";
import { TransparentButton } from "../../button/TransparentButton";
import { exists } from "../../common/helpers/exists";
import { useActiveLang } from "../../localization/useActiveLang";
import Modal from "../../modal/Modal";
import InputCheckbox from "../../ui-components/InputCheckbox";
import InputSelect from "../../ui-components/InputSelect/InputSelect";
import { useVisibleConceptListFields } from "../stateSelectors";

import type { FormConceptGroupT } from "./formConceptGroupState";

const buttons = tv({
  base: ["flex items-center justify-between", "w-full", "mt-5"],
});

const options = tv({
  base: [
    "pt-2 pl-[28px]",
    "max-h-[345px]",
    "overflow-y-auto",
    "[-webkit-overflow-scrolling:touch]",
  ],
});

const FormConceptCopyModal = ({
  targetFieldname,
  onAccept,
  onClose,
}: {
  targetFieldname: string;
  onAccept: (selectedNodes: FormConceptGroupT[]) => void;
  onClose: () => void;
}) => {
  const { t } = useTranslation();
  const activeLang = useActiveLang();
  const { getValues } = useFormContext();
  const formValues = getValues(); // Isn't watching for changes
  const visibleConceptListFields = useVisibleConceptListFields();

  const conceptListFieldOptions = visibleConceptListFields
    .filter((field) => {
      const isAnotherField = field.name !== targetFieldname;
      const hasValues =
        formValues[field.name] &&
        formValues[field.name]
          .flatMap((v: FormConceptGroupT) => v.concepts)
          .some(exists);

      return isAnotherField && hasValues;
    })
    .map((field) => ({
      label: field.label[activeLang] || "-",
      value: field.name,
    }));

  // Since the modal is only rendered when there exists more than one concept list field
  // we can assume that `conceptListFieldOptions` still has length >= 1
  const [selectedOption, setSelectedOption] = useState<SelectOptionT>(
    conceptListFieldOptions[0],
  );

  const [valuesChecked, setValuesChecked] = useState<{
    [key: string]: boolean;
  }>({});

  useEffect(() => {
    const values = getValues()[selectedOption.value] as unknown[];
    const initiallyChecked = Object.fromEntries(
      values.map((_, i) => [String(i), false]),
    );

    setValuesChecked(initiallyChecked);
  }, [selectedOption, getValues]);

  const allConceptsSelected = Object.keys(valuesChecked).every(
    (key) => valuesChecked[key],
  );

  const isAcceptDisabled = Object.keys(valuesChecked).every(
    (key) => !valuesChecked[key],
  );

  function idxHasConcepts(idx: string) {
    const values = formValues[selectedOption.value];
    const concepts = values[idx].concepts.filter(exists);

    return concepts.length > 0;
  }

  function getLabelFromIdx(idx: string) {
    const values = formValues[selectedOption.value];
    const concepts = values[idx].concepts.filter(exists);

    if (concepts.length === 0) return "-";

    return (
      concepts[0].label +
      (concepts.length > 1 ? ` + ${concepts.length - 1}` : "")
    );
  }

  function onToggleAllConcepts() {
    const allChecked = Object.fromEntries(
      Object.entries(valuesChecked).map(([key]) => [key, !allConceptsSelected]),
    );

    setValuesChecked(allChecked);
  }

  function onToggleConcept(idx: string, checked: boolean) {
    const nextValues = {
      ...valuesChecked,
      [idx]: checked,
    };

    setValuesChecked(nextValues);
  }

  function onSubmit() {
    const selectedNodes = Object.keys(valuesChecked)
      .filter((index) => valuesChecked[index])
      .map(
        (index) => formValues[selectedOption.value][index] as FormConceptGroupT,
      );

    onAccept(selectedNodes);
    onClose();
  }

  return (
    <Modal onClose={onClose} headline={t("externalForms.copyModal.headline")}>
      <InputSelect
        label={t("externalForms.copyModal.selectLabel")}
        options={conceptListFieldOptions}
        onChange={(val) => {
          if (val) setSelectedOption(val);
        }}
        value={selectedOption}
      />
      <InputCheckbox
        className="mt-[10px] ml-2"
        label={t("externalForms.copyModal.selectAll")}
        value={allConceptsSelected}
        onChange={onToggleAllConcepts}
      />
      <div className={options()}>
        {Object.keys(valuesChecked).map((idx) =>
          idxHasConcepts(idx) ? (
            <InputCheckbox
              className="my-[5px]"
              key={idx}
              label={getLabelFromIdx(idx)}
              value={valuesChecked[idx]}
              onChange={(checked: boolean) => onToggleConcept(idx, checked)}
            />
          ) : null,
        )}
      </div>
      <div className={buttons()}>
        <TransparentButton onClick={onClose}>
          {t("common.cancel")}
        </TransparentButton>
        <PrimaryButton onClick={onSubmit} disabled={isAcceptDisabled}>
          {t("externalForms.copyModal.accept")}
        </PrimaryButton>
      </div>
    </Modal>
  );
};

export default FormConceptCopyModal;
