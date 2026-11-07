# jenkins
Repository for Jenkins Education

## SAST

Job `sast` uses `sast.groovy` from branch `educations` and credential
`sast-login` (Username with password).

1. `POST /api/auth/login` sends `username` and `password` and obtains `access_token`.
2. The checked-out Git commit is packaged with `git archive` and uploaded to
   `POST /api/scans/archive` with Bearer authorization. The JSON result is saved
   as the build artifact `sast-result.json`.

`PROJECT_NAME` defaults to `test new image`. `RELEASE` is required and must be
filled in before each build, for example `sast-104`. Its value is sent as the
multipart field `release`; the API accepts 2–4 Latin letters followed by a
hyphen and a number from 1 to 9999. An empty or invalid value stops the build
before authentication or scanning.
Findings are reported without imposing a severity threshold on the build.
Temporary authorization files are deleted after every build.
