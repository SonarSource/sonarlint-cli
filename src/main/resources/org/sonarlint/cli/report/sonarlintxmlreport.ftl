<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<sonarlintreport>
  <files>
  <#list report.getResourceReports() as resourceReport>
    <file name="${resourceReport.getName()}">
      <issues total="${resourceReport.getTotal().getCountInCurrentAnalysis()?c}">
        <#list resourceReport.getCategoryReports() as categoryReport>
        <issue severity="${categoryReport.getSeverity()?lower_case}" count="${categoryReport.getTotal().getCountInCurrentAnalysis()?c}">${categoryReport.getName()?xml}</issue>
        </#list>
      </issues>
    </file>
  </#list>
  </files>
</sonarlintreport>