<#import "templates/template.html.ftl" as layout>
<#import "templates/breadcrumbs.html.ftl" as breadcrumbs>
<#import "templates/accordion.html.ftl" as accordion>

<@layout.layout>
  <script type="text/javascript">

    function cancelJob(jobId) {
      event.preventDefault(); 
      fetch(
        "/admin/jobs/" + jobId + "/cancel",
        {
          method: "post",
          credentials: "same-origin"
        }
      );
    }

    function getJobs() {
      return fetch("/admin/jobs")
        .then((res) => res.json())
        .then((entries) => {
          const origins = {};
          entries.forEach((entry) => {
            const origin = entry.origin
            origins[entry.origin] = [
              ...(origins[entry.origin] ?? []),
              entry
            ];
          });
          return origins;
        });
    }

    function findNodeOrCloneTemplate(templateId, newId = "", parentNode) {
      if (newId !== "" && document.getElementById(newId)) {
        return document.getElementById(newId);
      } else {
        const clonedTemplate = document.getElementById(templateId).cloneNode(true);
        clonedTemplate.id = newId;
        parentNode?.appendChild(clonedTemplate);
        return clonedTemplate;
      }
    }

    async function refreshJobs() {
      const origins = await getJobs();
      Object.keys(origins).forEach((origin) => {
        const nodes = origins[origin];
        const accordion = findNodeOrCloneTemplate("originTemplate", "origin_" + origin, document.getElementById("nodesAccordionGroup"));
        accordion.querySelector("h5").innerText = origin;

        const accordionDetails = findNodeOrCloneTemplate("originDetailsTemplate");
        const timeDifference = (new Date() - new Date(nodes[0].timestamp)) / 1000;
        accordionDetails.querySelector(".ageString").innerText = timeDifference + "s";
        accordionDetails.querySelector(".jobsAmount").innerText = nodes
          .map((node) => node?.jobs.length ?? 0)
          .reduce((partialSum, x) => partialSum + x, 0);
        accordion.querySelector(".accordion-infotext").innerHTML = "";
        accordion.querySelector(".accordion-infotext").appendChild(accordionDetails);

        nodes.forEach((node) => {
          const fullName = node.origin + (node.dataset ? "::" + node.dataset : "")
          const nodeElement = findNodeOrCloneTemplate("nodeTemplate", "node_" + fullName, accordion.querySelector(".accordionContent"));
          nodeElement.querySelector(".nodeName").innerText = fullName;
          nodeElement.querySelector(".jobsAmount").innerText = node?.jobs?.length ?? "0";

          const jobsList = nodeElement.querySelector(".jobsList");
          if (node?.jobs.length > 0) {
            jobsList.innerHTML = "";
            node.jobs.forEach((job) => {
              const jobElement = findNodeOrCloneTemplate("jobTemplate", "job_" + job.jobId, jobsList);
              jobElement.querySelector(".jobLabel").innerText = job?.label;
              jobElement.querySelector(".jobLabel").title = job?.label;
              jobElement.querySelector(".jobProgress").style.width = Math.round((job?.progress ?? 0) * 100) + "%";
              jobElement.querySelector(".jobProgress").attributes["aria-valuenow"] = job?.progress;

              const jobActionElement = jobElement.querySelector(".jobAction");
              if (job?.cancelled) {
                jobActionElement.innerText = 'Cancelled';
              } else {
                jobActionElement.querySelector("button").onclick = () => cancelJob(job?.jobId);
              }
            });
          } else {
            jobsList.innerHTML = "";
            findNodeOrCloneTemplate("nojobsTemplate", "", jobsList);
          }
        });
      });

      // collapse accordions on page laod
      if (!this.alreadyExecuted) {
        this.alreadyExecuted = true;
        document.querySelectorAll(".collapse").forEach((elem) => elem.classList.remove("show"));
      }
    }
    refreshJobs();

    setInterval(function () {
      if(!document.getElementById("update")?.checked) return;
      refreshJobs();
    }, 5000);
</script>

  <@breadcrumbs.breadcrumbs
    labels=["Jobs"]
  />

  <div class="d-flex justify-content-end">
    <div class="custom-control custom-switch">
      <input type="checkbox" class="custom-control-input" id="update" checked>
      <label class="custom-control-label" for="update">Reload automatically</label>
    </div>
  </div>

  <@accordion.accordionGroup id="nodesAccordionGroup" class="mt-3"></@accordion.accordionGroup>
</@layout.layout>

<!-- HTML Templates -->
<div class="d-none">
  <@accordion.accordion summary="" id="originTemplate"></@accordion.accordion>

  <div id="originDetailsTemplate">
    updated <span class="ageString"></span> ago
    <span class="jobsAmount badge badge-secondary"></span>
  </div>

  <div id="nodeTemplate">
    <div class="d-flex justify-content-between align-items-center">
      <span class="nodeName py-2" style="font-size: 1.2rem;"></span>
      <div>
        <span class="jobsAmount badge badge-secondary"></span>
      </div>
    </div>
    <div class="jobsList p-2 mb-3 border" style="max-height: 200px; overflow: auto;"></div>
  </div>

  <div id="nojobsTemplate" class="w-100 text-black-50 text-center">No jobs in this node</div>

  <div
    id="jobTemplate"
    class="d-flex justify-content-between align-items-center py-2"
    style="gap: 25px; border-bottom: 1px solid #ccc"
  >
    <div
      class="jobLabel text-nowrap"
      style="overflow: hidden; text-overflow: ellipsis; flex-basis: 600px;"
    ></div>
    <div class="progress position-relative" style="flex-grow: 1;">
      <div class="jobProgress progress-bar" role="progressbar" aria-valuemin="0" aria-valuemax="1"></div>
    </div>
    <div class="jobAction d-flex justify-content-center" style="flex-basis: 80px;">
      <button
        type="button"
        class="btn btn-danger btn-sm text-white fas fa-ban"
        data-toggle="tooltip"
        data-placement="bottom"
        title="Cancel Job"
      />
    </div>
  </div>
</div>
