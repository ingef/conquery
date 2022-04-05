import { useState } from "react";

import {
  addConcept,
  addValue,
  copyConcept,
  FormConceptGroupT,
} from "./formConceptGroupState";

export const useCopyModal = ({
  value,
  onChange,
  newValue,
}: {
  value: FormConceptGroupT[];
  onChange: (value: FormConceptGroupT[]) => void;
  newValue: FormConceptGroupT;
}) => {
  const [isOpen, setIsOpen] = useState(false);

  const onAccept = (valuesToCopy: FormConceptGroupT[]) => {
    // Deeply copy all values + concepts
    const nextValue = valuesToCopy.reduce((currentValue, value) => {
      const newVal = addValue(currentValue, newValue);

      return value.concepts.reduce(
        (curVal, concept) =>
          addConcept(curVal, curVal.length - 1, copyConcept(concept)),
        newVal,
      );
    }, value);

    return onChange(nextValue);
  };

  return {
    isOpen,
    onAccept,
    setIsOpen,
  };
};
