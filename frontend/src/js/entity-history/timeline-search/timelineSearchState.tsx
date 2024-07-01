import { ReactNode, createContext, useContext, useState } from "react";

const Context = createContext<{
  searchVisible: boolean;
  setSearchVisible: (searchVisible: boolean) => void;
  searchTerm?: string;
  setSearchTerm: (searchTerm: string) => void;
}>({
  searchVisible: false,
  setSearchVisible: () => {},
  searchTerm: undefined,
  setSearchTerm: () => {},
});

export const TimelineSearchProvider = ({
  children,
}: {
  children: ReactNode;
}) => {
  const [searchVisible, setSearchVisible] = useState<boolean>(false);
  const [searchTerm, setSearchTerm] = useState<string>();

  return (
    <Context.Provider
      value={{
        searchVisible,
        setSearchVisible,
        searchTerm,
        setSearchTerm,
      }}
    >
      {children}
    </Context.Provider>
  );
};

export const useTimelineSearch = () => {
  return useContext(Context);
};
