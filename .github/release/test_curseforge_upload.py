"""CurseForge upload rejection and ambiguous response regressions."""

import json
import os
import subprocess
import unittest
from pathlib import Path
from unittest.mock import patch

import mod_sites


@patch.dict(os.environ, {"CURSEFORGE_TOKEN": "test", "GITHUB_REPOSITORY": "example/AnvilLib"})
class CurseForgeUploadTests(unittest.TestCase):
    def setUp(self):
        self.ledger = {}
        self.target = {"sha": "sha", "game": "26.3", "curseforge": "123", "jar": "x.jar"}
        self.plan = {"tag": "2.0.0", "version": "2.0.0", "targets": {"26.3": self.target}}
        self.context = "release/2.0.0/26.3/curseforge"
        self.enterContext(patch.object(mod_sites, "commit_status", side_effect=lambda _, key: self.ledger.get(key)))
        self.enterContext(patch.object(mod_sites, "record", side_effect=self.record))
        self.sleep = self.enterContext(patch.object(mod_sites.time, "sleep"))

    def record(self, _sha, context, state, description):
        self.ledger[context] = {"state": state, "description": description}

    def publish(self):
        return mod_sites.publish_curseforge(self.plan, "26.3", Path("."), self.context)

    def test_parent_not_ready_retries_only_the_rejected_attachment(self):
        rejected = mod_sites.Rejected(400, json.dumps({"errorCode": 1012}))
        with patch.object(mod_sites, "multipart", side_effect=[
            {"id": 1}, {"id": 2}, rejected, rejected, {"id": 3},
        ]) as upload:
            self.assertEqual(self.publish(), "1, 2, 3")
            self.assertEqual(self.publish(), "1, 2, 3")
            names = [call.args[2]["file"].name for call in upload.call_args_list]
            self.assertEqual(names, ["x.jar", "x-sources.jar", *["x-javadoc.jar"] * 3])
            self.assertEqual(self.sleep.call_count, 2)
            self.assertTrue(all(call.args[2]["metadata"]["parentFileID"] == 1
                                for call in upload.call_args_list[1:]))

    def test_parent_rejections_are_bounded(self):
        rejected = mod_sites.Rejected(400, '{"errorCode":1012}')
        with patch.object(mod_sites, "multipart", side_effect=[{"id": 1}, *[rejected] * 12]) as upload:
            with self.assertRaises(mod_sites.Rejected):
                self.publish()
            self.assertEqual(upload.call_count, 13)
            self.assertEqual(self.sleep.call_count, 11)
            self.assertEqual(self.ledger[self.context + "/sources"]["state"], "failure")

    def test_500_and_connection_errors_leave_receipt_uncertain_and_are_not_retried(self):
        for error in (ValueError("Upload result uncertain (HTTP 500)"), OSError("connection lost")):
            with self.subTest(error=error):
                self.ledger.clear()
                with patch.object(mod_sites, "multipart", side_effect=[{"id": 1}, {"id": 2}, error]) as upload:
                    with self.assertRaises(type(error)):
                        self.publish()
                    with self.assertRaisesRegex(ValueError, "no retry"):
                        self.publish()
                    self.assertEqual(upload.call_count, 3)
                    self.assertEqual(self.ledger[self.context + "/javadoc"]["state"], "pending")
        self.sleep.assert_not_called()

    def test_unrelated_400_does_not_enter_parent_retry_loop(self):
        with patch.object(mod_sites, "multipart", side_effect=[
            {"id": 1}, mod_sites.Rejected(400, '{"errorCode":1001}'),
        ]) as upload:
            with self.assertRaises(mod_sites.Rejected):
                self.publish()
            self.assertEqual(upload.call_count, 2)
        self.sleep.assert_not_called()

    def test_http_error_classification_keeps_500_ambiguous(self):
        for status in (400, 500):
            output = json.dumps({"errorCode": 1012}) + f"\n{status}"
            with self.subTest(status=status), patch.object(mod_sites.subprocess, "run", return_value=
                    subprocess.CompletedProcess([], 0, stdout=output)), self.assertRaises(
                        mod_sites.Rejected if status == 400 else ValueError):
                mod_sites.multipart("example", "test", {})


if __name__ == "__main__":
    unittest.main()
