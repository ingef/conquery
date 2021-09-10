<#import "templates/template.html.ftl" as layout>
<@layout.layout>
    <style>
        .title {
          font-size: 18px;
          font-family: arial, sans-serif;
        }

        .inner {
          font-size: 12px;
          color: grey;
        }

        .innerBtn {
          font-size: 12px;
        }

        .btn {
          padding: 5px 5px;
        }

        .progress {
          margin: 2px;
        }

        .container {
          padding: 3px;
        }
    </style>
    <script type="text/javascript">
        var QueryCounter = 0;

        var runningQueriesTable = {};
        var notStartedQueriesTable = {};
        var failedQueriesTable = {};
        var succeedQueriesTable = {};

        var runningNbrElt = {};
        var notStartedNbrElt = {};
        var failedNbrElt = {};
        var succeedNbrElt = {};
        var reloader = {};
        var languageTag = {};
        window.onload = (event) => {
          languageTag = navigator.language || navigator.userLanguage;
          handleUpdateCheck(document.getElementById("updateCheckBox"));
          runningQueriesTable = document.getElementById("runningQueriesTable");
          notStartedQueriesTable = document.getElementById("notStartedQueriesTable");
          failedQueriesTable = document.getElementById("failedQueriesTable");
          succeedQueriesTable = document.getElementById("succeedQueriesTable");

          runningNbrElt = document.getElementById("runningNbr");
          notStartedNbrElt = document.getElementById("notStartedNbr");
          failedNbrElt = document.getElementById("failedNbr");
          succeedNbrElt = document.getElementById("succeedNbr");


        }

        function updateQueriesTable(queries) {


          clearTables();
          QueryCounter = 0;
          updateQueriesInnerCounter(queries);
          if (!queries) return;

          queries.sort(compareQueriesDate);
          for (i = 0; i < queries.length; i++) {
            QueryCounter++;
            if (queries[i].status === "RUNNING")
              runningQueriesTable.insertAdjacentHTML('beforeend', getHtmltemplate(queries[i], QueryCounter));

            if (queries[i].status === "NEW")
              notStartedQueriesTable.insertAdjacentHTML('beforeend', getHtmltemplate(queries[i], QueryCounter));


            if (queries[i].status === "FAILED")
              failedQueriesTable.insertAdjacentHTML('beforeend', getHtmltemplate(queries[i], QueryCounter));


            if (queries[i].status === "DONE")
              succeedQueriesTable.insertAdjacentHTML('beforeend', getHtmltemplate(queries[i], QueryCounter));
          }

        }

        function compareQueriesDate(query1, query2) {
          let createdAt1 = new Date(query1.createdAt);
          let createdAt2 = new Date(query2.createdAt);
          let date1Null = createdAt1 == null || createdAt1 == undefined;
          let date2Null = createdAt2 == null || createdAt2 == undefined;
          if (date1Null && date2Null) return 0;
          else if (date2Null || createdAt1 > createdAt2) return 1;
          else if (date1Null || createdAt1 < createdAt2) return -1;
          else return 0;
        }

        function updateQueriesInnerCounter(queries) {
          if (!queries) return;
          runningNbrElt.innerText = queries.filter(x => x.status === "RUNNING").length;
          notStartedNbrElt.innerText = queries.filter(x => x.status === "NEW").length;
          failedNbrElt.innerText = queries.filter(x => x.status === "FAILED").length;
          succeedNbrElt.innerText = queries.filter(x => x.status === "DONE").length;
        }

        function clearTables() {

          runningQueriesTable.innerHTML = "";
          failedQueriesTable.innerHTML = "";
          succeedQueriesTable.innerHTML = "";
          notStartedQueriesTable.innerHTML = "";

        }

        function getQueries() {
          try {
            fetch(
                '/admin/queries', {
                  method: 'get',

                  headers: {
                    'Accept': 'application/json'
                  },

                  credentials: 'same-origin'
                }).then(response => response.json()).then(queries => {
                updateQueriesTable(queries);
              })
              .catch(error => {
                console.log(error);
              });

          } catch (error) {
            console.log(error);
            return null;
          };

        }

        function handleUpdateCheck(event) {
          if (event.checked ) {
            reloader = setInterval(getQueries, 5000);
           return;
          }

          clearInterval(reloader);
          reloader = 0;


        }

        <#noparse >
            function getHtmltemplate(data,queryCounter) {
                return `

                <div class="row container" >
                  <div class="card container">
                    <div class="card-body">
                      <div class="row">
                        <div class="col title">
                          ${data.label} ( ${data.ownerName} , ${data.id.split('.')[0]} ) : ${data.id.split('.')[1]}
                        </div>
                      </div>
                      <div class="row container">
                        <div class="col inner">
                          Creation-Time : ${((new Date(data.createdAt)).toLocaleString(languageTag))}
                        </div>
                        <div class="col inner">
                          Start-Time : ${((new Date(data.startTime)).toLocaleString(languageTag))}
                        </div>
                        <div class="col inner">
                          Finish-Time : ${((new Date(data.finishTime)).toLocaleString(languageTag))}
                        </div>
                      </div>
                      <div class="row container">
                        <div class="col inner">
                          Requested-Time : ${data.requiredTime} ms
                        </div>
                        <div class="col inner">
                          Query-Type : ${data.queryType}
                        </div>
                        <div class="col">
                          <button class="btn btn-primary innerBtn" type="button" data-toggle="collapse" data-target="#query${queryCounter}" aria-expanded="false" aria-controls="query${queryCounter}" ${(data.query ? '' : 'disabled')}>
                          Show query content
                          </button>
                          <button class="btn btn-primary innerBtn" type="button" data-toggle="collapse" data-target="#error${queryCounter}" aria-expanded="false" aria-controls="error${queryCounter}" ${(data.error ? '' : 'disabled')}>
                          Show errors
                          </button>
                        </div>
                      </div>
                      <div class="row container" ${(data.progress && data.progress != null ? "" : "style=\"display: none;\"" )}>
                      <div class="col">
                        <div class="progress">
                          <div class="progress-bar ${(data.status === "RUNNING" ? "bg-warning" : (data.status === "FAILED" ? "bg-danger" : (data.status === "DONE" ? "bg-success" : "") ))}" role="progressbar" style="width: ${(data.progress && data.progress != null ? data.progress*100 : 0 )}%" aria-valuenow="${(data.progress && data.progress != null ? data.progress*100 : 0 )}" aria-valuemin="0" aria-valuemax="100">
                          ${(data.progress && data.progress != null ? data.progress*100 : 0 )} %
                        </div>
                      </div>
                    </div>
                  </div>
                  <div class="row container">
                    <div class="col">
                      <div class="collapse multi-collapse" id="query${queryCounter}">
                        <div class="card card-body">
                          <pre> ${(data.query ? JSON.stringify(data.query, undefined, 2) : '')}  </pre>
                        </div>
                      </div>
                    </div>
                    <div class="col">
                      <div class="collapse multi-collapse" id="error${queryCounter}">
                        <div class="card card-body">
                          <pre>  ${(data.error ? JSON.stringify(data.error, undefined, 2) : '')}  </pre>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
                </div>
                </div>

            `
            }
        </#noparse>
    </script>

    <h1> Queries </h1>
    <label><input id="updateCheckBox" type='checkbox' onclick='handleUpdateCheck(this);' checked> Update automatically.</label>
    <div id="accordion">
      <div class="card">
        <div class="card-header" id="headingOne">
          <h5 class="mb-0">
            <button class="btn btn-link" data-toggle="collapse" data-target="#collapseOne" aria-expanded="true" aria-controls="collapseOne">
            Running <span class="badge badge-warning" id="runningNbr">0</span>
            </button>
          </h5>
        </div>
        <div id="collapseOne" class="collapse show" aria-labelledby="headingOne" data-parent="#accordion">
          <div class="card-body">
            <div id="runningQueriesTable" class="container main"></div>
          </div>
        </div>
      </div>
      <div class="card">
        <div class="card-header" id="headingTwo">
          <h5 class="mb-0">
            <button class="btn btn-link collapsed" data-toggle="collapse" data-target="#collapseTwo" aria-expanded="false" aria-controls="collapseTwo">
            Failed <span class="badge badge-danger" id="failedNbr">0</span>
            </button>
          </h5>
        </div>
        <div id="collapseTwo" class="collapse" aria-labelledby="headingTwo" data-parent="#accordion">
          <div class="card-body">
            <div id="failedQueriesTable" class="container main"></div>
          </div>
        </div>
      </div>
      <div class="card">
        <div class="card-header" id="headingThree">
          <h5 class="mb-0">
            <button class="btn btn-link collapsed" data-toggle="collapse" data-target="#collapseThree" aria-expanded="false" aria-controls="collapseThree">
            Succeed <span class="badge badge-success" id="succeedNbr">0</span>
            </button>
          </h5>
        </div>
        <div id="collapseThree" class="collapse" aria-labelledby="headingThree" data-parent="#accordion">
          <div class="card-body">
            <div id="succeedQueriesTable" class="container main"></div>
          </div>
        </div>
      </div>
      <div class="card">
        <div class="card-header" id="headingFour">
          <h5 class="mb-0">
            <button class="btn btn-link collapsed" data-toggle="collapse" data-target="#collapseFour" aria-expanded="false" aria-controls="collapseFour">
            Not started <span class="badge badge-light" id="notStartedNbr">0</span>
            </button>
          </h5>
        </div>
        <div id="collapseFour" class="collapse" aria-labelledby="headingFour" data-parent="#accordion">
          <div class="card-body">
            <div id="notStartedQueriesTable" class="container main"></div>
          </div>
        </div>
      </div>
    </div>

</@layout.layout>