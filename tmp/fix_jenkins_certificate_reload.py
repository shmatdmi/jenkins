from datetime import datetime, timezone
from pathlib import Path
import shutil
import subprocess


directory = Path('/opt/dcomp/jenkins')
config = directory / 'docker-compose.yaml'
original = config.read_bytes()
text = original.decode('utf-8')
anchor = '    container_name: jenkins-nginx\n'
addition = '''    command:
      - /bin/sh
      - -c
      - |
        (while :; do sleep 1h; nginx -t && nginx -s reload; done) &
        exec nginx -g 'daemon off;'
'''
if text.count(anchor) != 1:
    raise SystemExit('Expected exactly one jenkins-nginx service; no changes made')
nginx_service = text.split('  nginx:\n', 1)[1].split('\nnetworks:', 1)[0]
if '    command:' in nginx_service:
    raise SystemExit('The nginx service already has a command; no changes made')
updated = text.replace(anchor, anchor + addition, 1)
candidate = directory / 'docker-compose.certificate-reload.candidate.yaml'
if candidate.exists():
    raise SystemExit('Candidate file already exists; no changes made')
candidate.write_bytes(updated.encode('utf-8'))
candidate.chmod(config.stat().st_mode & 0o777)

compose = ['docker', 'compose']
if subprocess.run(compose + ['version'], capture_output=True).returncode != 0:
    compose = ['docker-compose']
subprocess.run(compose + ['-f', str(candidate), 'config', '--quiet'], check=True)
stamp = datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ')
backup = config.with_name(config.name + '.bak-certificate-reload-' + stamp)
shutil.copy2(config, backup)
candidate.replace(config)
try:
    subprocess.run(compose + ['-f', str(config), 'up', '-d', '--no-deps', 'nginx'], check=True)
except subprocess.CalledProcessError:
    shutil.copy2(backup, config)
    subprocess.run(compose + ['-f', str(config), 'up', '-d', '--no-deps', 'nginx'], check=True)
    raise
print('Updated:', config)
print('Backup:', backup)
