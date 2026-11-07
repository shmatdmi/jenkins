# jenkins
Repository for Jenkins Education

## SAST

Job `sast` uses `sast.groovy` from branch `educations` and credential
`sast-login` (Username with password).

1. `POST /api/auth/login` sends `username` and `password` and obtains `access_token`.
2. The checked-out Git commit is packaged with `git archive` and uploaded to
   `POST /api/scans/archive` with Bearer authorization. The JSON result is saved
   as the build artifact `sast-result.json`.

`PROJECT_NAME` defaults to `jenkins`. An empty `RELEASE` uses `SAST-<build number>`;
the API accepts 2–4 Latin letters followed by a hyphen and a number from 1 to 9999.
Findings are reported without imposing a severity threshold on the build.
Temporary authorization files are deleted after every build.
