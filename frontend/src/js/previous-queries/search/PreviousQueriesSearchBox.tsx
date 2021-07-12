import styled from "@emotion/styled";
import { StateT } from "app-types";
import React, { FC, useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { useSelector, useDispatch } from "react-redux";

import IconButton from "../../button/IconButton";
import { exists } from "../../common/helpers/exists";
import BaseInput from "../../form-components/BaseInput";

import { clearQueriesSearch, useSearchQueries } from "./actions";
import { QueriesSearchStateT } from "./reducer";

const Root = styled("div")`
  position: relative;
`;

const InputContainer = styled("div")`
  flex-grow: 1;
  position: relative;
`;

const SxBaseInput = styled(BaseInput)`
  width: 100%;
  input {
    padding-right: 60px;
    width: 100%;
    &::placeholder {
      color: ${({ theme }) => theme.col.grayMediumLight};
      opacity: 1;
    }
  }
`;

const Right = styled("div")`
  position: absolute;
  top: 0px;
  right: 30px;
  display: flex;
  flex-direction: row;
  align-items: center;
  height: 34px;
`;

const StyledIconButton = styled(IconButton)`
  padding: 8px 10px;
  color: ${({ theme }) => theme.col.gray};
`;

interface Props {
  className?: string;
}

const PreviousQueriesSearchBox: FC<Props> = ({ className }) => {
  const { t } = useTranslation();
  const search = useSelector<StateT, QueriesSearchStateT>(
    (state) => state.previousQueriesSearch,
  );

  useEffect(() => {
    setLocalSearchTerm(search.searchTerm);
  }, [search.searchTerm]);

  const dispatch = useDispatch();
  const searchQueries = useSearchQueries();

  const onClearQuery = () => dispatch(clearQueriesSearch());

  const [localSearchTerm, setLocalSearchTerm] = useState<string | null>(null);

  return (
    <Root className={className}>
      <InputContainer>
        <SxBaseInput
          inputType="text"
          placeholder={t("previousQueries.searchPlaceholder")}
          value={localSearchTerm || ""}
          onChange={(value) => {
            if (!exists(value)) onClearQuery();

            setLocalSearchTerm(value as string | null);
          }}
          inputProps={{
            onKeyPress: (e) => {
              return e.key === "Enter" && exists(localSearchTerm)
                ? searchQueries(localSearchTerm)
                : null;
            },
          }}
        />
        {exists(localSearchTerm) && (
          <Right>
            <StyledIconButton
              icon="search"
              aria-hidden="true"
              onClick={() => searchQueries(localSearchTerm)}
            />
          </Right>
        )}
      </InputContainer>
    </Root>
  );
};

export default PreviousQueriesSearchBox;
