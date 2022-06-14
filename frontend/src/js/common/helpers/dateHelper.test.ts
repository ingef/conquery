import { getFirstAndLastDateOfRange, parseStdDate } from "./dateHelper";

describe("dateHelper", () => {
  describe("getFirstAndLastDateOfRange", () => {
    it.each([
      [
        "{2020-09-10/2020-09-10}",
        { first: parseStdDate("2020-09-10"), last: parseStdDate("2020-09-10") },
      ],
      [
        "{2020-09-10/2020-10-10}",
        { first: parseStdDate("2020-09-10"), last: parseStdDate("2020-10-10") },
      ],
      [
        "{2020-09-10/2020-12-12,2020-01-01/2020-10-10}",
        { first: parseStdDate("2020-09-10"), last: parseStdDate("2020-10-10") },
      ],
      [
        "{2020-01-10/2020-12-12,2020-01-01/2020-10-10,2020-05-01/+∞}",
        { first: parseStdDate("2020-01-10"), last: null },
      ],
      [
        "{-∞/2020-01-10,2020-12-12/2021-01-01,2022-10-10/2022-12-01}",
        { first: null, last: parseStdDate("2022-12-01") },
      ],
    ])("parses date string %s correctly", (dateStr, result) => {
      expect(getFirstAndLastDateOfRange(dateStr)).toMatchObject(result);
    });
  });
});
