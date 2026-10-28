#!/usr/bin/env python3
"""Scan resolved Maven coordinates against OSV. No account or paid API required.
First run: mvn -B dependency:list -DoutputFile=target/security-dependencies.txt
Exit 1 for findings, 2 for incomplete scan; never treats service errors as clean.
"""
import datetime
import json
import re
import sys
import urllib.request
from pathlib import Path

path = Path('target/security-dependencies.txt')
if not path.exists():
    sys.exit('Missing dependency list; run Maven dependency:list first')
coordinates = []
for line in path.read_text().splitlines():
    match = re.match(r'\s+([^:\s]+):([^:\s]+):(?:jar|pom|test-jar):(?:(?:[^:\s]+):)?([^:\s]+):(compile|runtime|provided|test|system)(?:\s|$)', line)
    if match:
        group, artifact, version, scope = match.groups()
        coordinates.append({'package': {'ecosystem': 'Maven', 'name': f'{group}:{artifact}'}, 'version': version})
if not coordinates:
    sys.exit('No coordinates parsed; scan is incomplete')
try:
    request = urllib.request.Request('https://api.osv.dev/v1/querybatch',
        data=json.dumps({'queries': coordinates}).encode(), headers={'Content-Type': 'application/json'})
    with urllib.request.urlopen(request, timeout=120) as response:
        results = json.load(response)['results']
    if len(results) != len(coordinates):
        raise ValueError('Incomplete OSV response')
    findings = [{'package': q['package']['name'], 'version': q['version'], 'vulnerabilities': result['vulns']}
                for q, result in zip(coordinates, results) if result.get('vulns')]
    reviews = json.loads(Path('security/advisory-reviews.json').read_text())['reviews']
    sources = '\n'.join(p.read_text() for p in Path('src/main/java').rglob('*.java'))
    actionable = 0
    for finding in findings:
        for vulnerability in finding['vulnerabilities']:
            matched = next((review for review in reviews if review['id'] == vulnerability['id']
                and review['package'] == finding['package'] and review['version'] == finding['version']
                and review['status'] == 'not_affected'
                and datetime.date.fromisoformat(review['review_until']) >= datetime.date.today()
                and not any(pattern in sources for pattern in review['forbidden_source_patterns'])), None)
            if matched:
                vulnerability['review'] = matched
            else:
                actionable += 1
    report = {'database': 'OSV', 'scanned': len(coordinates), 'actionable_advisories': actionable, 'findings': findings}
    Path('target/osv-report.json').write_text(json.dumps(report, indent=2)+'\n')
    print(f"OSV scanned {len(coordinates)} dependencies; {len(findings)} packages have advisories. See target/osv-report.json")
    for finding in findings:
        print(f"{finding['package']}:{finding['version']} " + ', '.join(v['id'] for v in finding['vulnerabilities']))
    print(f"Unreviewed/actionable advisories: {actionable}; reviewed non-applicable findings remain in the report.")
    sys.exit(1 if actionable else 0)
except Exception as error:
    print(f'OSV scan incomplete: {error}', file=sys.stderr)
    sys.exit(2)
