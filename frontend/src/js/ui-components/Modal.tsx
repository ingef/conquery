import type { ReactNode } from "react";
import {
  Dialog,
  type DialogRenderProps,
  Heading,
  ModalOverlay,
  type ModalOverlayProps,
  Modal as RacModal,
} from "react-aria-components";
import { tv } from "tailwind-variants";

const overlay = tv({
  base: [
    "fixed inset-0 z-10",
    "flex items-center justify-center",
    "bg-white/50",
    "data-entering:animate-fade-in",
    "data-exiting:animate-fade-out",
  ],
});

const modal = tv({
  base: [
    "w-full mx-5",
    "max-h-[95%]",
    "rounded",
    "bg-white",
    "shadow-[0_0_15px_0_rgba(0,0,0,0.2)]",
    "outline-none",
  ],
  variants: {
    // the card's width, 30 px padding included; the content fills it
    size: {
      sm: "max-w-[440px]",
      md: "max-w-[560px]",
      lg: "max-w-[760px]",
      xl: "max-w-[1000px]",
      full: "max-w-none",
    },
    // visible overflow lets the menus of the select boxes reach past the card
    scrollable: {
      true: "overflow-y-auto",
      false: "overflow-y-visible",
    },
  },
  defaultVariants: { size: "md", scrollable: false },
});

const dialog = tv({
  base: ["flex flex-col", "gap-5", "p-[30px]", "text-left", "outline-none"],
});

const header = tv({ base: ["flex flex-col", "gap-[15px]"] });

const heading = tv({
  base: ["flex items-center", "text-lg font-normal text-gray-800"],
});

const body = tv({ base: "min-w-0" });

const footer = tv({ base: ["flex items-center justify-end", "gap-[10px]"] });

export interface ModalProps
  extends Omit<
    ModalOverlayProps,
    "className" | "style" | "children" | "isDismissable"
  > {
  children: ReactNode | ((renderProps: DialogRenderProps) => ReactNode);
  /** names the dialog when it has no ModalHeader */
  "aria-label"?: string;
  /**
   * the card's width: `sm` 440 px for a confirmation, `md` 560 px for a form
   * (the default), `lg` 760 px for a text area or a drop area, `xl` 1000 px
   * for wide tables, `full` the viewport minus its margins
   */
  size?: "sm" | "md" | "lg" | "xl" | "full";
  /** the card scrolls instead of growing past the viewport */
  scrollable?: boolean;
  "data-test-id"?: string;
}

/**
 * A modal dialog on react-aria-components: it traps focus, closes on Escape
 * and on a click outside, and hands focus back afterwards. Inside a
 * DialogTrigger it opens from the trigger; on its own it is controlled via
 * `isOpen` / `onOpenChange`. A Button with `slot="close"` closes it, so do
 * the render props: `{({ close }) => …}`.
 *
 *   <DialogTrigger>
 *     <Button>Settings</Button>
 *     <Modal>
 *       <ModalHeader subtitle={item.label}>Edit folders</ModalHeader>
 *       <ModalBody>…</ModalBody>
 *       <ModalFooter>
 *         <Button slot="close">Cancel</Button>
 *         <Button intent="primary" onPress={save}>Save</Button>
 *       </ModalFooter>
 *     </Modal>
 *   </DialogTrigger>
 *
 * Header and footer are optional. `size` sets the card's width, the content
 * fills it; layout inside the body is the caller's.
 */
export const Modal = ({
  children,
  "aria-label": ariaLabel,
  size,
  scrollable,
  "data-test-id": dataTestId,
  ...props
}: ModalProps) => (
  <ModalOverlay className={overlay()} isDismissable {...props}>
    <RacModal className={modal({ size, scrollable })} data-test-id={dataTestId}>
      <Dialog className={dialog()} aria-label={ariaLabel}>
        {children}
      </Dialog>
    </RacModal>
  </ModalOverlay>
);

/** the dialog's title, which also names it for assistive technology */
export const ModalHeader = ({
  children,
  subtitle,
}: {
  children: ReactNode;
  subtitle?: ReactNode;
}) => (
  <header className={header()}>
    <Heading slot="title" className={heading()}>
      {children}
    </Heading>
    {subtitle && <p>{subtitle}</p>}
  </header>
);

export const ModalBody = ({ children }: { children: ReactNode }) => (
  <div className={body()}>{children}</div>
);

/** the dialog's actions, right-aligned: a closing button first, the primary one last */
export const ModalFooter = ({ children }: { children: ReactNode }) => (
  <footer className={footer()}>{children}</footer>
);
