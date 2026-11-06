import { Fragment, useMemo } from "react";

const escapeRegExp = (s: string) => s.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");

// Highlights every case-insensitive occurrence of the search words in the text,
// like react-highlight-words did: matches are wrapped in <mark>.
export const Highlighter = ({
  searchWords,
  textToHighlight,
  className,
}: {
  searchWords: string[];
  textToHighlight: string;
  className?: string;
}) => {
  const regex = useMemo(() => {
    const words = searchWords.filter((w) => w.length > 0).map(escapeRegExp);
    return words.length > 0 ? new RegExp(`(${words.join("|")})`, "gi") : null;
  }, [searchWords]);

  if (!regex) return <span className={className}>{textToHighlight}</span>;

  // A capturing group makes split() keep the matches at the odd indices
  const chunks = textToHighlight.split(regex);

  return (
    <span className={className}>
      {chunks.map((chunk, i) =>
        i % 2 === 1 ? (
          <mark key={i}>{chunk}</mark>
        ) : (
          <Fragment key={i}>{chunk}</Fragment>
        ),
      )}
    </span>
  );
};
