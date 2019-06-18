import { useEffect } from "react";

function useEscPress(onEscCallback: () => void) {
  function handleKeyDown(e: KeyboardEvent) {
    switch (e.keyCode) {
      case 27: // Esc key
        onEscCallback();
        break;
      default:
        break;
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
