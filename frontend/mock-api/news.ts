const daysAgo = (days: number) => {
  const date = new Date();
  date.setDate(date.getDate() - days);

  return date.toISOString().slice(0, 10);
};

const NEWS = [
  {
    id: "news-8",
    title: "One menu for history, help and logout",
    description:
      "The top bar got tidier: the history, the manual, the contact address and the logout now sit behind the menu button on the right.",
    link: null,
    daysAgo: 1,
  },
  {
    id: "news-7",
    title: "New icons",
    description:
      "All icons come from one family now, with the same line width everywhere.\nFolders that are in use show a light tint.",
    link: "https://example.org/news/icons",
    daysAgo: 6,
  },
  {
    id: "news-6",
    title: "Select several values faster",
    description:
      "The multi select keeps its list open while you pick, and Backspace removes the last value.",
    link: "https://example.org/news/multi-select",
    daysAgo: 20,
  },
  {
    id: "news-5",
    title: "Range filters on one row",
    description:
      "Number and date ranges show their two bounds side by side. For an exact value, enter it on both sides.",
    link: null,
    daysAgo: 45,
  },
  {
    id: "news-4",
    title: "Maintenance window",
    description:
      "The application was unavailable for two hours during a scheduled update.",
    link: null,
    daysAgo: 75,
  },
  {
    id: "news-3",
    title: "Results as a spreadsheet",
    description:
      "Results can be downloaded as a spreadsheet file, the format you picked last is remembered.",
    link: "https://example.org/news/downloads",
    daysAgo: 120,
  },
  {
    id: "news-2",
    title: "Folders for your queries",
    description: "Sort saved queries into folders and filter the list by them.",
    link: null,
    daysAgo: 200,
  },
  {
    id: "news-1",
    title: "Welcome",
    description: "This is where we tell you about changes to the application.",
    link: "https://example.org/news/welcome",
    daysAgo: 365,
  },
];

export const getNews = (datasetId: string) =>
  datasetId === "imdb"
    ? NEWS.map(({ daysAgo: days, ...item }) => ({
        ...item,
        date: daysAgo(days),
      }))
    : [];
