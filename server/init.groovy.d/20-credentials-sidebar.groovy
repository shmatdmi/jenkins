import hudson.model.Action
import hudson.model.TransientViewActionFactory
import hudson.model.View
import jenkins.model.Jenkins
import org.jenkins.ui.icon.IconSpec

// The credentials plugin directs administrators through Manage Jenkins.
// Restore a dashboard shortcut using the standard credentials page and ACL.
class CredentialsSidebarShortcut implements Action, IconSpec {
    @Override
    String getDisplayName() {
        return 'Credentials'
    }

    @Override
    String getUrlName() {
        return '/manage/credentials/'
    }

    @Override
    String getIconClassName() {
        return Jenkins.get().hasPermission(Jenkins.ADMINISTER)
            ? 'symbol-credentials plugin-credentials'
            : null
    }

    @Override
    String getIconFileName() {
        return getIconClassName()
    }
}

class CredentialsSidebarShortcutFactory extends TransientViewActionFactory {
    @Override
    List<Action> createFor(View view) {
        return view.owner instanceof Jenkins ? [new CredentialsSidebarShortcut()] : []
    }
}

def jenkins = Jenkins.get()
jenkins.checkPermission(Jenkins.ADMINISTER)
def factories = TransientViewActionFactory.all()
factories.findAll { it.class.name == CredentialsSidebarShortcutFactory.name }.each {
    factories.remove(it)
}
factories.add(new CredentialsSidebarShortcutFactory())
