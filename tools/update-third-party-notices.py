"""Generate bundled notices from the exact release runtime artifacts (no network).

Run ./gradlew :app:exportReleaseLicenseInputs, then python tools/update-third-party-notices.py.
Review generated changes before committing; unknown or missing POM licenses fail closed.
"""
from pathlib import Path
import io
import os
import re
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'app/src/main/assets/third_party_licenses'
CACHE = Path(os.environ.get('GRADLE_USER_HOME', Path.home() / '.gradle')) / 'caches/modules-2/files-2.1'
NS = {'m': 'http://maven.apache.org/POM/4.0.0'}

def pom_licenses(group, name, version):
    poms = list((CACHE / group / name / version).glob('*/*.pom'))
    if not poms:
        raise RuntimeError(f'Missing POM: {group}:{name}:{version}')
    pom = ET.parse(poms[0]).getroot()
    licenses = [(entry.findtext('m:name', '', NS), entry.findtext('m:url', '', NS))
                for entry in pom.findall('m:licenses/m:license', NS)]
    parent = pom.find('m:parent', NS)
    if not licenses and parent is not None:
        return pom_licenses(*(parent.findtext(f'm:{key}', '', NS)
                              for key in ('groupId', 'artifactId', 'version')))
    return licenses

def notices(data, prefix=''):
    result = []
    with zipfile.ZipFile(io.BytesIO(data)) as archive:
        for name in sorted(archive.namelist()):
            if name.endswith('/'):
                continue
            base = name.rsplit('/', 1)[-1]
            if not base.endswith(('.class', '.kotlin_metadata')) and re.search(r'(?i)(license|notice|copying|copyright|^AL2\.0$|^LGPL2\.1$)', base):
                content = archive.read(name).decode('utf-8-sig', errors='strict')
                result.append((prefix + name, content))
            elif name.endswith('.jar'):
                result.extend(notices(archive.read(name), prefix + name + '!/'))
    return result

def main():
    rows = []
    bundled = []
    seen = set()
    for line in (ROOT / 'app/build/license-inputs.tsv').read_text(encoding='utf-8').splitlines():
        group, name, version, filename = line.split('\t')
        coordinate = f'{group}:{name}:{version}'
        licenses = pom_licenses(group, name, version)
        if not licenses:
            raise RuntimeError(f'Missing license declaration: {coordinate}')
        if not all('apache' in (label + url).lower() or label == 'BSD-3-Clause' for label, url in licenses):
            raise RuntimeError(f'Review non-Apache dependency before extending generator: {coordinate}: {licenses}')
        if coordinate not in seen:
            rows.append(f'| `{coordinate}` | ' + ', '.join(f'[{label}]({url})' for label, url in licenses) + ' |')
            seen.add(coordinate)
        for origin, text in notices(Path(filename).read_bytes()):
            bundled.append(f'=== {coordinate} / {Path(filename).name}!/{origin} ===\n{text.rstrip()}\n')
    header = '''# Third-party components / 第三者コンテンツ

Generated from the resolved **releaseRuntimeClasspath**, including transitive
dependencies. Build/test-only tools are not app runtime dependencies. Versions
below are resolved versions, not merely the versions requested in build.gradle.kts.

The table records the licenses declared in each component's published POM.
See [APACHE-2.0.txt](APACHE-2.0.txt) for the complete license and
[DEPENDENCY_NOTICES.txt](DEPENDENCY_NOTICES.txt) for notices retained verbatim
from the distributed AAR/JAR files, including nested JARs. No dependency source
has been modified; Android build tools may transform/shrink compiled code.

These materials are excluded from Tabiline's proprietary license.
The repackaged Protocol Buffers component uses BSD-3-Clause; its full license
is also included in DEPENDENCY_NOTICES.txt.
See PROTOBUF_BSD-3-CLAUSE.txt for the upstream copyright and license.

| Component | Published license |
|---|---|
'''
    extra = '''
## Other bundled materials

- Roboto Flex and Japanese glyphs from Noto Sans JP: SIL OFL-1.1; see
  ROBOTO_FLEX_OFL-1.1.txt and NOTO_SANS_JP_OFL-1.1.txt. The static weight fonts
  are modified/merged derivatives, not unmodified upstream font files.
  These fonts remain under OFL, not Tabiline's proprietary terms.
- Ko-fi logo: official creator asset used to link to the developer's page;
  see KOFI_BRAND_ASSET.txt for source, permitted purpose and modification.
- Gradle Wrapper (repository build tool): its original license is retained
  inside gradle/wrapper/gradle-wrapper.jar at META-INF/LICENSE; launcher
  scripts retain their original copyright/license headers.
- README app icon: derived from Tabiline's own launcher vector. Promotional
  image: actual app captures with fictional data; original code-drawn device
  frames, with no externally sourced device mockup.

## Updating this inventory

Run `./gradlew :app:exportReleaseLicenseInputs`, then
`python tools/update-third-party-notices.py` from the repository root.
Review new component licenses and notices whenever dependencies change.
The generator refuses missing or unrecognized POM licenses rather than
silently claiming coverage. Original upstream notices take precedence over
this index. This inventory describes the current source build; it does not
retroactively alter previously published APKs.
'''
    # Haze's existing file already contains the complete Apache-2.0 text.
    apache = (OUT / 'HAZE_APACHE-2.0.txt').read_text(encoding='utf-8')
    apache = apache[apache.index('Apache License'):]
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / 'APACHE-2.0.txt').write_text(apache, encoding='utf-8')
    (OUT / 'DEPENDENCIES.md').write_text(header + '\n'.join(rows) + '\n' + extra, encoding='utf-8')
    (OUT / 'DEPENDENCY_NOTICES.txt').write_text('\n'.join(bundled), encoding='utf-8')
    print(f'{len(seen)} components; {len(bundled)} original notice entries retained.')

if __name__ == '__main__':
    main()
