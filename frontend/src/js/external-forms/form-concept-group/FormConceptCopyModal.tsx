import styled from "@emotion/styled";
import { StateT } from "app-types";
import React, { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";

import PrimaryButton from "../../button/PrimaryButton";
import { TransparentButton } from "../../button/TransparentButton";
import { useActiveLang } from "../../localization/useActiveLang";
import Modal from "../../modal/Modal";
import InputCheckbox from "../../ui-components/InputCheckbox";
import InputSelect from "../../ui-components/InputSelectOld";
import {
  selectActiveFormValues,
  useVisibleConceptListFields,
} from "../stateSelectors";

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

type PropsT = {
  targetFieldname: string;
  onAccept: (selectedNodes: Object[]) => void;
  onClose: () => void;
};

const FormConceptCopyModal = ({
  targetFieldname,
  onAccept,
  onClose,
}: PropsT) => {
  const { t } = useTranslation();
  const activeLang = useActiveLang();
  const formValues = useSelector<StateT, Record<string, any>>((state) =>
    selectActiveFormValues(state),
  );
  const visibleConceptListFields = useVisibleConceptListFields();

  const conceptListFieldOptions = visibleConceptListFields
    .filter((field) => field.name !== targetFieldname)
    .map((field) => ({
      label: field.label[activeLang],
      value: field.name,
    }));

  // Since the modal is only rendered when there exists more than one concept list field
  // we can assume that `conceptListFieldOptions` still has length >= 1
  const [selectedOption, setSelectedOption] = useState<string>(
    conceptListFieldOptions[0].value,
  );

  const [valuesChecked, setValuesChecked] = useState<{
    [key: number]: boolean;
  }>({});

  useEffect(() => {
    const values = formValues[selectedOption];
    const initiallyChecked = values.reduce((checkedValues, value, i) => {
      checkedValues[i] = false;
      return checkedValues;
    }, {});

    setValuesChecked(initiallyChecked);
  }, [selectedOption]);

  const allConceptsSelected = Object.keys(valuesChecked).every(
    (key) => valuesChecked[key],
  );

  const isAcceptDisabled = Object.keys(valuesChecked).every(
    (key) => !valuesChecked[key],
  );

  function idxHasConcepts(idx: number) {
    const values = formValues[selectedOption];
    const concepts = values[idx].concepts.filter((cpt) => !!cpt);

    return concepts.length > 0;
  }

  function getLabelFromIdx(idx: number) {
    const values = formValues[selectedOption];
    const concepts = values[idx].concepts.filter((cpt) => !!cpt);

    if (concepts.length === 0) return "-";

    return (
      concepts[0].label +
      (concepts.length > 1 ? ` + ${concepts.length - 1}` : "")
    );
  }

  function onToggleAllConcepts(checked: boolean) {
    const allChecked = Object.keys(valuesChecked).reduce((all, key) => {
      all[key] = allConceptsSelected ? false : true;

      return all;
    }, {});

    setValuesChecked(allChecked);
  }

  function onToggleConcept(idx: number, checked: boolean) {
    const nextValues = {
      ...valuesChecked,
      [idx]: checked,
    };

    setValuesChecked(nextValues);
  }

  function onSubmit() {
    const selectedValues = Object.keys(valuesChecked)
      .filter((key) => valuesChecked[key])
      .map((key) => formValues[selectedOption][key]);

    onAccept(selectedValues);
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
        input={{ onChange: setSelectedOption, value: selectedOption }}
      />
      <SelectAllCheckbox
        label={t("externalForms.copyModal.selectAll")}
        input={{ value: allConceptsSelected, onChange: onToggleAllConcepts }}
      />
      <Options>
        {Object.keys(valuesChecked).map((idx, i) =>
          idxHasConcepts ? (
            <SxInputCheckbox
              key={idx}
              label={getLabelFromIdx(idx)}
              input={{
                value: valuesChecked[idx],
                onChange: (checked: boolean) => onToggleConcept(idx, checked),
              }}
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
