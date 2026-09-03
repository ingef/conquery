import { faFile, faPaste } from "@fortawesome/free-solid-svg-icons";
import {
  type ChangeEvent,
  type MouseEvent,
  useEffect,
  useRef,
  useState,
} from "react";
import { NativeTypes } from "react-dnd-html5-backend";
import { createPortal } from "react-dom";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";

import IconButton from "../button/IconButton";
import PrimaryButton from "../button/PrimaryButton";
import { getUniqueFileRows } from "../common/helpers/fileHelper";
import Modal from "../modal/Modal";

import DropzoneWithFileInput, {
  type DragItemFile,
} from "./DropzoneWithFileInput";

const content = tv({
  base: ["flex flex-col", "gap-5"],
});

const row = tv({
  base: ["flex items-center justify-end", "gap-[10px]"],
});

const textarea = tv({
  base: ["font-mono", "w-full"],
});

const subtitle = tv({
  base: ["m-0", "max-w-[600px]"],
});

const acceptedDropTypes = [NativeTypes.FILE];

const useCanReadClipboard = () => {
  const [canReadClipboard, setCanReadClipboard] = useState(false);
  useEffect(() => {
    const checkPersmission = async () => {
      const { state } = await navigator.permissions.query({
        // @ts-ignore https://github.com/microsoft/TypeScript/issues/33923
        name: "clipboard-read",
      });

      if (state === "granted" || state === "prompt") {
        setCanReadClipboard(true);
      }
    };
    checkPersmission();
  }, []);

  return canReadClipboard;
};

export const ImportModal = ({
  placeholder,
  description,
  onClose,
  onSubmit,
}: {
  description?: string;
  placeholder?: string;
  onClose: () => void;
  onSubmit: (lines: string[], filename?: string) => void;
}) => {
  const { t } = useTranslation();
  const [textInput, setTextInput] = useState("");
  const [droppedFilename, setDroppedFilename] = useState<string>();
  const canReadClipboard = useCanReadClipboard();

  const fileInputRef = useRef<HTMLInputElement>(null);

  const onSubmitClick = (
    e: MouseEvent<HTMLButtonElement, globalThis.MouseEvent>,
  ) => {
    e.stopPropagation();

    const lines = textInput
      .split("\n")
      .map((line) => line.trim())
      .filter((line) => line.length > 0);

    onSubmit(lines, droppedFilename);
    onClose();
  };

  const onOpenFileDialog = () => {
    if (fileInputRef.current) {
      fileInputRef.current.click();
    }
  };

  const autoFormatAndSet = (text: string) => {
    let delimiter = "\n";

    if (text.includes(";")) {
      delimiter = ";";
    } else if (text.includes(",")) {
      delimiter = ",";
    }

    const formatted = text
      .split(delimiter)
      .map((part) => part.trim())
      .join("\n");

    setTextInput(formatted);
  };

  const onSelectFile = async (file: File) => {
    const rows = await getUniqueFileRows(file);

    setDroppedFilename(file.name);
    autoFormatAndSet(rows.join("\n"));
  };

  const onPasteClick = async () => {
    if (navigator.clipboard) {
      const text = await navigator.clipboard.readText();

      autoFormatAndSet(text);
    }
  };

  const onDrop = async ({ files }: DragItemFile) => {
    const file = files[0];
    onSelectFile(file);
  };

  const onChange = (e: ChangeEvent<HTMLTextAreaElement>) => {
    const val = e.target.value;

    // If the user is regularly typing or deleting, don't auto format
    // but if the user is pasting, auto format
    if (val.length - textInput.length < 3) {
      setTextInput(val);
    } else {
      autoFormatAndSet(val);
    }
  };

  return createPortal(
    <Modal
      headline={t("importModal.headline")}
      subtitle={t("importModal.subtitle")}
      onClose={onClose}
    >
      <div className={content()}>
        {description && (
          <p
            className={subtitle()}
            // biome-ignore lint/security/noDangerouslySetInnerHtml: description is our own i18n text
            dangerouslySetInnerHTML={{ __html: description }}
          />
        )}
        <DropzoneWithFileInput
          onDrop={onDrop}
          acceptedDropTypes={acceptedDropTypes}
          disableClick
          accept="text/plain,text/csv"
        >
          {() => (
            <textarea
              className={textarea()}
              rows={15}
              value={textInput}
              onChange={onChange}
              placeholder={placeholder}
            />
          )}
        </DropzoneWithFileInput>
        <div className={row()}>
          <IconButton icon={faFile} onClick={onOpenFileDialog}>
            {t("common.openFileDialog")}
          </IconButton>
          {canReadClipboard && (
            <IconButton icon={faPaste} onClick={onPasteClick}>
              {t("importModal.paste")}
            </IconButton>
          )}
          <PrimaryButton
            disabled={textInput.length === 0}
            onClick={onSubmitClick}
          >
            {t("importModal.submit")}
          </PrimaryButton>
        </div>
        <input
          className="hidden"
          type="file"
          ref={fileInputRef}
          accept="text/plain,text/csv"
          onChange={(e) => {
            if (e.target.files) {
              onSelectFile(e.target.files[0]);
            }

            if (fileInputRef.current) {
              fileInputRef.current.value = "";
            }
          }}
        />
      </div>
    </Modal>,
    document.getElementById("root")!,
  );
};
