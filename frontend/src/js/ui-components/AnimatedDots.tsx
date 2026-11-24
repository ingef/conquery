import { tv } from "tailwind-variants";

import { C2 } from "./Typography";

const dot = tv({
  base: [
    "animate-blink",
    "nth-of-type-2:[animation-delay:250ms]",
    "nth-of-type-3:[animation-delay:500ms]",
  ],
});

export default function AnimatedDots() {
  return (
    <C2 as="span" strong>
      <span className={dot()}>.</span>
      <span className={dot()}>.</span>
      <span className={dot()}>.</span>
    </C2>
  );
}
