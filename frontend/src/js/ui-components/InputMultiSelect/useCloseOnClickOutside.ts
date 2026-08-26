import { useCallback, useRef } from "react";

import { useClickOutside } from "../../common/helpers/useClickOutside";

export const useCloseOnClickOutside = <T extends HTMLElement = HTMLElement>({
  isOpen,
  toggleMenu,
}: {
  isOpen?: boolean;
  toggleMenu: () => void;
}) => {
  const clickOutsideRef = useRef<T | null>(null);

  useClickOutside(
    clickOutsideRef,
    useCallback(() => {
      if (isOpen) {
        toggleMenu();
      }
    }, [isOpen, toggleMenu]),
  );

  return clickOutsideRef;
};
