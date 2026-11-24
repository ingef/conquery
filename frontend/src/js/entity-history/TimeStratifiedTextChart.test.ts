import { describe, expect, it } from "vitest";

import { createTimeStratifiedTextMatrix } from "./TimeStratifiedTextChart";

describe("createTimeStratifiedTextMatrix", () => {
  it("sorts scalar string values and years", () => {
    expect(
      createTimeStratifiedTextMatrix(
        [
          { year: 2020, values: { Status: "alpha" }, quarters: [] },
          { year: 2022, values: { Status: "10" }, quarters: [] },
          { year: 2019, values: { Status: "2" }, quarters: [] },
          { year: 2021, values: {}, quarters: [] },
        ],
        "Status",
      ),
    ).toEqual({
      years: [2022, 2021, 2020, 2019],
      values: ["2", "10", "alpha"],
      valuesPerYear: [["10"], [], ["alpha"], ["2"]],
    });
  });

  it("deduplicates values across years", () => {
    expect(
      createTimeStratifiedTextMatrix(
        [
          { year: 2021, values: { Status: "active" }, quarters: [] },
          { year: 2020, values: { Status: "active" }, quarters: [] },
        ],
        "Status",
      ),
    ).toEqual({
      years: [2021, 2020],
      values: ["active"],
      valuesPerYear: [["active"], ["active"]],
    });
  });

  it("flattens list values into the matrix", () => {
    expect(
      createTimeStratifiedTextMatrix(
        [
          {
            year: 2021,
            values: { Status: ["inactive", "active", "active"] },
            quarters: [],
          },
          {
            year: 2020,
            values: { Status: ["pending", "active"] },
            quarters: [],
          },
        ],
        "Status",
      ),
    ).toEqual({
      years: [2021, 2020],
      values: ["active", "inactive", "pending"],
      valuesPerYear: [
        ["inactive", "active"],
        ["pending", "active"],
      ],
    });
  });

  it("returns an empty matrix for missing values", () => {
    expect(
      createTimeStratifiedTextMatrix(
        [{ year: 2020, values: {}, quarters: [] }],
        "Status",
      ),
    ).toEqual({ years: [2020], values: [], valuesPerYear: [[]] });
  });
});
