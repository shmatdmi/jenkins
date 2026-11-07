# jenkins
Repository for Jenkins Education

## SAST

Job `sast` loads `sast.groovy` from branch `educations` in this repository and
uses credential `sast-login` (Username with password) for the SAST API.

1. `POST /api/auth/login` sends `username` and `password` and obtains `access_token`.
2. The selected GitHub branch is resolved to a commit. Only its GitHub ZIP
   snapshot is downloaded over HTTPS, streamed directly to a temporary file,
   and uploaded without unpacking to
   `POST /api/scans/archive` with Bearer authorization. The JSON result is saved
   as the build artifact `sast-result.json`.

Before each build, select the source using these parameters:

| Parameter | Default | Purpose |
| --- | --- | --- |
| `REPOSITORY_URL` | `https://github.com/shmatdmi/jenkins.git` | Target Git repository, HTTP(S) or SSH; supply access credentials separately |
| `BRANCH` | `educations` | Exact branch name, including names such as `feature/my-branch` |
| `GITHUB_API_CREDENTIALS_ID` | Empty | Optional Username with password credential for private GitHub archives; password must be a PAT with repository read access |

The pipeline definition always comes from this repository. Target source is
downloaded as a ZIP without cloning, checkout, Git history, or separate Git LFS
requests. The archive is not unpacked on Jenkins and source build scripts are
not executed. The console log
records the selected repository, branch and commit.

The 100 MiB (104,857,600 byte) limit applies to the compressed ZIP. If GitHub
provides Content-Length for the final archive response, the limit is checked
before the body download. Otherwise the console explains that the size is
unknown and curl enforces the limit during transfer. curl 8.4 or newer is
required so this also works without Content-Length. Oversized transfers are
stopped and the partial archive is deleted. Download is limited to 4 minutes.
The SAST API's stricter 25 MiB upload limit is also checked before upload.

Archive download supports `github.com` repository URLs, including SSH URL
notation; the transfer itself always uses HTTPS. Private repositories need a
GitHub PAT rather than an SSH key. GitHub's normal source archives contain LFS
pointer files instead of external LFS objects. Keep the repository's GitHub
archive setting to exclude LFS objects. The pipeline does not fetch LFS objects
separately or turn pointers into large SQL files. SAST applies its own 1 MiB
per-source-file limit while processing the ZIP.

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
