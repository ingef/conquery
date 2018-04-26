const path = require('path');
const version = require('../../package.json').version;

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
      res.status(201);
      res.send(JSON.stringify({ id: 1 }));
    }, 300)
  });

  app.delete('/api/datasets/:datasetId/queries/:id', function response(req, res) {
    setTimeout(() => {
      res.setHeader('Content-Type', 'application/json');
      res.send(JSON.stringify({ id: 1 }));
    }, 300)
  });

  app.get('/api/datasets/:datasetId/queries/:id', function response(req, res) {
    setTimeout(() => {
      res.setHeader('Content-Type', 'application/json');

      const dice = Math.random();

      if (dice > 0.1 && dice <= 0.6)
        res.send(JSON.stringify({ id: 1, status: "RUNNING" }));
      else
        res.send(JSON.stringify({
          id: 1,
          status: "DONE",
          numberOfResults: 5,
          resultUrl: `/api/results/results.csv`
        }));
    }, 500)
  });

  /*
    DATASETS
  */
  app.get('/api/datasets', function response(req, res) {
    res.setHeader('Content-Type', 'application/json');

    res.send(JSON.stringify([
      { id: 'imdb', label: "IMDb" }
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
        label: Math.random() > 0.7 ? "Saved Query" : null,
        numberOfResults: Math.floor(Math.random() * 500000),
        tags: shuffleArray(possibleTags.filter(() => Math.random() < 0.3)),
        createdAt: new Date(Date.now() - Math.floor(Math.random() * 10000000)).toISOString(),
        own: Math.random() < 0.1,
        shared: Math.random() < 0.8,
        resultUrl: `/api/results/results.csv`,
        ownerName: "System",
      });

      res.send(JSON.stringify(ids));
    }, 500)
  });

  app.get('/api/datasets/:datasetId/stored-queries/:id', function response(req, res) {
    setTimeout(() => {
      res.sendFile(path.join(__dirname, './stored-queries/25.json'))
    }, 500)
  });

  app.patch('/api/datasets/:datasetId/stored-queries/:id', function response(req, res) {
    setTimeout(() => {
      res.send(JSON.stringify({}));
    }, 500)
  });

  app.delete('/api/datasets/:datasetId/stored-queries/:id', function response(req, res) {
    setTimeout(() => {
      res.setHeader('Content-Type', 'application/json');
      res.send(JSON.stringify({ id: 1 }));
    }, 300)
  });


  /*
    FORM QUERIES (COPIED FROM QUERIES)
  */
  app.post('/api/datasets/:datasetId/form-queries', function response(req, res) {
    setTimeout(() => {
      res.setHeader('Content-Type', 'application/json');
      res.status(201);
      res.send(JSON.stringify({ id: 1 }));
    }, 300)
  });

  app.delete('/api/datasets/:datasetId/form-queries/:id', function response(req, res) {
    setTimeout(() => {
      res.setHeader('Content-Type', 'application/json');
      res.send(JSON.stringify({ id: 1 }));
    }, 300)
  });

  app.get('/api/datasets/:datasetId/form-queries/:id', function response(req, res) {
    setTimeout(() => {
      res.setHeader('Content-Type', 'application/json');

      const dice = Math.random();

      if (dice > 0.1 && dice <= 0.6)
        res.send(JSON.stringify({ id: 1, status: "RUNNING" }));
      else
        res.send(JSON.stringify({
          id: 1,
          status: "DONE",
          numberOfResults: 5,
          resultUrl: `/api/results/results.csv`
        }));
    }, 500)
  });

  app.post('/api/datasets/:datasetId/import', function response(req, res) {
    setTimeout(() => {
      res.setHeader('Content-Type', 'application/json');
      res.status(201);
      res.send(JSON.stringify({
        successful: 1 + Math.floor(Math.random() * 200),
        unsuccessful: 586,
      }));
    }, 500)
  });

  app.post(
    '/api/datasets/:datasetId/concepts/:conceptId/tables/:tableId/filters/:filterId/autocomplete',
    function response (req, res) {
      setTimeout(() => {
        res.setHeader('Content-Type', 'application/json');

        const countriesRequested = req.params.filterId === 'production_country';

        const storedValues = countriesRequested ? require('./autocomplete/countries') : [
          "1008508208", "1015841172", "1011218302",
          "1007680436", "1017776144", "1003780588",
          "1000326535", "1014150881", "1017126347", "1008445564"
        ];

        const suggestions = storedValues
          .map((v, id) => ({ label: v, value: id }))
          .filter(v => v.label.toLowerCase().startsWith(req.body.text.toLowerCase()));

        res.send(JSON.stringify(suggestions));
      }, 500);
    });

  app.post(
    '/api/datasets/:datasetId/concepts/:conceptId/resolve',
    function response (req, res) {
      setTimeout(() => {
        res.setHeader('Content-Type', 'application/json');

        res.send({
          unknownConcepts: req.body.concepts.slice(0, 1),
          resolvedConcepts: req.body.concepts.slice(1),
        });
      }, 500);
    }
  );

  /*
    VERSION
  */
  app.get('/api/version', function(req, res) {
    res.setHeader('Content-Type', 'application/json');

    res.send({
      version: version,
      isDevelopment: process.env.NODE_ENV !== 'production'
    })
  });

  /*
    SEARCH
  */
  app.post('/api/datasets/:datasetId/concepts/search',
    function response (req, res) {
      setTimeout(() => {
        res.setHeader('Content-Type', 'application/json');

        const result = [];
        const awards = require('./concepts/awards');
        const movieAppearance = require('./concepts/movie_appearances');

        result.push(...findConcepts(awards, req.body.query))
        result.push(...findConcepts(movieAppearance, req.body.query))

        // see type SearchResult
        res.send({ result: result, limit: 20, size: result.length });
      }, 500);
    }
  );
};

const findConcepts = (concepts, query) => {
  return Object.keys(concepts)
    .map(key => ({id: key, label: concepts[key].label}))
    .filter(res => res.label.toLowerCase().includes(query.toLowerCase()))
    .map(res => res.id);
}
