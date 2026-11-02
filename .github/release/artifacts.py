"""Check the local publications, including the actual nested NeoForge modules."""

import hashlib
import io
import json
import shutil
import subprocess
import sys
import tomllib
import xml.etree.ElementTree as ET
import zipfile
from pathlib import Path

from common import digest, mod_version, require, staging_digest, summary

GROUP = "dev.anvilcraft.lib"


def mod_id(source, directory):
    properties = (source / directory / "gradle.properties").read_text().splitlines()
    return next(line.split("=", 1)[1].strip() for line in properties if line.startswith("mod_id="))


def verify_mod(jar, expected_id, version):
    with zipfile.ZipFile(jar) as archive:
        metadata = tomllib.loads(archive.read("META-INF/neoforge.mods.toml").decode())
        require(any(m["modId"] == expected_id and m["version"] == version for m in metadata["mods"]),
                f"Invalid mod descriptor for {expected_id} {version}")


def collect(source, line, sha, version, dest):
    actual_sha = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=source, text=True).strip()
    require(actual_sha == sha, f"Expected {sha} for {line}, found {actual_sha}")
    require(mod_version((source / "gradle.properties").read_bytes()) == version, "Version drift")
    game = tomllib.loads((source / "gradle/libs.versions.toml").read_text())["versions"]["minecraft"]
    modules = json.loads((source / ".github/modules.json").read_text())
    main = f"anvillib-neoforge-{line}"
    processor = f"anvillib-sync-processor-neoforge-{line}"
    ids = {f"anvillib-{m['module']}-neoforge-{line}": mod_id(source, "module." + m["module"])
           for m in modules}
    ids[processor] = mod_id(source, "module.sync/processor")
    expected = set(ids) | {main}
    group_dir = source / "build/release/staging/dev/anvilcraft/lib"
    require(not any(group_dir.rglob("*.asc")), "Found a signature from an earlier deployment")
    require({p.name for p in group_dir.iterdir()} == expected, "Incomplete Maven publication set")
    pom_ns = {"m": "http://maven.apache.org/POM/4.0.0"}
    module_deps = {f"anvillib-{m['module']}-neoforge-{line}":
                   {f"anvillib-{n}-neoforge-{line}" for n in m.get("needs", [])} for m in modules}
    module_deps[f"anvillib-sync-neoforge-{line}"].add(processor)
    module_deps[main] = set(ids) - {processor}
    for name in sorted(expected):
        prefix = group_dir / name / version / f"{name}-{version}"
        for suffix in (".jar", "-sources.jar", "-javadoc.jar", "-slim.jar", "-slim-metadata.json"):
            require(Path(str(prefix) + suffix).is_file(), f"Missing {name}{suffix}")
        pom = ET.parse(str(prefix) + ".pom").getroot()
        coords = [pom.findtext("m:" + key, namespaces=pom_ns) for key in ("groupId", "artifactId", "version")]
        require(coords == [GROUP, name, version], f"Invalid Maven coordinates: {coords}")
        internal = set()
        for dep in pom.findall("m:dependencies/m:dependency", pom_ns):
            if dep.findtext("m:groupId", namespaces=pom_ns) == GROUP:
                dep_name = dep.findtext("m:artifactId", namespaces=pom_ns)
                require(dep_name in expected and dep.findtext("m:version", namespaces=pom_ns) == version
                        and dep.findtext("m:scope", "compile", pom_ns) == "compile",
                        f"Incorrect internal dependency {name} -> {dep_name}")
                internal.add(dep_name)
        require(module_deps.get(name, set()) <= internal, f"Incomplete API dependencies for {name}")

    dest.mkdir(parents=True, exist_ok=True)
    prefix = f"anvillib-neoforge-{game}-{version}"
    jar_path = source / "module.main/build/libs" / (prefix + ".jar")
    verify_mod(jar_path, "anvillib", version)
    with zipfile.ZipFile(jar_path) as archive:
        metadata = json.loads(archive.read("META-INF/jarjar/metadata.json"))
        bundled = set()
        for nested in metadata["jars"]:
            identity = nested["identifier"]
            if identity["group"] != GROUP:
                continue
            name = identity["artifact"]
            require(name in ids and name not in bundled and nested["version"]["artifactVersion"] == version,
                    f"Unexpected embedded module {name}")
            data = archive.read(nested["path"])
            slim = group_dir / name / version / f"{name}-{version}-slim.jar"
            require(hashlib.sha256(data).hexdigest() == digest(slim), f"Stale embedded module {name}")
            if name != processor:
                verify_mod(io.BytesIO(data), ids[name], version)
            bundled.add(name)
        require(bundled == set(ids), f"Missing embedded modules: {set(ids) - bundled}")

    hashes = {}
    for suffix in (".jar", "-sources.jar", "-javadoc.jar"):
        filename = prefix + suffix
        original = jar_path.parent / filename
        central = group_dir / main / version / (f"{main}-{version}" + suffix)
        hashes[filename] = digest(original)
        require(hashes[filename] == digest(central), f"Maven and mod-site files differ: {filename}")
        shutil.copyfile(original, dest / filename)
    properties = dict(line.split("=", 1) for line in (source / "gradle.properties").read_text().splitlines()
                      if "=" in line and not line.startswith("#"))
    entry = {"sha": sha, "game": game, "jar": jar_path.name, "files": hashes,
             "staging_sha256": staging_digest(source / "build/release/staging"),
             "modrinth": properties["modrinth_project_id"], "curseforge": properties["curseforge_project_id"]}
    (dest / "entry.json").write_text(json.dumps(entry, indent=2) + "\n")
    summary(f"MC {game}: verified {len(expected)} publications and {len(bundled)} embedded modules.")


if __name__ == "__main__":
    collect(Path(sys.argv[1]), *sys.argv[2:5], Path(sys.argv[5]))
