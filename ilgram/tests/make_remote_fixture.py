"""Regenerates ilgram/tests/fixtures/remote/*. Uses the same envelope code as the admin bot (TEST keys only)."""
import base64, json, os, pathlib
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import ec
from cryptography.hazmat.primitives.ciphers.aead import AESGCM

AAD = b"ILGRAM-CONFIG-v1"
out = pathlib.Path(__file__).parent / "fixtures" / "remote"
out.mkdir(parents=True, exist_ok=True)
b64 = lambda b: base64.b64encode(b).decode()


def seal(sk, aes, payload):
    pt = json.dumps(payload, ensure_ascii=False, separators=(",", ":"), sort_keys=True).encode()
    nonce = os.urandom(12)
    ct = AESGCM(aes).encrypt(nonce, pt, AAD)
    sig = sk.sign(AAD + nonce + ct, ec.ECDSA(hashes.SHA256()))
    return json.dumps({"format": "ilgram-config", "v": 1, "nonce": b64(nonce), "ct": b64(ct), "sig": b64(sig)}, separators=(",", ":"))


sk = ec.generate_private_key(ec.SECP256R1())
other = ec.generate_private_key(ec.SECP256R1())
aes = AESGCM.generate_key(bit_length=256)
pub = sk.public_key().public_bytes(serialization.Encoding.DER, serialization.PublicFormat.SubjectPublicKeyInfo)
payload = {"schema": 1, "version": 3, "published_at": "2026-10-15T08:00:00Z", "check_interval_minutes": 60,
           "default": {"groups": ["-1001234567890", "-987654321"], "users": ["111111111", "7012345678"]}, "profiles": {}}
(out / "public_key.b64").write_text(b64(pub))
(out / "aes_key.b64").write_text(b64(aes))
(out / "valid_v3.json").write_text(seal(sk, aes, payload))
(out / "valid_v2.json").write_text(seal(sk, aes, dict(payload, version=2, check_interval_minutes=5,
                                                        default={"groups": [], "users": ["5"]})))
(out / "wrong_signer.json").write_text(seal(other, aes, dict(payload, version=99)))
(out / "unicode.json").write_text(seal(sk, aes, dict(payload, version=4, published_at="O'zbek ✓ \"q\"")))
env = json.loads((out / "valid_v3.json").read_text())
ct = bytearray(base64.b64decode(env["ct"])); ct[0] ^= 1
env["ct"] = b64(bytes(ct))
(out / "tampered.json").write_text(json.dumps(env))
print("ok")
