// @flow

import { useEffect } from "react";

function useEscPress(onEscCallback: () => void) {
  function handleKeyDown(e: KeyboardEvent) {
    if (e.key === "Escape" || e.keyCode === 27) {
      onEscCallback();
    }
  }

  useEffect(() => {
    document.addEventListener("keydown", handleKeyDown);

    return () => {
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, []);
}

export default useEscPress;
