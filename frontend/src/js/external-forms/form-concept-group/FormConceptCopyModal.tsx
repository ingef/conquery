import { useEffect, useState } from "react";
import { useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";
import type { SelectOptionT } from "../../api/types";
import { exists } from "../../common/helpers/exists";
import { useActiveLang } from "../../localization/useActiveLang";
import { Button } from "../../ui-components/Button";
import { Checkbox } from "../../ui-components/Checkbox";
import InputSelect from "../../ui-components/InputSelect/InputSelect";
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalHeader,
} from "../../ui-components/Modal";
import { useVisibleConceptListFields } from "../stateSelectors";

import type { FormConceptGroupT } from "./formConceptGroupState";

const selectAll = tv({ base: "mt-[10px] ml-2" });

const options = tv({
  base: [
    "flex flex-col gap-1",
    "pt-2 pl-[28px]",
    "max-h-[345px]",
    "overflow-y-auto",
    "[-webkit-overflow-scrolling:touch]",
  ],
});

const FormConceptCopyModal = ({
  targetFieldname,
  onAccept,
}: {
  targetFieldname: string;
  onAccept: (selectedNodes: FormConceptGroupT[]) => void;
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

  function onSubmit(close: () => void) {
    const selectedNodes = Object.keys(valuesChecked)
      .filter((index) => valuesChecked[index])
      .map(
        (index) => formValues[selectedOption.value][index] as FormConceptGroupT,
      );

    onAccept(selectedNodes);
    close();
  }

  return (
    <Modal>
      {({ close }) => (
        <>
          <ModalHeader>{t("externalForms.copyModal.headline")}</ModalHeader>
          <ModalBody>
            <InputSelect
              label={t("externalForms.copyModal.selectLabel")}
              options={conceptListFieldOptions}
              onChange={(val) => {
                if (val) setSelectedOption(val);
              }}
              value={selectedOption}
            />
            <div className={selectAll()}>
              <Checkbox
                isSelected={allConceptsSelected}
                onChange={onToggleAllConcepts}
              >
                {t("externalForms.copyModal.selectAll")}
              </Checkbox>
            </div>
            <div className={options()}>
              {Object.keys(valuesChecked).map((idx) =>
                idxHasConcepts(idx) ? (
                  <Checkbox
                    key={idx}
                    isSelected={valuesChecked[idx]}
                    onChange={(checked: boolean) =>
                      onToggleConcept(idx, checked)
                    }
                  >
                    {getLabelFromIdx(idx)}
                  </Checkbox>
                ) : null,
              )}
            </div>
          </ModalBody>
          <ModalFooter>
            <Button slot="close">{t("common.cancel")}</Button>
            <Button
              intent="primary"
              onPress={() => onSubmit(close)}
              isDisabled={isAcceptDisabled}
            >
              {t("externalForms.copyModal.accept")}
            </Button>
          </ModalFooter>
        </>
      )}
    </Modal>
  );
};

export default FormConceptCopyModal;
