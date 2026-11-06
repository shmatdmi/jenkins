import base64
import json
import ssl
import sys
from pathlib import Path
from urllib.error import HTTPError
from urllib.parse import urlencode
from urllib.request import Request, urlopen
import xml.etree.ElementTree as ET

home = Path('/opt/docker/volumes/jenkins')
job_path = home / 'jobs/sport/config.xml'
job = ET.parse(job_path).getroot()
parameters = {}
definitions = job.find('./properties/hudson.model.ParametersDefinitionProperty/parameterDefinitions')
for definition in definitions or []:
    name = definition.findtext('name')
    kind = definition.tag.rsplit('.', 1)[-1]
    if kind == 'ChoiceParameterDefinition':
        choices = definition.findall('.//choices//string')
        if not choices:
            raise SystemExit('Unable to determine default choice: ' + name)
        default = choices[0].text or ''
    elif kind in ('StringParameterDefinition', 'TextParameterDefinition', 'BooleanParameterDefinition'):
        default = definition.findtext('defaultValue') or ''
    else:
        raise SystemExit('Unsupported parameter type: ' + kind)
    parameters[name] = default
users = []
for path in sorted((home / 'users').glob('*/config.xml')):
    root = ET.parse(path).getroot()
    user_id = root.findtext('id')
    if user_id and root.find('.//jenkins.security.ApiTokenProperty') is not None:
        users.append(user_id)
if '--inspect' in sys.argv:
    print(json.dumps({'users_with_tokens': users, 'parameters': parameters}, ensure_ascii=False))
    raise SystemExit(0)
username = sys.argv[1]
if username not in users:
    raise SystemExit('Unknown token user')
token = sys.stdin.readline().strip()
if not token:
    raise SystemExit('No token supplied')
authorization = base64.b64encode((username + ':' + token).encode()).decode()
headers = {'Authorization': 'Basic ' + authorization}
base = 'https://84.54.57.64:8282'
context = ssl.create_default_context()


def request_json(path):
    request = Request(base + path, headers=headers)
    with urlopen(request, context=context, timeout=30) as response:
        return json.load(response)


identity = request_json('/whoAmI/api/json')
if not identity.get('authenticated') or identity.get('anonymous'):
    raise SystemExit('API authentication failed')
request = Request(base + '/job/sport/buildWithParameters',
                  data=urlencode(parameters).encode(),
                  headers={**headers, 'Content-Type': 'application/x-www-form-urlencoded'},
                  method='POST')
with urlopen(request, context=context, timeout=30) as response:
    location = response.headers.get('Location')
    if response.status != 201 or not location:
        raise SystemExit('Unexpected build trigger response: ' + str(response.status))
    print(json.dumps({'status': response.status, 'queue_url': location,
                      'parameters': parameters}, ensure_ascii=False))
