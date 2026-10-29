"""Generate the small, public update manifest consumed by Ciel."""
import hashlib
import json
import pathlib
import sys

apk, code, version, output = sys.argv[1:]
code = int(code)
if code < 1:
    raise ValueError('versionCode must be positive')
digest = hashlib.file_digest(pathlib.Path(apk).open('rb'), 'sha256').hexdigest()
pathlib.Path(output).write_text(json.dumps({
    'versionCode': code,
    'versionName': version,
    'sha256': digest,
}, indent=2) + '\n', encoding='utf-8')
