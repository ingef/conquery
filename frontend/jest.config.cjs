module.exports = {
  preset: "ts-jest",
  testEnvironment: "jsdom",
  extensionsToTreatAsEsm: [".ts"],
  transform: {
    "^.+\\.tsx?$": ["@swc/jest"],
  },
  setupFiles: ["./src/test/setup.ts"],
};

// "isolatedModules": false

// {
//   useESM: true,
// },
