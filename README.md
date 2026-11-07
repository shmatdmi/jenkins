# jenkins
Repository for Jenkins Education

## SAST

Job `sast` loads `sast.groovy` from branch `educations` in this repository and
uses credential `sast-login` (Username with password) for the SAST API.

1. `POST /api/auth/login` sends `username` and `password` and obtains `access_token`.
2. The selected repository and branch are checked out in a clean `sast-source`
   directory. That Git commit is packaged with `git archive` and uploaded to
   `POST /api/scans/archive` with Bearer authorization. The JSON result is saved
   as the build artifact `sast-result.json`.

Before each build, select the source using these parameters:

| Parameter | Default | Purpose |
| --- | --- | --- |
| `REPOSITORY_URL` | `https://github.com/shmatdmi/jenkins.git` | Target Git repository, HTTP(S) or SSH; supply access credentials separately |
| `BRANCH` | `educations` | Exact branch name, including names such as `feature/my-branch` |
| `GIT_CREDENTIALS_ID` | Empty | Optional Jenkins credential ID for a private repository; username/password for HTTPS, SSH key for SSH |

The pipeline definition always comes from this repository; the selected source
is packaged for analysis without executing its build scripts. The console log
records the selected repository, branch and commit.

`PROJECT_NAME` defaults to `test new image`. `RELEASE` is required and must be
filled in before each build, for example `sast-104`. Its value is sent as the
multipart field `release`; the API accepts 2–4 Latin letters followed by a
hyphen and a number from 1 to 9999. An empty or invalid value stops the build
before authentication or scanning.
Findings are reported without imposing a severity threshold on the build.
Temporary authorization files are deleted after every build.

## Credentials navigation

`server/init.groovy.d/20-credentials-sidebar.groovy` adds an administrator-only
`Credentials` shortcut to the dashboard sidebar, opening `/manage/credentials/`.
The script is installed in `$JENKINS_HOME/init.groovy.d/` on the controller and
can also be applied through the Script Console without restarting Jenkins.
It preserves the standard credentials permissions and registers one shortcut
when applied repeatedly.
