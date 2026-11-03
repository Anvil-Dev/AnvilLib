"""Regression checks for the Maven index and the main-last publication gate."""

import hashlib
import os
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from unittest.mock import patch
from xml.etree import ElementTree as ET

import publish_maven as ci


class SnapshotTests(unittest.TestCase):
    def test_history_and_older_retries(self):
        old = "2.0.0+snapshot.9"
        new = "2.0.0+snapshot.10"
        xml = ci.metadata(None, "module", old)
        xml = ci.metadata(xml, "module", new)
        xml = ET.fromstring(ci.metadata(xml, "module", old))
        self.assertEqual(xml.findtext("versioning/latest"), new)
        self.assertEqual(xml.findtext("versioning/release"), new)
        self.assertEqual([v.text for v in xml.findall("versioning/versions/version")], [old, new])

    def test_main_is_uploaded_only_after_all_children_and_indexes(self):
        class Response:
            def __init__(self, code):
                self.status = code
            def read(self):
                return b""

        paths = []
        class Connection:
            def __init__(self, *args, **kwargs):
                pass
            def request(self, method, path, body=None, headers=None):
                paths.append((method, path, body))
                self.status = 404 if method == "GET" else 201
                if fail and path.endswith("/child/maven-metadata.xml.sha512"):
                    self.status = 403
            def getresponse(self):
                return Response(self.status)
            def close(self):
                pass

        with TemporaryDirectory() as tmp:
            stage = Path(tmp)
            def entry(name):
                jar = stage / "dev/anvilcraft/lib" / name / "2.0.0+snapshot.10" / "item.jar"
                jar.parent.mkdir(parents=True)
                jar.write_bytes(name.encode())
                return name, [jar]
            items = [entry("child"), entry("main")]
            env = dict(MAVEN_URL="http://repo.test/releases", MAVEN_USERNAME="user", MAVEN_PASSWORD="pass")
            with patch.dict(os.environ, env), patch.object(ci, "HTTPConnection", Connection), \
                    patch.object(ci, "publications", return_value=items):
                fail = True
                with self.assertRaisesRegex(RuntimeError, "403"):
                    ci.publish(stage, "main", "2.0.0+snapshot.10", [])
                self.assertFalse(any("/main/" in path for _, path, _ in paths))
                fail = False
                paths.clear()
                ci.publish(stage, "main", "2.0.0+snapshot.10", [])
                first_main = next(n for n, (_, path, _) in enumerate(paths) if "/main/" in path)
                self.assertTrue(all("/child/" in path for _, path, _ in paths[:first_main]))
                self.assertTrue(all("/main/" in path for _, path, _ in paths[first_main:]))
                puts = {path: body for method, path, body in paths if method == "PUT"}
                index = "/releases/dev/anvilcraft/lib/main/maven-metadata.xml"
                self.assertEqual(puts[index + ".sha256"], hashlib.sha256(puts[index]).hexdigest().encode())
                self.assertIn("/releases/dev/anvilcraft/lib/main/2.0.0%2Bsnapshot.10/item.jar", puts)


if __name__ == "__main__":
    unittest.main()
