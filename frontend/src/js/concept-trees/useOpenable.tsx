import { useState } from "react";

interface PropsT {
  openInitially?: boolean;
}

const useOpenable = ({ openInitially }: PropsT) => {
  const [open, setOpen] = useState<boolean>(openInitially || false);

  function onToggleOpen() {
    setOpen(!open);
  }

  return {
    open,
    onToggleOpen,
  };
};

export default useOpenable;
