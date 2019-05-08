const path = require("path");
const glob = require("glob");
const version = require("../../package.json").version;

// Taken from:
// http://stackoverflow.com/questions/2450954/how-to-randomize-shuffle-a-javascript-array
function shuffleArray(array) {
  for (var i = array.length - 1; i > 0; i--) {
    var j = Math.floor(Math.random() * (i + 1));
    var temp = array[i];
    array[i] = array[j];
    array[j] = temp;
  }
  return array;
}

const LONG_DELAY = 500;
const SHORT_DELAY = 300;
const NO_DELAY = 10;

// Simulate API
module.exports = function(app, port) {
  /*
    QUERIES
  */
  app.post("/api/datasets/:datasetId/queries", function response(req, res) {
    setTimeout(() => {
      res.setHeader("Content-Type", "application/json");
      res.status(201);
      res.send(JSON.stringify({ id: 1 }));
    }, NO_DELAY);
  });

  app.delete("/api/datasets/:datasetId/queries/:id", function response(
    req,
    res
  ) {
    setTimeout(() => {
      res.setHeader("Content-Type", "application/json");
      res.send(JSON.stringify({ id: 1 }));
    }, SHORT_DELAY);
  });

  app.get("/api/datasets/:datasetId/queries/:id", function response(req, res) {
    setTimeout(() => {
      res.setHeader("Content-Type", "application/json");

      const dice = Math.random();

      if (dice > 0.1 && dice <= 0.6)
        res.send(JSON.stringify({ id: 1, status: "RUNNING" }));
      else
        res.send(
          JSON.stringify({
            id: 1,
            status: "DONE",
            numberOfResults: 5,
            resultUrl: `/api/results/results.csv`
          })
        );
    }, LONG_DELAY);
  });

  /*
    DATASETS
  */
  app.get("/api/datasets", function response(req, res) {
    res.setHeader("Content-Type", "application/json");

    res.send(
      JSON.stringify([
        { id: "imdb", label: "IMDb" },
        { id: "empty-set", label: "Empty Dataset" },
        {
          id: "another-empty-set",
          label: "Another empty dataset with a long name"
        }
      ])
    );
  });

  /*
    QUERY RESULT DOWNLOAD
  */
  app.get("/api/results/:filename", function response(req, res) {
    res.sendFile(path.join(__dirname, `./results/${req.params.filename}`));
  });

  /*
    CONCEPTS
  */
  app.get("/api/datasets/:id/concepts", function response(req, res) {
    res.sendFile(path.join(__dirname, "./concepts.json"));
  });

  app.get("/api/datasets/:datasetId/concepts/:id", function response(req, res) {
    res.sendFile(path.join(__dirname, `./concepts/${req.params.id}.json`));
  });

  /*
    STORED QUERIES
  */
  app.get("/api/datasets/:datasetId/stored-queries", function response(
    req,
    res
  ) {
    res.setHeader("Content-Type", "application/json");

    setTimeout(() => {
      const ids = [];
      const possibleTags = [
        "research",
        "fun",
        "test",
        "group 1",
        "important",
        "jk",
        "interesting"
      ];

      for (var i = 25600; i < 35600; i++)
        ids.push({
          id: i,
          label: Math.random() > 0.7 ? "Saved Query" : null,
          numberOfResults: Math.floor(Math.random() * 500000),
          tags: shuffleArray(possibleTags.filter(() => Math.random() < 0.3)),
          createdAt: new Date(
            Date.now() - Math.floor(Math.random() * 10000000)
          ).toISOString(),
          own: Math.random() < 0.1,
          shared: Math.random() < 0.8,
          resultUrl: `/api/results/results.csv`,
          ownerName: "System"
        });

      res.send(JSON.stringify(ids));
    }, LONG_DELAY);
  });

  app.get("/api/datasets/:datasetId/stored-queries/:id", function response(
    req,
    res
  ) {
    setTimeout(() => {
      res.sendFile(path.join(__dirname, "./stored-queries/25.json"));
    }, LONG_DELAY);
  });

  app.patch("/api/datasets/:datasetId/stored-queries/:id", function response(
    req,
    res
  ) {
    setTimeout(() => {
      res.send(JSON.stringify({}));
    }, LONG_DELAY);
  });

  app.delete("/api/datasets/:datasetId/stored-queries/:id", function response(
    req,
    res
  ) {
    setTimeout(() => {
      res.setHeader("Content-Type", "application/json");
      res.send(JSON.stringify({ id: 1 }));
    }, SHORT_DELAY);
  });

  /*
    FORM QUERIES (COPIED FROM QUERIES)
  */
  app.post("/api/datasets/:datasetId/form-queries", function response(
    req,
    res
  ) {
    setTimeout(() => {
      res.setHeader("Content-Type", "application/json");
      res.status(201);
      res.send(JSON.stringify({ id: 1 }));
    }, SHORT_DELAY);
  });

  app.delete("/api/datasets/:datasetId/form-queries/:id", function response(
    req,
    res
  ) {
    setTimeout(() => {
      res.setHeader("Content-Type", "application/json");
      res.send(JSON.stringify({ id: 1 }));
    }, SHORT_DELAY);
  });

  app.get("/api/datasets/:datasetId/form-queries/:id", function response(
    req,
    res
  ) {
    setTimeout(() => {
      res.setHeader("Content-Type", "application/json");

      const dice = Math.random();

      if (dice > 0.1 && dice <= 0.6)
        res.send(JSON.stringify({ id: 1, status: "RUNNING" }));
      else
        res.send(
          JSON.stringify({
            id: 1,
            status: "DONE",
            numberOfResults: 5,
            resultUrl: `/api/results/results.csv`
          })
        );
    }, LONG_DELAY);
  });

  app.post("/api/datasets/:datasetId/import", function response(req, res) {
    setTimeout(() => {
      res.setHeader("Content-Type", "application/json");
      res.status(201);
      res.send(
        JSON.stringify({
          successful: 1 + Math.floor(Math.random() * 200),
          unsuccessful: 586
        })
      );
    }, LONG_DELAY);
  });

  app.post(
    "/api/datasets/:datasetId/concepts/:conceptId/tables/:tableId/filters/:filterId/autocomplete",
    function response(req, res) {
      setTimeout(() => {
        res.setHeader("Content-Type", "application/json");

        const text = req.body.text.toLowerCase();
        const countriesRequested = req.params.filterId === "production_country";

        const storedValues = countriesRequested
          ? require("./autocomplete/countries")
          : [
              "1008508208",
              "1015841172",
              "1011218302",
              "1007680436",
              "1017776144",
              "1003780588",
              "1000326535",
              "1014150881",
              "1017126347",
              "1008445564"
            ];

        const suggestions = storedValues
          .map((v, id) => ({
            label: v,
            value: id,
            templateValues: { company: "Columbia Pictures Corporation" }
          }))
          .filter(v => v.label.toLowerCase().startsWith(text));

        res.send(JSON.stringify(suggestions));
      }, LONG_DELAY);
    }
  );

  app.post(
    "/api/datasets/:datasetId/concepts/:conceptId/resolve",
    function response(req, res) {
      setTimeout(() => {
        res.setHeader("Content-Type", "application/json");

        const { concepts } = req.body;

        res.send({
          unknownConcepts: concepts.slice(5),
          resolvedConcepts: concepts.slice(1)
        });
      }, LONG_DELAY);
    }
  );

  /*
    VERSION
  */
  app.get("/api/version", function(req, res) {
    res.setHeader("Content-Type", "application/json");

    res.send({
      version: version,
      isDevelopment: process.env.NODE_ENV !== "production"
    });
  });

  /*
    For DND File see ./app/api/dnd
  */
  app.post(
    "/api/datasets/:datasetId/concepts/:conceptId/tables/:tableId/filters/:filterId/resolve",
    function response(req, res) {
      setTimeout(() => {
        res.setHeader("Content-Type", "application/json");

        const { values } = req.body;

        if (req.params.filterId !== "production_country") return null;

        const countries = require("./autocomplete/countries");
        const unknownCodes = values.filter(val => !countries.includes(val));
        const resolvedValues = values.filter(val => countries.includes(val));

        res.send({
          unknownCodes: unknownCodes,
          resolvedFilter: {
            tableId: req.params.tableId,
            filterId: req.params.filterId,
            value: resolvedValues.map(val => ({ label: val, value: val }))
          }
        });
      }, LONG_DELAY);
    }
  );

  app.get("/api/config/frontend", (req, res) => {
    res.setHeader("Content-Type", "application/json");

    const config = require("./config.json");

    config.version = version;

    res.send(config);
  });
};

