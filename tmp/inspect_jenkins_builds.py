import json
import re
from pathlib import Path
import xml.etree.ElementTree as ET

home = Path('/opt/docker/volumes/jenkins')
unfinished = []
latest = []
checked = 0
for job_config in sorted((home / 'jobs').rglob('config.xml')):
    job_dir = job_config.parent
    builds = job_dir / 'builds'
    if not builds.is_dir():
        continue
    name = '/'.join(part for part in job_dir.relative_to(home / 'jobs').parts if part != 'jobs')
    completed = []
    for build_dir in builds.iterdir():
        if not build_dir.is_dir() or not build_dir.name.isdigit():
            continue
        build_xml = build_dir / 'build.xml'
        if not build_xml.is_file():
            continue
        checked += 1
        metadata = build_xml.read_text(encoding='utf-8')
        match = re.search(r'<result>([A-Z_]+)</result>', metadata)
        result = match.group(1) if match else None
        record = {'job': name, 'build': int(build_dir.name), 'result': result}
        if result is None or '<building>true</building>' in metadata:
            unfinished.append(record)
        else:
            completed.append(record)
    if completed:
        latest.append(max(completed, key=lambda entry: entry['build']))
queue = home / 'queue.xml'
queue_items = []
if queue.is_file():
    queue_root = ET.parse(queue).getroot()
    queue_items = [item.findtext('task') for item in queue_root.findall('.//items/*')]
print(json.dumps({'builds_checked': checked, 'unfinished': unfinished,
                  'persisted_queue_items': queue_items, 'latest_completed': latest}, ensure_ascii=False, indent=2))
if unfinished or queue_items:
    raise SystemExit('Jenkins is not confirmed idle; do not restart')
