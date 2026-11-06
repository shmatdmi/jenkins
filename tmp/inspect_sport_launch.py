import crypt
import json
from pathlib import Path
import shutil
import xml.etree.ElementTree as ET

home = Path('/opt/docker/volumes/jenkins')
job = ET.parse(home / 'jobs/sport/config.xml').getroot()
parameters = []
definitions = job.find('./properties/hudson.model.ParametersDefinitionProperty/parameterDefinitions')
if definitions is not None:
    for definition in definitions:
        name = definition.findtext('name')
        kind = definition.tag
        if 'Password' in kind:
            default = '[hidden]'
        elif 'ChoiceParameterDefinition' in kind:
            default = definition.findtext('./choices/a/string')
        else:
            default = definition.findtext('defaultValue')
        parameters.append({'name': name, 'type': kind, 'default': default})
initial_password = home / 'secrets/initialAdminPassword'
matching_login = False
if initial_password.is_file():
    password = initial_password.read_text().strip()
    for user_config in (home / 'users').glob('*/config.xml'):
        user = ET.parse(user_config).getroot()
        password_hash = user.findtext('.//passwordHash') or ''
        if password_hash.startswith('#jbcrypt:'):
            hashed = password_hash[len('#jbcrypt:'):]
            if crypt.crypt(password, hashed) == hashed:
                matching_login = True
print(json.dumps({'disabled': job.findtext('disabled'), 'parameters': parameters,
                  'saved_initial_credential_valid': matching_login}, ensure_ascii=False, indent=2))
