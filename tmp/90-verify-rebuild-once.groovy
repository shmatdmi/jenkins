import groovy.json.JsonOutput
import hudson.model.Job
import hudson.security.ACL
import jenkins.model.Jenkins

def jenkins = Jenkins.get()
def report = [:]
def authContext = ACL.as2(ACL.SYSTEM2)
try {
    def plugin = jenkins.pluginManager.getPlugin('rebuild')
    report.plugin = [version: plugin?.version, active: plugin?.isActive()]
    report.queueSize = jenkins.queue.items.length
    report.runs = jenkins.getAllItems(Job.class).collect { job ->
        def run = job.lastCompletedBuild
        if (run == null) {
            return null
        }
        def action = run.allActions.find { it.urlName == 'rebuild' }
        return [job: job.fullName, build: run.number, url: run.url,
                displayName: action?.displayName, icon: action?.iconFileName,
                taskUrl: action?.taskUrl]
    }.findAll { it != null }
} catch (Throwable error) {
    report.error = error.class.name + ': ' + error.message
} finally {
    authContext.close()
}
def result = new File(jenkins.rootDir, 'rebuild-install-verification.json')
result.text = JsonOutput.prettyPrint(JsonOutput.toJson(report))
println('REBUILD_INSTALL_VERIFICATION ' + result.absolutePath)
new File(jenkins.rootDir, 'init.groovy.d/90-verify-rebuild-once.groovy').delete()
