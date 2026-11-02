"""Upload the staged Gradle publications to the internal Maven HTTP repository."""

import base64
import hashlib
import json
import os
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timezone
from http.client import HTTPConnection, HTTPException, HTTPSConnection
from pathlib import Path
from time import sleep
from urllib.parse import quote, urlsplit
from xml.etree import ElementTree as ET

GROUP = "dev.anvilcraft.lib"
CHECKSUMS = ("md5", "sha1", "sha256", "sha512")


def metadata(previous, artifact, version):
    root = ET.fromstring(previous) if previous else ET.Element("metadata")
    if root.tag != "metadata" or root.findtext("groupId", GROUP) != GROUP or root.findtext("artifactId", artifact) != artifact:
        raise ValueError(f"Unexpected Maven index for {artifact}")

    def field(parent, name):
        node = parent.find(name)
        return node if node is not None else ET.SubElement(parent, name)

    field(root, "groupId").text = GROUP
    field(root, "artifactId").text = artifact
    info = field(root, "versioning")
    latest = field(info, "latest")
    release = field(info, "release")
    versions = field(info, "versions")
    if version not in [v.text for v in versions.findall("version")]:
        ET.SubElement(versions, "version").text = version
    # A retried older Actions run must not replace a newer snapshot as latest.
    previous_latest = latest.text or release.text or ""
    latest_run = previous_latest.rpartition("+snapshot.")[2] if "+snapshot." in previous_latest else ""
    current_run = version.rpartition("+snapshot.")[2]
    if not latest_run.isdigit() or int(current_run) >= int(latest_run):
        latest.text = release.text = version
    else:
        latest.text = previous_latest
        release.text = release.text or previous_latest
    field(info, "lastUpdated").text = datetime.now(timezone.utc).strftime("%Y%m%d%H%M%S")
    return ET.tostring(root, encoding="utf-8", xml_declaration=True)


def publications(staging, main, version, modules):
    suffix = main.removeprefix("anvillib-")
    names = [f"anvillib-{m}-{suffix}" for m in ["sync-processor", *modules]] + [main]
    group_dir = staging / GROUP.replace(".", "/")
    if {p.name for p in group_dir.iterdir()} != set(names):
        raise ValueError("Incomplete set of staged CI publications")
    result = []
    for name in names:
        folder = group_dir / name / version
        prefix = f"{name}-{version}"
        required = {prefix + s for s in (".pom", ".jar", ".module", "-sources.jar", "-javadoc.jar",
                                          "-slim.jar", "-slim-metadata.json")}
        files = sorted(p for p in folder.iterdir() if p.is_file())
        present = {p.name for p in files}
        if not required <= present:
            raise ValueError(f"Missing publication files for {name}: {required - present}")
        ns = {"m": "http://maven.apache.org/POM/4.0.0"}
        pom = ET.parse(folder / (prefix + ".pom")).getroot()
        if tuple(pom.findtext("m:" + k, namespaces=ns) for k in ("groupId", "artifactId", "version")) != (GROUP, name, version):
            raise ValueError(f"Unexpected coordinates in {name}")
        for dep in pom.findall("m:dependencies/m:dependency", ns):
            if dep.findtext("m:groupId", namespaces=ns) == GROUP and (
                dep.findtext("m:artifactId", namespaces=ns) not in names
                or dep.findtext("m:version", namespaces=ns) != version
                or dep.findtext("m:scope", "compile", ns) != "compile"
            ):
                raise ValueError(f"Invalid internal dependency in {name}")
        result.append((name, files))
    return result


def publish(staging, main, version, modules):
    if "+snapshot." not in version or not version.rpartition("+snapshot.")[2].isdigit():
        raise ValueError(f"Expected a CI snapshot, got {version!r}")
    items = publications(staging, main, version, modules)
    url = urlsplit(os.environ["MAVEN_URL"])
    if url.scheme not in ("https", "http") or not url.hostname or url.query or url.fragment:
        raise ValueError("Expected an HTTP Maven repository URL")
    token = base64.b64encode((os.environ["MAVEN_USERNAME"] + ":" + os.environ["MAVEN_PASSWORD"]).encode()).decode()
    headers = {"Authorization": "Basic " + token, "Content-Type": "application/octet-stream"}

    def upload(item):
        artifact, files = item
        client = (HTTPSConnection if url.scheme == "https" else HTTPConnection)(url.hostname, url.port, timeout=90)

        def request(method, path, data=None):
            for attempt in range(3):
                try:
                    client.request(method, url.path.rstrip("/") + "/" + quote(path, safe="/"), body=data, headers=headers)
                    response = client.getresponse()
                    body = response.read()
                    if method == "GET" and response.status == 404:
                        return None
                    if 200 <= response.status < 300:
                        return body
                    retryable = response.status in (408, 429) or response.status >= 500
                    if not retryable or attempt == 2:
                        raise RuntimeError(f"Maven {method} {path} returned HTTP {response.status}")
                except (OSError, HTTPException):
                    if attempt == 2:
                        raise
                client.close()
                sleep(2 ** attempt)

        try:
            print(f"Publishing {GROUP}:{artifact}:{version}", flush=True)
            index = f"{GROUP.replace('.', '/')}/{artifact}/maven-metadata.xml"
            data = metadata(request("GET", index), artifact, version)
            for path in files:
                request("PUT", path.relative_to(staging).as_posix(), path.read_bytes())
            request("PUT", index, data)
            for algorithm in CHECKSUMS:
                request("PUT", index + "." + algorithm, hashlib.new(algorithm, data).hexdigest().encode())
        finally:
            client.close()

    with ThreadPoolExecutor(max_workers=4) as pool:
        list(pool.map(upload, items[:-1]))
    upload(items[-1])


if __name__ == "__main__":
    build = Path("build/ci")
    outputs = dict(line.split("=", 1) for line in (build / "outputs").read_text().splitlines())
    modules = [m["module"] for m in json.loads(Path(".github/modules.json").read_text())]
    publish(build / "maven", outputs["main_id"], outputs["version"], modules)
