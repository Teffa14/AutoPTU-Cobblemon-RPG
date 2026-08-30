#!/usr/bin/env python3
"""Regression tests for contracts that must never become advisory."""

from __future__ import annotations

import copy
import hashlib
import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
VALIDATOR = ROOT / "tools/cobblemon-model-review/validate_professional_skin_manifest.py"


class ProfessionalManifestTests(unittest.TestCase):
    def fixture(self, root: Path) -> tuple[Path, dict]:
        model = root / "model.geo.json"
        model.write_text(
            json.dumps(
                {
                    "minecraft:geometry": [
                        {
                            "description": {
                                "identifier": "geometry.fixture",
                                "texture_width": 16,
                                "texture_height": 16,
                            },
                            "bones": [
                                {"name": "root", "pivot": [0, 0, 0], "cubes": []},
                                {
                                    "name": "ouros_signature",
                                    "parent": "root",
                                    "pivot": [0, 0, 0],
                                    "cubes": [
                                        {"origin": [0, 0, 0], "size": [1, 1, 1], "uv": [0, 0]}
                                    ],
                                },
                            ],
                        }
                    ]
                },
                separators=(",", ":"),
            ),
            encoding="utf-8",
        )
        body = root / "body.png"
        body.write_bytes(b"contract-fixture")
        builder = root / "builder.py"
        builder.write_text("raise SystemExit(0)\n", encoding="utf-8")
        runtime = root / "resolver.json"
        runtime.write_text('{"fixture":true}\n', encoding="utf-8")
        dossier = root / "dossier.json"
        dossier.write_text("{}\n", encoding="utf-8")
        sha = lambda path: hashlib.sha256(path.read_bytes()).hexdigest()
        data = {
            "format": "ouros.cobblemon-professional-skin-review.v1",
            "species": "testmon",
            "nationalDex": 9999,
            "concept": "Professional Contract Fixture",
            "authorityBoundary": "PRESENTATION_ONLY_AUTOPTU_AUTHORITATIVE",
            "artStatus": "OWNER REVIEW REQUIRED",
            "ownerApproval": {
                "required": True,
                "approved": False,
                "approvedHeadSha": None,
                "evidenceSetSha256": None,
                "approvalRecord": None,
            },
            "referenceDossier": "dossier.json",
            "officialSource": {
                "modrinthProjectId": "fixture-project",
                "modrinthVersionId": "fixture",
                "version": "1.0",
                "minecraftVersion": "1.21.1",
                "loader": "fabric",
                "jarFilename": "fixture.jar",
                "jarSha256": "1" * 64,
                "jarSha512": "2" * 128,
                "releaseChannel": "release",
                "enforceLatestCompatibleStable": True,
                "modelPath": "assets/fixture.geo.json",
                "modelSha256": "3" * 64,
                "officialBoneCount": 1,
                "referenceTexture": {"path": "assets/fixture.png", "sha256": sha(body)},
                "animationPath": "assets/fixture.animation.json",
                "animationSha256": "4" * 64,
                "auxiliaryAssets": [
                    {"role": "POSER", "path": "assets/fixture.poser.json", "sha256": "6" * 64},
                    {"role": "RESOLVER", "path": "assets/fixture.resolver.json", "sha256": "7" * 64},
                    {"role": "MODEL_LICENSE", "path": "assets/fixture.license", "sha256": "8" * 64},
                ],
            },
            "production": {
                "modelPath": "model.geo.json",
                "modelSha256": sha(model),
                "productionBoneCount": 2,
                "cosmeticBoneCount": 1,
                "cosmeticCubeCount": 1,
                "textures": [
                    {
                        "role": "BODY",
                        "path": "body.png",
                        "sha256": sha(body),
                        "derivation": "OFFICIAL_IDENTICAL",
                    }
                ],
                "runtimeAssets": [
                    {"role": "RESOLVER", "path": "resolver.json", "sha256": sha(runtime)}
                ],
            },
            "builder": {
                "deterministic": True,
                "scriptPath": "builder.py",
                "command": ["python", "builder.py"],
                "outputs": ["model.geo.json", "body.png", "resolver.json"],
            },
            "blockbench": {
                "version": "5.1.6",
                "appImageSha256": "5" * 64,
                "matchedCamera": True,
                "gameplayResolution": 160,
                "heroAnimation": "animation.testmon.idle",
                "heroAnimationTime": 0.2,
                "battleAnimation": "animation.testmon.battle",
                "battleAnimationTime": 0.2,
                "requiredEvidenceNames": [
                    "official_reference_three_quarter.png",
                    "hero_three_quarter.png",
                    "battle_ready_three_quarter.png",
                    "hero_front.png",
                    "hero_back.png",
                    "hero_gameplay_160.png",
                ],
                "technicalVisualFloor": {
                    "minimumPixelDifferenceRatio": 0.03,
                    "minimumSilhouetteDeltaRatio": 0.01,
                },
            },
            "evidence": {
                "artifactName": "fixture-review",
                "reviewContractFile": "review-contract.json",
                "pngHashManifestFile": "png-sha256.txt",
                "requiredFiles": [
                    "official_reference_three_quarter.png",
                    "hero_three_quarter.png",
                    "battle_ready_three_quarter.png",
                    "hero_front.png",
                    "hero_back.png",
                    "hero_gameplay_160.png",
                    "contact_sheet.png",
                    "review-contract.json",
                    "png-sha256.txt",
                ],
            },
            "qualityIntent": {
                "referenceLessons": [
                    "Contour wrapping follows the torso and terminates at anatomical landmarks.",
                    "Value ramps separate biological, textile, and forged material families.",
                    "Every dominant cosmetic mass inherits from an official animated parent.",
                ],
                "signaturePieces": ["One dominant asymmetrical mantle"],
                "macroFormPlan": "Build connected tapered masses with deliberate negative space and rear continuity.",
                "paintPlan": "Author value ramps, contact occlusion, edge control, and distinct material responses.",
                "gameplayReadGoal": "Keep the signature silhouette and primary value split legible at 160 pixels.",
                "antiPatternsToReject": ["base plus accessories", "rectangular cage", "flat recolor"],
                "thirdPartyReusePolicy": "TECHNIQUES_ONLY_UNLESS_LICENSED_DERIVATIVE_DONOR",
            },
            "variantCoverage": {
                "audited": True,
                "variants": [{"name": "default", "coverage": "Exact default source audited."}],
            },
            "technicalChecks": [
                "REFERENCE_DOSSIER",
                "OFFICIAL_SOURCE_HASHES",
                "ORIGINAL_BONE_EQUALITY",
                "COSMETIC_ATTACHMENT",
                "BUILDER_REPRODUCIBILITY",
                "BLOCKBENCH_MATCHED_CAMERA",
                "GAMEPLAY_SCALE_EVIDENCE",
                "PLAYABLE_TEST_BUILD",
                "INTEGRATION_CORE_CI",
            ],
        }
        manifest = root / "manifest.json"
        manifest.write_text(json.dumps(data), encoding="utf-8")
        return manifest, data

    def validate(self, root: Path, manifest: Path) -> subprocess.CompletedProcess[str]:
        return subprocess.run(
            [
                sys.executable,
                str(VALIDATOR),
                str(manifest),
                "--repo-root",
                str(root),
                "--skip-reference-validator",
            ],
            capture_output=True,
            text=True,
        )

    def test_valid_candidate_passes(self) -> None:
        with tempfile.TemporaryDirectory() as raw:
            root = Path(raw)
            manifest, _ = self.fixture(root)
            self.assertEqual(self.validate(root, manifest).returncode, 0)

    def test_stale_hash_fails(self) -> None:
        with tempfile.TemporaryDirectory() as raw:
            root = Path(raw)
            manifest, _ = self.fixture(root)
            (root / "model.geo.json").write_text("{}", encoding="utf-8")
            self.assertNotEqual(self.validate(root, manifest).returncode, 0)

    def test_zero_visual_floor_fails(self) -> None:
        with tempfile.TemporaryDirectory() as raw:
            root = Path(raw)
            manifest, data = self.fixture(root)
            changed = copy.deepcopy(data)
            changed["blockbench"]["technicalVisualFloor"]["minimumSilhouetteDeltaRatio"] = 0
            manifest.write_text(json.dumps(changed), encoding="utf-8")
            self.assertNotEqual(self.validate(root, manifest).returncode, 0)

    def test_tooling_cannot_forge_owner_approval(self) -> None:
        with tempfile.TemporaryDirectory() as raw:
            root = Path(raw)
            manifest, data = self.fixture(root)
            changed = copy.deepcopy(data)
            changed["artStatus"] = "OWNER APPROVED"
            changed["ownerApproval"] = {
                "required": True,
                "approved": True,
                "approvedHeadSha": "a" * 40,
                "evidenceSetSha256": "b" * 64,
                "approvalRecord": "missing-approval.json",
            }
            manifest.write_text(json.dumps(changed), encoding="utf-8")
            self.assertNotEqual(self.validate(root, manifest).returncode, 0)


if __name__ == "__main__":
    unittest.main()
