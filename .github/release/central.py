"""Upload one signed bundle with JReleaser and wait for the Portal's publication."""

import base64
import json
import os
import re
import subprocess
import sys
import time
import uuid
from pathlib import Path
from urllib.request import Request, urlopen

from common import commit_status, record, require, staging_digest, summary, tag_manifest

PORTAL = "https://central.sonatype.com/api/v1/publisher/"


def portal(path):
    credentials = os.environ["JRELEASER_MAVENCENTRAL_USERNAME"] + ":" + os.environ["JRELEASER_MAVENCENTRAL_PASSWORD"]
    token = base64.b64encode(credentials.encode()).decode()
    req = Request(PORTAL + path, method="POST", headers={"Authorization": "Bearer " + token})
    with urlopen(req, timeout=60) as result:
        return result.read()


def deploy(tag, line, source, staging):
    target = tag_manifest(tag)["targets"][line]
    sha = target["sha"]
    context = f"release/{tag}/{line}/central"
    previous = commit_status(sha, context)
    if previous and previous["state"] == "success":
        summary(f"Maven Central {line}: {previous['description']}")
        return
    deployment_id = None
    if previous and previous["description"].startswith("deployment "):
        deployment_id = str(uuid.UUID(previous["description"].split()[1]))
    else:
        require(not previous or previous["state"] != "pending",
                f"Previous upload has no receipt. Reconcile {context} in the Portal before retrying.")
        require(staging_digest(staging) == target["staging_sha256"], "Maven staging checksum mismatch")
        for key in ("JRELEASER_MAVENCENTRAL_USERNAME", "JRELEASER_MAVENCENTRAL_PASSWORD",
                    "JRELEASER_GPG_SECRET_KEY", "JRELEASER_GPG_PUBLIC_KEY", "JRELEASER_GPG_PASSPHRASE"):
            require(os.environ.get(key), f"Missing {key}")
        record(sha, context, "pending", "upload in progress")
        submitted = False
        properties = Path(source) / "module.main/build/jreleaser/output.properties"
        fresh_output = not properties.exists()
        # JReleaser 1.23 logs the receipt immediately, before waiting for validation.
        # Save it here so a cancelled Gradle process can be resumed by deployment ID.
        with subprocess.Popen(["bash", "gradlew", f":anvillib-neoforge-{line}:jreleaserDeploy",
                               f"-PanvillibCentralStaging={Path(staging).resolve()}",
                               "--git-root-search", "--no-configuration-cache", "--console=plain"], cwd=source,
                              env=dict(os.environ, JRELEASER_MAVENCENTRAL_STAGE="UPLOAD"),
                              stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True) as gradle:
            for message in gradle.stdout:
                print(message, end="", flush=True)
                if re.search(r"Uploading \S+-bundle\.zip", message):
                    submitted = True
                receipt = re.search(r"uploaded as deployment ([0-9a-fA-F-]{36})", message)
                if receipt:
                    deployment_id = str(uuid.UUID(receipt[1]))
                    record(sha, context, "pending", "deployment " + deployment_id)
            code = gradle.wait()
        if not deployment_id and fresh_output and properties.is_file():
            receipt = re.search(r"(?im)^deployMavenCentralSonatypeDeploymentId\s*=\s*([0-9a-f-]{36})\s*$",
                                properties.read_text())
            if receipt:
                deployment_id = str(uuid.UUID(receipt[1]))
                record(sha, context, "pending", "deployment " + deployment_id)
        if not deployment_id:
            if not submitted and code:
                record(sha, context, "failure", "bundle preparation failed")
            raise RuntimeError(f"JReleaser exited {code} without a deployment ID; inspect its upload log.")

    publish_requested = False
    for _ in range(180):
        response = json.loads(portal("status?id=" + deployment_id))
        state = response["deploymentState"]
        if state == "PUBLISHED":
            record(sha, context, "success", "deployment " + deployment_id)
            summary(f"Maven Central {line}: PUBLISHED ({deployment_id})")
            return
        if state == "FAILED":
            record(sha, context, "failure", "rejected " + deployment_id)
            raise RuntimeError(f"Deployment {deployment_id} rejected: {response.get('errors')}")
        if state == "VALIDATED" and not publish_requested:
            portal("deployment/" + deployment_id)
            publish_requested = True
        require(state in {"PENDING", "VALIDATING", "VALIDATED", "PUBLISHING"}, f"Unexpected state {state}")
        time.sleep(20)
    raise TimeoutError(f"Deployment {deployment_id} is still {state}. Re-run this job to resume.")


if __name__ == "__main__":
    deploy(*sys.argv[1:])
