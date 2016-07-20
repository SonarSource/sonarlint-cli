<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<sonarlintreport>
  <files>
  <#list report.getResourceReports() as resourceReport>
    <file name="${resourceReport.getName()}">
      <issues total="${resourceReport.getTotal().getCountInCurrentAnalysis()?c}">
        <#list resourceReport.getIssues() as issue>
        <issue severity="${issue.getSeverity()?lower_case}" key="${issue.getRuleKey()?xml}" name="${issue.getRuleName()?xml}" line="${(issue.getStartLine()!0)?c}" offset="${(issue.getStartLineOffset()!0)?c}"/>
        </#list>
      </issues>
    </file>
  </#list>
  </files>
</sonarlintreport>