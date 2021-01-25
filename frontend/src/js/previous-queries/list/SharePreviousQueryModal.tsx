import React, { useEffect, useState } from "react";
import T from "i18n-react";
import styled from "@emotion/styled";
import { useDispatch, useSelector } from "react-redux";
import { StateT } from "app-types";

import Modal from "../../modal/Modal";
import { patchStoredQuery } from "../../api/api";
import type { DatasetIdT, UserGroupT } from "../../api/types";
import { setMessage } from "../../snack-message/actions";
import TransparentButton from "../../button/TransparentButton";
import PrimaryButton from "../../button/PrimaryButton";
import InputMultiSelect from "../../form-components/InputMultiSelect";
import { usePrevious } from "../../common/helpers/usePrevious";
import { exists } from "../../common/helpers/exists";

import { PreviousQueryT } from "./reducer";
import { loadPreviousQuery, sharePreviousQuerySuccess } from "./actions";

const Buttons = styled("div")`
  text-align: center;
`;

const Btn = styled(TransparentButton)`
  margin: 0 10px;
`;

const PrimaryBtn = styled(PrimaryButton)`
  margin: 0 10px;
`;

const SxInputMultiSelect = styled(InputMultiSelect)`
  display: block;
  margin-bottom: 20px;
`;

const QueryName = styled("p")`
  margin: -15px 0 20px;
`;

interface SelectValueT {
  label: string;
  value: string;
}

interface PropsT {
  previousQueryId: string;
  onClose: () => void;
  onShareSuccess: () => void;
}

const getUserGroupsValue = (
  userGroups: UserGroupT[],
  previousQuery?: PreviousQueryT
) => {
  return previousQuery && previousQuery.groups
    ? userGroups
        .filter((group) => previousQuery.groups?.includes(group.id))
        .map((group) => ({
          label: group.label,
          value: group.id,
        }))
    : [];
};

const SharePreviousQueryModal = ({
  previousQueryId,
  onClose,
  onShareSuccess,
}: PropsT) => {
  const datasetId = useSelector<StateT, DatasetIdT | null>(
    (state) => state.datasets.selectedDatasetId
  );
  const userGroups = useSelector<StateT, UserGroupT[]>((state) =>
    state.user.me ? state.user.me.groups : []
  );
  const previousQuery = useSelector<StateT, PreviousQueryT | undefined>(
    (state) =>
      state.previousQueries.queries.find(
        (query) => query.id === previousQueryId
      )
  );
  const initialUserGroupsValue = getUserGroupsValue(userGroups, previousQuery);

  const [userGroupsValue, setUserGroupsValue] = useState<SelectValueT[]>(
    initialUserGroupsValue
  );

  const previousPreviousQueryId = usePrevious(previousQueryId);

  const dispatch = useDispatch();

  useEffect(() => {
    if (
      exists(datasetId) &&
      !exists(previousPreviousQueryId) &&
      exists(previousQueryId)
    ) {
      dispatch(loadPreviousQuery(datasetId, previousQueryId));
    }
  }, [datasetId, previousPreviousQueryId, previousQueryId, dispatch]);

  useEffect(() => {
    setUserGroupsValue(getUserGroupsValue(userGroups, previousQuery));
  }, [userGroups, previousQuery]);

  const onSetUserGroupsValue = (value: SelectValueT[] | null) => {
    setUserGroupsValue(value ? value : []);
  };

  if (!previousQuery) {
    return null;
  }

  const userGroupOptions = userGroups.map((group) => ({
    label: group.label,
    value: group.id,
  }));

  async function onShareClicked() {
    if (!datasetId) return;

    const userGroupsToShare = userGroupsValue.map((group) => group.value);

    try {
      await patchStoredQuery(datasetId, previousQueryId, {
        groups: userGroupsToShare,
      });

      dispatch(sharePreviousQuerySuccess(previousQueryId, userGroupsToShare));

      onShareSuccess();
    } catch (e) {
      dispatch(setMessage("previousQuery.shareError"));
    }
  }

  return (
    <Modal
      onClose={onClose}
      headline={T.translate("sharePreviousQueryModal.headline")}
    >
      <QueryName>{previousQuery.label}</QueryName>
      <SxInputMultiSelect
        input={{ value: userGroupsValue, onChange: onSetUserGroupsValue }}
        label={T.translate("sharePreviousQueryModal.groupsLabel")}
        options={userGroupOptions}
      />
      <Buttons>
        <Btn onClick={onClose}>{T.translate("common.cancel")}</Btn>
        <PrimaryBtn onClick={onShareClicked}>
          {previousQuery.shared && userGroupsValue.length === 0
            ? T.translate("sharePreviousQueryModal.unshare")
            : T.translate("common.share")}
        </PrimaryBtn>
      </Buttons>
    </Modal>
  );
};

export default SharePreviousQueryModal;
