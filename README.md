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
| `GITHUB_API_CREDENTIALS_ID` | Empty | Optional Username with password credential for GitHub REST API; password must be a PAT with repository read access. For HTTPS this falls back to `GIT_CREDENTIALS_ID` |

The pipeline definition always comes from this repository; the selected source
is packaged for analysis without executing its build scripts. The console log
records the selected repository, branch and commit.

Before fetching the target repository, the pipeline checks GitHub REST metadata.
It refuses to download if either the Git repository size reported by GitHub or
the sum of blob sizes in the selected branch exceeds 100 MiB (104,857,600 bytes).
Both estimates and the refusal reason are printed in Console Output. A rejected
build fails without running target checkout, ZIP upload or SAST analysis. The
exact commit checked by the preflight is the commit fetched for scanning.

The size check currently supports `github.com` URLs over HTTP(S) and SSH. An
unsupported host, API failure, missing branch, unavailable size, or truncated
tree response stops the build without downloading the target code. GitHub tree
sizes describe Git blobs; external LFS objects are excluded separately as below.

Automatic Git LFS downloads are disabled for checkout. LFS-tracked files are
excluded from the scan ZIP rather than submitting their pointer files as source
code. The build logs this exclusion. This avoids downloading the large generated
SQL catalogs in `shmatdmi/sast`; those catalogs also exceed the SAST API's 1 MiB
per-source-file limit. Other repositories with source code stored in LFS will
need that code checked separately.

Repository checkout is limited to 4 minutes overall (3 minutes for Git fetch,
2 minutes for Git checkout). The ZIP size is checked against the API's 25 MiB
upload limit before sending it. Console messages mark checkout, packaging and
upload so that the operation being awaited is visible.

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
