const path = require('path');

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

// Simulate API
module.exports = function (app, port) {
  /*
    QUERIES
  */
  app.post('/api/datasets/:datasetId/queries', function response(req, res) {
    setTimeout(() => {
      res.setHeader('Content-Type', 'application/json');

      if (Math.random() > 0.2) {
        res.status(201);
        res.send(JSON.stringify({ id: 1 }));
      } else {
        res.status(404);
        res.send(JSON.stringify({ message: "Something went wrong" }));
      }
    }, 300)
  });

  app.delete('/api/datasets/:datasetId/queries/:id', function response(req, res) {
    setTimeout(() => {
      res.setHeader('Content-Type', 'application/json');

      if (Math.random() > 0.2) {
        res.send(JSON.stringify({ id: 1 }));
      } else {
        res.status(404);
        res.send(JSON.stringify({ message: "Something went wrong" }));
      }
    }, 300)
  });

  app.get('/api/datasets/:datasetId/queries/:id', function response(req, res) {
    setTimeout(() => {
      res.setHeader('Content-Type', 'application/json');

      const dice = Math.random();

      if (dice > 0.1 && dice <= 0.6) {
        res.send(JSON.stringify({ id: 1, status: "RUNNING" }));
      } else if (dice > 0.6) {
        res.send(JSON.stringify({
          id: 1,
          status: "DONE",
          numberOfResults: 5,
          resultUrl: `http://localhost:${port}/api/results/results.csv`
        }));
      } else {
        res.status(422);
        res.send(JSON.stringify({ message: "Something went wrong" }));
      }
    }, 500)
  });

  /*
    DATASETS
  */
  app.get('/api/datasets', function response(req, res) {
    res.setHeader('Content-Type', 'application/json');

    res.send(JSON.stringify([
      { id: 1, label: "Research database" },
      { id: 2, label: "Development database" },
      { id: 3, label: "Test database" }
    ]));
  });

  /*
    QUERY RESULT DOWNLOAD
  */
  app.get('/api/results/:filename', function response(req, res) {
    res.sendFile(path.join(__dirname, `./results/${req.params.filename}`))
  });

  /*
    CONCEPTS
  */
  app.get('/api/datasets/:id/concepts', function response(req, res) {
    res.sendFile(path.join(__dirname, './concepts.json'))
  });

  app.get('/api/datasets/:datasetId/concepts/:id', function response(req, res) {
    res.sendFile(path.join(__dirname, `./concepts/${req.params.id}.json`))
  });

  /*
    STORED QUERIES
  */
  app.get('/api/datasets/:datasetId/stored-queries', function response(req, res) {
    res.setHeader('Content-Type', 'application/json');

    setTimeout(() => {
      const ids = [];
      const possibleTags = [
        "research", "fun", "test", "group 1", "important", "jk", "interesting"
      ];

      for (var i = 25600; i < 35600; i++) ids.push({
        id: i,
        label: Math.random() > 0.7 ? "Gespeicherte Anfrage" : null,
        numberOfResults: Math.floor(Math.random() * 500000),
        tags: shuffleArray(possibleTags.filter(() => Math.random() < 0.3)),
        createdAt: new Date(Date.now() - Math.floor(Math.random() * 10000000)).toISOString(),
        own: Math.random() < 0.1,
        shared: Math.random() < 0.8,
        resultUrl: `http://localhost:${port}/api/results/results.csv`,
        ownerName: "System",
      });

      res.send(JSON.stringify(ids));
    }, 500)
  });

  app.get('/api/datasets/:datasetId/stored-queries/:id', function response(req, res) {
    setTimeout(() => {
      const dice = Math.random();

      if (dice < 0.1) {
        res.status(422);
        res.send(JSON.stringify({ message: "Something went wrong" }));
      } else {
        res.sendFile(path.join(__dirname, './stored-queries/25.json'))
      }
    }, 500)
  });

  app.patch('/api/datasets/:datasetId/stored-queries/:id', function response(req, res) {
    setTimeout(() => {
      const dice = Math.random();

      if (dice < 0.1) {
        res.status(422);
        res.send(JSON.stringify({ message: "Something went wrong" }));
      } else {
        res.send(JSON.stringify({}));
      }
    }, 500)
  });

  app.delete('/api/datasets/:datasetId/stored-queries/:id', function response(req, res) {
    setTimeout(() => {
      res.setHeader('Content-Type', 'application/json');

      if (Math.random() > 0.2) {
        res.send(JSON.stringify({ id: 1 }));
      } else {
        res.status(404);
        res.send(JSON.stringify({ message: "Something went wrong" }));
      }
    }, 300)
  });


  /*
    FORM QUERIES (COPIED FROM QUERIES)
  */
  app.post('/api/datasets/:datasetId/form-queries', function response(req, res) {
    setTimeout(() => {
      res.setHeader('Content-Type', 'application/json');

      if (Math.random() > 0.2) {
        res.status(201);
        res.send(JSON.stringify({ id: 1 }));
      } else {
        res.status(404);
        res.send(JSON.stringify({ message: "Something went wrong" }));
      }
    }, 300)
  });

  app.delete('/api/datasets/:datasetId/form-queries/:id', function response(req, res) {
    setTimeout(() => {
      res.setHeader('Content-Type', 'application/json');

      if (Math.random() > 0.2) {
        res.send(JSON.stringify({ id: 1 }));
      } else {
        res.status(404);
        res.send(JSON.stringify({ message: "Something went wrong" }));
      }
    }, 300)
  });

  app.get('/api/datasets/:datasetId/form-queries/:id', function response(req, res) {
    setTimeout(() => {
      res.setHeader('Content-Type', 'application/json');

      const dice = Math.random();

      if (dice > 0.1 && dice <= 0.6) {
        res.send(JSON.stringify({ id: 1, status: "RUNNING" }));
      } else if (dice > 0.6) {
        res.send(JSON.stringify({
          id: 1,
          status: "DONE",
          numberOfResults: 5,
          resultUrl: `http://localhost:${port}/api/results/results.csv`
        }));
      } else {
        res.status(422);
        res.send(JSON.stringify({ message: "Something went wrong" }));
      }
    }, 500)
  });

  app.post('/api/datasets/:datasetId/import', function response(req, res) {
    setTimeout(() => {
      res.setHeader('Content-Type', 'application/json');

      const dice = Math.random();

      if (dice < 0.5) {
        res.status(201);
        res.send(JSON.stringify({
          successful: 1 + Math.floor(Math.random() * 200),
          unsuccessful: 586,
        }));
      } else {
        res.status(422);
        res.send(JSON.stringify({
          message: "Couldn't parse a single row",
          successful: 0,
          unsuccessful: 586,
        }));
      }
    }, 500)
  });

  app.post(
    '/api/datasets/:datasetId/concepts/:conceptId/tables/:tableId/filters/:filterId/autocomplete',
    function response (req, res) {
      setTimeout(() => {
        res.setHeader('Content-Type', 'application/json');
        const storedValues = [
          "1008508208", "1015841172", "1011218302",
          "1007680436", "1017776144", "1003780588",
          "1000326535", "1014150881", "1017126347", "1008445564"
        ];

        const suggestions = storedValues
          .filter(v => v.startsWith(req.body.prefix))
          .map((v, id) => ({ label: v, value: id }));

        res.send(JSON.stringify(suggestions));
      }, 500);
    });

  app.post(
    '/api/datasets/:datasetId/concepts/:conceptId/resolve',
    function response (req, res) {
      setTimeout(() => {
        res.setHeader('Content-Type', 'application/json');

        res.send({
          resolvedConcepts: req.body.concepts.slice(0, 1),
          unknownConcepts: req.body.concepts.slice(1)
        });
      }, 500);
    }
  )
};
