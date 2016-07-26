<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN">
<html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
  <title>SonarLint report of ${report.getTitle()?html}</title>
  <link href="sonarlintreport_files/sonar.css" media="all" rel="stylesheet" type="text/css">
  <link rel="shortcut icon" type="image/x-icon" href="sonarlintreport_files/favicon.ico">
  <script type="text/javascript" src="sonarlintreport_files/jquery.min.js"></script>
  <script type="text/javascript">
    var issuesPerResource = [
    <#list report.getResourceReports() as resourceReport>
      [
        <#list resourceReport.getIssues() as issue>
          {'k': '${report.issueId(issue)?html}', 'r': 'R${issue.getRuleKey()}', 'l': ${(issue.getStartLine()!0)?c}, 's': '${issue.getSeverity()?lower_case}'}<#if issue_has_next>,</#if>
        </#list>
      ]
      <#if resourceReport_has_next>,</#if>
    </#list>
    ];
    var nbResources = ${report.getResourcesWithReport()?size?c};
    var separators = new Array();

    function showLine(fileIndex, lineId) {
      var elt = $('#' + fileIndex + 'L' + lineId);
      if (elt != null) {
        elt.show();
      }
      elt = $('#' + fileIndex + 'LV' + lineId);
      if (elt != null) {
        elt.show();
      }
    }

    /* lineIds must be sorted */
    function showLines(fileIndex, lineIds) {
      var lastSeparatorId = 9999999;
      for (var lineIndex = 0; lineIndex < lineIds.length; lineIndex++) {
        var lineId = lineIds[lineIndex];
        if (lineId > 0) {
          if (lineId > lastSeparatorId) {
            var separator = $('#' + fileIndex + 'S' + lastSeparatorId);
            if (separator != null) {
              separator.addClass('visible');
              separators.push(separator);
            }
          }

          for (var i = -2; i < 3; ++i) {
            showLine(fileIndex, lineId + i);
          }
          lastSeparatorId = lineId + 2;
        }
      }
    }
     function hideAll() {
       $('tr.row').hide();
       $('div.issue').hide();
       for (var separatorIndex = 0; separatorIndex < separators.length; separatorIndex++) {
         separators[separatorIndex].removeClass('visible');
       }
       separators.length = 0;
       $('.sources td.ko').removeClass('ko');
     }

     function showIssues(fileIndex, issues) {
       $.each(issues, function(index, issue) {
         $('#' + issue['k']).show();
         $('#' + fileIndex + 'L' + issue['l'] + ' td.line').addClass('ko');
       });
       var showResource = issues.length > 0;
       if (showResource) {
         $('#resource-' + fileIndex).show();
       } else {
         $('#resource-' + fileIndex).hide();
       }
     }


    function refreshFilters(updateSelect) {
      if (updateSelect) {
        populateSelectFilter();
      }
      var ruleFilter = $('#rule_filter').val();

      hideAll();
      $('.all').removeClass('all-masked');
      for (var resourceIndex = 0; resourceIndex < nbResources; resourceIndex++) {
        var filteredIssues = $.grep(issuesPerResource[resourceIndex], function(v) {
              return (ruleFilter == '' || v['r'] == ruleFilter || v['s'] == ruleFilter);
            }
        );

        var linesToDisplay = $.map(filteredIssues, function(v, i) {
          return v['l'];
        });

        linesToDisplay.sort();// the showLines() requires sorted ids
        showLines(resourceIndex, linesToDisplay);
        showIssues(resourceIndex, filteredIssues);
      }
    }


    var severityFilter = [
    <#assign severities = report.getSummary().getTotalBySeverity()>
       <#list severities?keys as severity>
       { "key": "${severity?lower_case}",
         "label": "${severity?lower_case?cap_first}",
         "total": ${severities[severity].getCountInCurrentAnalysis()?c}
       }<#if severity_has_next>,</#if>
       </#list>
    ];

    var ruleFilter = [
    <#assign rules = report.getSummary().getTotalByRuleKey()>
       <#list rules?keys as ruleKey>
       { "key": "${ruleKey}",
         "label": "${report.getRuleName(ruleKey)?html}",
         "total": ${rules[ruleKey].getCountInCurrentAnalysis()?c}
       }<#if ruleKey_has_next>,</#if>
       </#list>
    ].sort(function(a, b) {
        var x = a.label; var y = b.label;
        return ((x < y) ? -1 : ((x > y) ? 1 : 0));
    });

    function populateSelectFilter() {
       var ruleFilterSelect = $('#rule_filter');
       ruleFilterSelect.empty().append(function() {
         var output = '';
         output += '<option value="" selected>Filter by:</option>';
         output += '<optgroup label="Severity">';
         $.each(severityFilter, function(key, value) {
           if (value.total > 0) {
             output += '<option value="' + value.key + '">' + value.label + ' (' + value.total + ')</option>';
           }
         });
         output += '<optgroup label="Rule">';
         $.each(ruleFilter, function(key, value) {
           if (value.total > 0) {
             output += '<option value="R' + value.key + '">' + value.label + ' (' + value.total + ')</option>';
           }
         });
         return output;
       });
    }
  </script>
</head>
<body>
<div id="reportHeader">
  <div id="logo"><img src="sonarlintreport_files/sonarlint.png" alt="SonarLint"/></div>
  <div class="title">SonarLint Report</div>
  <div class="subtitle">${report.getTitle()?html} - ${report.getDate()?datetime}</div>
</div>

<#if report.noIssues()>
<div id="content">
  <div class="banner">No issues</div>
</div>
<#else>
<div id="content">

  <div id="summary">
  <table width="100%">
    <tbody>
    <tr>
      <#assign size = '33'>
      <td align="center" width="${size}%">
        <h3>Issues</h3>
        <#if report.getSummary().getTotal().getCountInCurrentAnalysis() gt 0>
          <span class="big worst">${report.getSummary().getTotal().getCountInCurrentAnalysis()?c}</span>
        <#else>
        <span class="big">0</span>
      </#if>
      </td>
    </tr>
    </tbody>
  </table>
  <br/>
  <table width="100%" class="data">
    <thead>
    <tr class="total">
      <th colspan="2" align="left">
          Issues per Rule
      </th>
      <th align="right" width="1%" nowrap>Issues</th>
    </tr>
    </thead>
    <tbody>
      <#list report.getSummary().getCategoryReports() as categoryReport>
      <tr class="hoverable">
        <td width="20">
          <i class="icon-severity-${categoryReport.getSeverity()?lower_case}"></i>
        </td>
        <td align="left">
          ${categoryReport.getName()?html}
        </td>
        <td align="right">
          <#if categoryReport.getTotal().getCountInCurrentAnalysis() gt 0>
            <span class="worst">${categoryReport.getTotal().getCountInCurrentAnalysis()?c}</span>
          <#else>
            <span>0</span>
          </#if>
        </td>
      </tr>
      </#list>
    </tbody>
  </table>
  </div>

  <br/>

  <div class="banner">
  <input type="checkbox" id="new_filter" onclick="refreshFilters(true)" checked="checked" />

  <select id="rule_filter" onchange="refreshFilters(false)"></select>
  </div>

  <div id="summary-per-file">
  <#list report.getResourceReports() as resourceReport>
  <#assign issueId=0>
  <table width="100%" class="data" id="resource-${resourceReport_index?c}">
    <thead>
    <tr class="total">
      <th align="left" colspan="2" nowrap>
        <div class="file_title">
          <img src="sonarlintreport_files/${resourceReport.getType()}.png" title="Resource icon"/>
          <a href="#" onclick="$('.resource-details-${resourceReport_index?c}').toggleClass('masked'); return false;" style="color: black">${resourceReport.getName()}</a>
        </div>
      </th>
      <th align="right" width="1%" nowrap class="resource-details-${resourceReport_index?c}">
        <#if resourceReport.getTotal().getCountInCurrentAnalysis() gt 0>
          <span class="worst" id="total">${resourceReport.getTotal().getCountInCurrentAnalysis()?c}</span>
        <#else>
          <span id="current-total">0</span>
        </#if>
        <br/>Issues
      </th>
    </tr>
    </thead>
    <tbody class="resource-details-${resourceReport_index?c}">
    <#list resourceReport.getCategoryReports() as categoryReport>
      <tr class="hoverable">
        <td width="20">
          <i class="icon-severity-${categoryReport.getSeverity()?lower_case}"></i>
        </td>
        <td align="left">
          ${categoryReport.getName()?html}
        </td>
        <td align="right">
          ${categoryReport.getTotal().getCountInCurrentAnalysis()?c}
        </td>
      </tr>
    </#list>
    <#assign colspan = '3'>
    <#assign issues=resourceReport.getIssuesAtLine(0)>
      <#if issues?has_content>
      <tr class="globalIssues">
        <td colspan="${colspan}">
          <#list issues as issue>
            <div class="issue" id="$report.issueId(issue)?html">
              <div class="vtitle">
                <i class="icon-severity-${issue.getSeverity()?lower_case}"></i>
                <#if issue.getMessage()?has_content>
                  <span class="rulename">${issue.getMessage()?html}</span>
                <#else>
                  <span class="rulename">${issue.getRuleName()}</span>
                </#if>
                &nbsp;
                <img src="sonarlintreport_files/sep12.png">&nbsp;
                <span class="rule_key">${issue.getRuleKey()}</span>
              </div>
              <div class="discussionComment">
                ${issue.getRuleName()}
              </div>
            </div>
            <#assign issueId = issueId + 1>
          </#list>
        </td>
      </tr>
      </#if>
      <tr>
        <td colspan="${colspan}">
          <table class="sources" border="0" cellpadding="0" cellspacing="0">
            <#list sources.getEscapedSource(resourceReport.getPath()) as line>
              <#assign lineIndex=line_index+1>
              <#if resourceReport.isDisplayableLine(lineIndex)>
                <tr id="${resourceReport_index?c}L${lineIndex?c}" class="row">
                  <td class="lid ">${lineIndex?c}</td>
                  <td class="line ">
                    <pre>${line}</pre>
                  </td>
                </tr>
                <tr id="${resourceReport_index}S${lineIndex?c}" class="blockSep">
                  <td colspan="2"></td>
                </tr>
                <#assign issues=resourceReport.getIssuesAtLine(lineIndex)>
                <#if issues?has_content>
                  <tr id="${resourceReport_index?c}LV${lineIndex?c}" class="row">
                    <td class="lid"></td>
                    <td class="issues">
                      <#list issues as issue>
                        <div class="issue" id="${report.issueId(issue)?html}">
                          <div class="vtitle">
                            <i class="icon-severity-${issue.getSeverity()?lower_case}"></i>
                            <#if issue.getMessage()?has_content>
                            <span class="rulename">${issue.getMessage()?html}</span>
                            <#else>
                            <span class="rulename">${issue.getRuleName()}</span>
                            </#if>
                            &nbsp;
                            <img src="sonarlintreport_files/sep12.png">&nbsp;
                            <span class="rule_key">${issue.getRuleKey()}</span>
                          </div>
                          <div class="discussionComment">
                            ${issue.getRuleName()}
                          </div>
                        </div>
                        <#assign issueId = issueId + 1>
                      </#list>
                    </td>
                  </tr>
                </#if>
              </#if>
            </#list>
          </table>
        </td>
      </tr>
    </tbody>
  </table>
  </#list>
  </div>
</div>
<script type="text/javascript">
  $(function() {
    refreshFilters(true);
  });
</script>
</#if>
</body>
</html>
