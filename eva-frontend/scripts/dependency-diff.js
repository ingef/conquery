const path = require("path");

const CONQUERY = {
  name: "conquery",
  path: "../conquery/frontend/package.json"
};
const EVA = {
  name: "eva",
  path: "../package.json",
};

CONQUERY.packageJson = require(path.join(__dirname, CONQUERY.path ));
EVA.packageJson = require(path.join(__dirname, EVA.path));

function getDependencies(packageJson) {
  return {
    ...(packageJson.dependencies || {}),
    ...(packageJson.devDependencies || {})
  };
}

function diff(configA, configB) {
  const a = getDependencies(configA.packageJson);
  const b = getDependencies(configB.packageJson);

  let diffCount = 0, missCount = 0;

  console.log("\n\n---------------------------------")
  console.log(`Checking all dependencies in "${configA.name}" for existence / diff in "${configB.name}".`);

  Object.keys(a).forEach(k => {
    if (b[k]) {
      if (b[k] !== a[k]) {
        console.log(`DIFF ! ${k} : ${a[k]} vs ${b[k]}`);
        diffCount += 1;
      }
    } else {
      console.log(`MISS ${k}@${a[k]}`);
      missCount += 1;
    }
  });

  console.log("---------------------------------")
  if (missCount === 0) {
    console.log(`No dependencies from "${configA.name}" missing in "${configB.name}".`);
  } else {
    console.log(`${missCount} dependencies from ${configA.name} missing in ${configB.name}.`);
  }

  if (diffCount === 0) {
    console.log(`No dependencies with differing version.`);
  } else {
    console.log(`${diffCount} dependencies with differing version.`);
  }
}


diff(CONQUERY, EVA);
diff(EVA, CONQUERY);
