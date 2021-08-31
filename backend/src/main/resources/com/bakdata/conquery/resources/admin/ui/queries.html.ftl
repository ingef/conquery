<#import "templates/template.html.ftl" as layout>
<@layout.layout>
<style>
   .title{
        font-size: 18px;
        font-family: arial, sans-serif;
   }

   .inner{
           font-size: 12px;
           color: grey;
   }
   .innerBtn{
               font-size: 12px;
   }

   .btn {
        padding : 5px 5px;
   }
   .progress{
     margin : 8px;
   }
   table {
   font-family: arial, sans-serif;
   border-collapse: collapse;
   width: 100%;
   }
   td, th {
   border: 1px solid white;
   text-align: left;
   padding: 8px;
   }
   tr:nth-child(even) {
   background-color: #e6ffff;
   }

</style>
<script type="text/javascript">
var queries = [];
var k = 0;
var queryCounter = 0;

var runningQueriesTable = {};
var notStartedQueriesTable = {};
var failedQueriesTable = {};
var succeedQueriesTable = {};

var runningNbrElt = {};
var notStartedNbrElt = {};
var failedNbrElt = {};
var succeedNbrElt = {};
window.onload = (event) => {
    setInterval(getQueries, 10000);
    runningQueriesTable = document.getElementById("runningQueriesTable");
    notStartedQueriesTable = document.getElementById("notStartedQueriesTable");
    failedQueriesTable = document.getElementById("failedQueriesTable");
    succeedQueriesTable = document.getElementById("succeedQueriesTable");

    runningNbrElt = document.getElementById("runningNbr");
    notStartedNbrElt = document.getElementById("notStartedNbr");
    failedNbrElt = document.getElementById("failedNbr");
    succeedNbrElt = document.getElementById("succeedNbr");


}

function updateQueriesTable() {
    clearTables();
    queryCounter = 0;
    updateQueriesInnerCounter();
    for (i = 0; i < queries.length; i++) {
        queryCounter++;
        if(queries[i].status === "RUNNING")
            runningQueriesTable.insertAdjacentHTML('beforeend', getHtmltemplate(queries[i]));

        if(queries[i].status === "NEW")
            notStartedQueriesTable.insertAdjacentHTML('beforeend', getHtmltemplate(queries[i]));


        if(queries[i].status === "FAILED")
            failedQueriesTable.insertAdjacentHTML('beforeend', getHtmltemplate(queries[i]));


        if(queries[i].status === "DONE")
            succeedQueriesTable.insertAdjacentHTML('beforeend', getHtmltemplate(queries[i]));
    }
    if (k <= 30)
        k++;
    else k = 0;
}

function updateQueriesInnerCounter() {
    runningNbrElt.innerText = queries.filter(x => x.status === "RUNNING").length;
    notStartedNbrElt.innerText = queries.filter(x => x.status === "NEW").length;
    failedNbrElt.innerText = queries.filter(x => x.status === "FAILED").length;
    succeedNbrElt.innerText = queries.filter(x => x.status === "DONE").length;
}

function clearTables() {
    for (var i = runningQueriesTable.rows.length - 1; i > 0; i--) {
        runningQueriesTable.deleteRow(i);
    }

    for (var i = notStartedQueriesTable.rows.length - 1; i > 0; i--) {
        notStartedQueriesTable.deleteRow(i);
    }

    for (var i = failedQueriesTable.rows.length - 1; i > 0; i--) {
        failedQueriesTable.deleteRow(i);
    }


    for (var i = succeedQueriesTable.rows.length - 1; i > 0; i--) {
        succeedQueriesTable.deleteRow(i);
    }
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
                }).then(response => response.json()).then(data => {
                queries = data;
                updateQueriesTable();
            })
            .catch(error => {
                console.log(error);
            });

    } catch (error) {
        console.log(error);
        return null;
    };

}
<#noparse >
    function getHtmltemplate(data) {
        return `


<tr>
   <td >
<div class="card container">
  <div class="card-body">
      <div class="row">
         <div class="col title">
            ${data.label} ( ${data.ownerName} , ${data.id.split('.')[0]} ) : ${data.id.split('.')[1]}
         </div>
      </div>



      <div class="row container">
         <div class="col inner">
            Creation-Time : ${((new Date(data.createdAt)).toLocaleString("de-De"))}
         </div>
         <div class="col inner">
            Start-Time : ${((new Date(data.startTime)).toLocaleString("de-De"))}
         </div>
         <div class="col inner">
            Finish-Time : ${((new Date(data.finishTime)).toLocaleString("de-De"))}
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



      <div class="row container">
        <div class="col">
             <div class="progress">
                <div class="progress-bar ${(data.status === "RUNNING" ? "bg-warning" : (data.status === "FAILED" ? "bg-danger" : (data.status === "DONE" ? "bg-success" : "") ))}" role="progressbar" style="width: ${(data.progress+k)}%" aria-valuenow="${(data.progress+k)}" aria-valuemin="0" aria-valuemax="100">
                    ${(data.progress+k)} %
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
   </td>
</tr>



    `
    }
    </#noparse>
</script>
<h1> Queries </h1>



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

           <table id="runningQueriesTable">
                    <tr>
                       <th>#</th>
                    </tr>
           </table>
      </div>
    </div>
  </div>
  <div class="card">
    <div class="card-header" id="headingTwo">
      <h5 class="mb-0">
        <button class="btn btn-link collapsed" data-toggle="collapse" data-target="#collapseTwo" aria-expanded="false" aria-controls="collapseTwo">
          Not started <span class="badge badge-light" id="notStartedNbr">0</span>
        </button>
      </h5>
    </div>
    <div id="collapseTwo" class="collapse" aria-labelledby="headingTwo" data-parent="#accordion">
      <div class="card-body">

                 <table id="notStartedQueriesTable">
                          <tr>
                             <th>#</th>
                          </tr>
                 </table>

      </div>
    </div>
  </div>
  <div class="card">
    <div class="card-header" id="headingThree">
      <h5 class="mb-0">
        <button class="btn btn-link collapsed" data-toggle="collapse" data-target="#collapseThree" aria-expanded="false" aria-controls="collapseThree">
          Failed <span class="badge badge-danger" id="failedNbr">0</span>
        </button>
      </h5>
    </div>
    <div id="collapseThree" class="collapse" aria-labelledby="headingThree" data-parent="#accordion">
      <div class="card-body">
                   <table id="failedQueriesTable">
                            <tr>
                               <th>#</th>
                            </tr>
                   </table>
        </div>
    </div>
  </div>
  <div class="card">
      <div class="card-header" id="headingFour">
        <h5 class="mb-0">
          <button class="btn btn-link collapsed" data-toggle="collapse" data-target="#collapseFour" aria-expanded="false" aria-controls="collapseFour">
            Succeed <span class="badge badge-success" id="succeedNbr">0</span>
          </button>
        </h5>
      </div>
      <div id="collapseFour" class="collapse" aria-labelledby="headingFour" data-parent="#accordion">
        <div class="card-body">
                   <table id="succeedQueriesTable">
                            <tr>
                               <th>#</th>
                            </tr>
                   </table>
        </div>
      </div>
   </div>
</div>







</@layout.layout>