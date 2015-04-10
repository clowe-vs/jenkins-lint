import groovy.xml.MarkupBuilder
import hudson.model.*
import hudson.triggers.*
import groovy.transform.ToString

@ToString(includeNames=true)
class Rule {
    String id
    String description
    String severity
    ArrayList jobList = new ArrayList()
    def addJob(String jobName) {
      jobList.add(jobName)
    }
}

HIGH    = "High"
MEDIUM  = "Medium"
LOW     = "Low"
IGNORED = "Ignored"

RULE_JL001 = "[JL-001]"
RULE_JL002 = "[JL-002]"
RULE_JL003 = "[JL-003]"
RULE_JL004 = "[JL-004]"
RULE_JL005 = "[JL-005]"
RULE_JL006 = "[JL-006]"
RULE_JL007 = "[JL-007]"
RULE_JL008 = "[JL-008]"
RULE_JL009 = "[JL-009]"
RULE_JL010 = "[JL-010]"
RULE_JL011 = "[JL-011]"
RULE_JL012 = "[JL-012]"
RULE_JL013 = "[JL-013]"
RULE_JL014 = "[JL-014]"

def rulesMap = [:]
rulesMap.put(RULE_JL001, new Rule(id: RULE_JL001, description: "Job name", severity: HIGH))
rulesMap.put(RULE_JL002, new Rule(id: RULE_JL002, description: "Log Rotator doesn't exist", severity: HIGH))
rulesMap.put(RULE_JL003, new Rule(id: RULE_JL003, description: "Description hasn't been set", severity: HIGH))
rulesMap.put(RULE_JL004, new Rule(id: RULE_JL004, description: "SCM hasn't been set", severity: HIGH))
rulesMap.put(RULE_JL005, new Rule(id: RULE_JL005, description: "SCM trigger is polling rather than pulling", severity: HIGH))
rulesMap.put(RULE_JL006, new Rule(id: RULE_JL006, description: "SCM trigger is duplicated", severity: HIGH))
rulesMap.put(RULE_JL007, new Rule(id: RULE_JL007, description: "Restric Label executions", severity: HIGH))
rulesMap.put(RULE_JL008, new Rule(id: RULE_JL008, description: "CleanUp Workspace", severity: HIGH))
rulesMap.put(RULE_JL009, new Rule(id: RULE_JL009, description: "Javadoc", severity: HIGH))
rulesMap.put(RULE_JL010, new Rule(id: RULE_JL010, description: "Artifact", severity: HIGH))
rulesMap.put(RULE_JL011, new Rule(id: RULE_JL011, description: "Harcoded Script", severity: HIGH))
rulesMap.put(RULE_JL012, new Rule(id: RULE_JL012, description: "Maven Job Type", severity: HIGH))
rulesMap.put(RULE_JL013, new Rule(id: RULE_JL013, description: "Git Shallow", severity: HIGH))
rulesMap.put(RULE_JL014, new Rule(id: RULE_JL014, description: "Multijob", severity: HIGH))


/**
 * Print rule with provided rule and jobName.
 *
 * @param ruleClass Rule class.
 * @param jobName Name of the Job.
 */
def printRule(ruleClass, jobName) {
   //println "\t$ruleClass.id|$ruleClass.description|$ruleClass.severity|$jobName"
}

/**
 * Return if ruleId has been ignored in the job description.
 *
 * @param ruleId Rule id.
 * @param jobDescription Job description.
 * @return Boolean whether that ruleId has been ignored or not.
 */
def isIgnored(ruleId, jobDescription){
  if (ruleId != null && jobDescription != null) {
    if (jobDescription.indexOf("lint:ignored:$ruleId") > -1) {
      return true
    }
    return false
  }
  return false
}

/**
 * Return if ruleId has been ignored in the job description.
 *
 * @param ruleClass Rule instance.
 * @param itemClass Job instance.
 * @return Boolean whether that ruleId has been ignored or not.
 */
def runRule(ruleClass, itemClass) {
  if (itemClass != null && ruleClass != null) {
    if (!isIgnored(ruleClass.id,itemClass.description)){
      ruleClass.addJob(itemClass.name)
      printRule (ruleClass, itemClass.name)
    }
  }
}

// list of jobs
jobs = Hudson.instance?.items

jobs?.findAll{ !it.disabled && it.name.contains(" ") }.each {
  runRule (rulesMap[RULE_JL001], it)
}

jobs?.findAll{ !it.logRotator && !it.disabled }.each {
  runRule (rulesMap[RULE_JL002], it)
}

jobs?.findAll{ !it.description && !it.disabled }.each {
  runRule (rulesMap[RULE_JL003], it)
}

jobs?.findAll{ it.scm instanceof hudson.scm.NullSCM && !it.disabled }.each {
  runRule (rulesMap[RULE_JL004], it)
}

TriggerDescriptor SCM_TRIGGER_DESCRIPTOR = Hudson.instance.getDescriptorOrDie(SCMTrigger.class)
jobs?.findAll{ it.triggers.get(SCM_TRIGGER_DESCRIPTOR) && it.triggers.get(SCM_TRIGGER_DESCRIPTOR) instanceof SCMTrigger && !it.disabled }.each {
    runRule (rulesMap[RULE_JL005], it)
}

TriggerDescriptor SCM_TIMER_TRIGGER_DESCRIPTOR = Hudson.instance.getDescriptorOrDie(TimerTrigger.class)
jobs?.findAll{ it.triggers.get(SCM_TIMER_TRIGGER_DESCRIPTOR) && it.triggers.get(SCM_TIMER_TRIGGER_DESCRIPTOR) instanceof TimerTrigger && 
               it.triggers.get(SCM_TRIGGER_DESCRIPTOR) && it.triggers.get(SCM_TRIGGER_DESCRIPTOR) instanceof SCMTrigger && 
               !it.disabled }.each {
    runRule (rulesMap[RULE_JL006], it)
}

jobs?.findAll{ !it.disabled && !it.getAssignedLabelString() }.each {
    runRule (rulesMap[RULE_JL007], it)
}

// PUBLISHERS
jobs?.findAll{ !it.disabled }.each {
  def hasCleanup = false
  def hasJavadoc = true
  def hasArtifact = true

  if ( ! it.publishersList ) {
      hasCleanup = false
  }
  else {
      for(p in it.getPublishersList()) {
        if(p instanceof hudson.plugins.ws_cleanup.WsCleanup) {
            hasCleanup = true
        }
        if(p instanceof hudson.tasks.JavadocArchiver && !p.javadocDir) {
            hasJavadoc = false
        }
        if(p instanceof hudson.tasks.ArtifactArchiver && !p.artifacts) {
            hasArtifact = false
        }
      }
  }
  if (!hasCleanup)  { runRule (rulesMap[RULE_JL008], it) }
  if (!hasJavadoc)  { runRule (rulesMap[RULE_JL009], it) }
  if (!hasArtifact) { runRule (rulesMap[RULE_JL010], it) }
}

// BUILDERS
jobs?.findAll{ !it.disabled && !it instanceof hudson.maven.MavenModuleSet && it.builders}.each {
  for(p in it.builders) {
    if (( p instanceof hudson.tasks.Shell || p instanceof hudson.tasks.BatchFile ) &&
          p.getContents().split("\r\n|\r|\n").length > 3) {
      runRule (rulesMap[RULE_JL011], it)
    }
  }
}

jobs?.findAll{ !it.disabled && it instanceof hudson.maven.MavenModuleSet}.each {
  runRule (rulesMap[RULE_JL012], it)
}

// GIT SCM
jobs?.findAll{ it.scm && it.scm instanceof hudson.plugins.git.GitSCM &&
               it.scm.getExtensions() && !it.disabled }.each {
  for (p in it.scm.getExtensions()) {
    if (p instanceof hudson.plugins.git.extensions.impl.CloneOption && !p.isShallow()){
      runRule (rulesMap[RULE_JL013], it)
    }
  }
}

jobs?.findAll{ !it.disabled && it instanceof com.tikal.jenkins.plugins.multijob.MultiJobProject &&
              (!it.getAssignedLabelString() || !it.getAssignedLabelString().contains("multijob")) }.each {
  runRule (rulesMap[RULE_JL014], it)
}

// HTML formatting
def sb = new StringWriter()
def html = new MarkupBuilder(sb)
html.doubleQuotes = true
html.expandEmptyElements = true
html.omitEmptyAttributes = false
html.omitNullAttributes = false
html.table(class:"stats-table") {
  tr {
    td(id:"stats-header-ruleid", "Job")
    td(id:"stats-header-ruleid-total", "Total")
  }
  rulesMap.each{item->
    tr {
      td(style:"background-color: #C5D88A", item.value.id)
      td(style:"background-color: #C5D88A", item.value.jobList.size())
    }
  }
}

new File(build.getEnvironment(listener).get('WORKSPACE') + "/build/generated.html").withWriter {it.println sb.toString()}
