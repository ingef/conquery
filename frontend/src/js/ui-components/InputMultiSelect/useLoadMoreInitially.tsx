import { useEffect } from "react";

import { usePrevious } from "../../common/helpers/usePrevious";

export const useLoadMoreInitially = ({
  onLoadMore,
  isOpen,
  optionsLength,
}: {
  onLoadMore?: (inputValue: string) => void;
  isOpen?: boolean;
  optionsLength?: number;
}) => {
  const wasOpen = usePrevious(isOpen);

  useEffect(
    function loadInitialOptionsWithEmptySearch() {
      if (!!onLoadMore && optionsLength === 0 && !wasOpen && isOpen) {
        onLoadMore("");
      }
    },
    [wasOpen, isOpen, optionsLength, onLoadMore],
  );
};
