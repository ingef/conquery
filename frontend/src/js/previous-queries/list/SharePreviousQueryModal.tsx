import React, { useState } from "react";
import T from "i18n-react";
import styled from "@emotion/styled";

import Modal from "../../modal/Modal";
import { patchStoredQuery } from "../../api/api";
import { useDispatch, useSelector } from "react-redux";
import { StateT } from "app-types";
import type { DatasetIdT, UserGroupT } from "js/api/types";
import { setMessage } from "../../snack-message/actions";
import TransparentButton from "../../button/TransparentButton";
import PrimaryButton from "../../button/PrimaryButton";
import { PreviousQueryT } from "./reducer";
import InputMultiSelect from "../../form-components/InputMultiSelect";
import { sharePreviousQuerySuccess } from "./actions";

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

interface PropsT {
  previousQueryId: string;
  onClose: () => void;
  onShareSuccess: () => void;
}

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
  const initialUserGroupsValue =
    previousQuery && previousQuery.groups
      ? userGroups.filter((group) =>
          previousQuery.groups?.includes(group.groupId)
        )
      : [];

  const [userGroupsValue, setUserGroupsValue] = useState<UserGroupT[]>(
    initialUserGroupsValue
  );

  const onSetUserGroupsValue = (value: UserGroupT[] | null) => {
    setUserGroupsValue(value ? value : []);
  };

  const dispatch = useDispatch();

  if (!previousQuery) {
    return null;
  }

  const userGroupOptions = userGroups.map((group) => ({
    label: group.label,
    value: group.groupId,
  }));

  async function onShareClicked() {
    if (!datasetId) return;

    const shared = userGroupsValue.length > 0;
    const userGroupsToShare = userGroupsValue.map((group) => group.value);

    try {
      await patchStoredQuery(datasetId, previousQueryId, {
        shared,
        groups: userGroupsToShare,
      });

      dispatch(
        sharePreviousQuerySuccess(previousQueryId, shared, userGroupsToShare)
      );

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
