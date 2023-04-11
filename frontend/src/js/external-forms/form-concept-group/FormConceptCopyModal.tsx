import styled from "@emotion/styled";
import { useEffect, useState } from "react";
import { useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";

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

const Buttons = styled("div")`
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  margin-top: 20px;
`;

const Options = styled("div")`
  padding: 8px 0 0 28px;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  max-height: 345px;
`;

const SelectAllCheckbox = styled(InputCheckbox)`
  margin: 10px 0 0 8px;
`;

const SxInputCheckbox = styled(InputCheckbox)`
  margin: 5px 0;
`;

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
        formValues[field.name].some((value: FormConceptGroupT) =>
          value.concepts.some(exists),
        );

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
      Object.entries(valuesChecked).map(([key]) => [
        key,
        allConceptsSelected ? false : true,
      ]),
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
    <Modal
      onClose={onClose}
      closeIcon
      headline={t("externalForms.copyModal.headline")}
    >
      <InputSelect
        label={t("externalForms.copyModal.selectLabel")}
        options={conceptListFieldOptions}
        onChange={(val) => {
          if (val) setSelectedOption(val);
        }}
        value={selectedOption}
      />
      <SelectAllCheckbox
        label={t("externalForms.copyModal.selectAll")}
        value={allConceptsSelected}
        onChange={onToggleAllConcepts}
      />
      <Options>
        {Object.keys(valuesChecked).map((idx) =>
          idxHasConcepts(idx) ? (
            <SxInputCheckbox
              key={idx}
              label={getLabelFromIdx(idx)}
              value={valuesChecked[idx]}
              onChange={(checked: boolean) => onToggleConcept(idx, checked)}
            />
          ) : null,
        )}
      </Options>
      <Buttons>
        <TransparentButton onClick={onClose}>
          {t("common.cancel")}
        </TransparentButton>
        <PrimaryButton onClick={onSubmit} disabled={isAcceptDisabled}>
          {t("externalForms.copyModal.accept")}
        </PrimaryButton>
      </Buttons>
    </Modal>
  );
};

export default FormConceptCopyModal;
