// @flow

export const mapOptionToLabelKey = (option: string) => {
  const parts = option.toLowerCase().split("_");

  if (parts[1] === "wise")
    return parts[0].toLowerCase() + "s";

  return parts[0];
}
