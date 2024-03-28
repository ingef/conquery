# Debuging FTL

Sometimes FTL errors are printed nowhere, e.g. if an import fails.
In this case but a breakpoint in `freemarker.template.TemplateExceptionHandler#DEBUG_HANDLER`.