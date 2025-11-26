"""
Configuration management for Shioaji Bridge
Loads credentials from environment variables or encrypted .env file
"""
import os
import sys
import base64
import hashlib
from pathlib import Path
from typing import Optional
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.backends import default_backend

# Encryption constants (Jasypt-compatible)
ITERATIONS = 1000
KEY_SIZE = 32
IV_SIZE = 16
SALT_SIZE = 16
SENSITIVE_KEYS = {'SHIOAJI_API_KEY', 'SHIOAJI_SECRET_KEY', 'SHIOAJI_PERSON_ID'}

def _derive_key_and_iv(password: str, salt: bytes) -> tuple:
    """Derive key and IV from password using PBKDF2-HMAC-SHA512."""
    key_iv = hashlib.pbkdf2_hmac(
        'sha512',
        password.encode('utf-8'),
        salt,
        ITERATIONS,
        dklen=KEY_SIZE + IV_SIZE
    )
    return key_iv[:KEY_SIZE], key_iv[KEY_SIZE:KEY_SIZE + IV_SIZE]

def _encrypt(plaintext: str, password: str) -> str:
    """Encrypt plaintext and return ENC(base64) format."""
    salt = os.urandom(SALT_SIZE)
    key, iv = _derive_key_and_iv(password, salt)
    
    pad_len = 16 - (len(plaintext) % 16)
    padded = plaintext + chr(pad_len) * pad_len
    
    cipher = Cipher(algorithms.AES(key), modes.CBC(iv), backend=default_backend())
    encryptor = cipher.encryptor()
    ciphertext = encryptor.update(padded.encode('utf-8')) + encryptor.finalize()
    
    combined = salt + ciphertext
    encoded = base64.b64encode(combined).decode('utf-8')
    return f"ENC({encoded})"

def _decrypt(encrypted: str, password: str) -> str:
    """Decrypt ENC(base64) format and return plaintext."""
    if not encrypted.startswith("ENC(") or not encrypted.endswith(")"):
        return encrypted  # Not encrypted, return as-is
    
    encoded = encrypted[4:-1]
    combined = base64.b64decode(encoded)
    
    salt = combined[:SALT_SIZE]
    ciphertext = combined[SALT_SIZE:]
    
    key, iv = _derive_key_and_iv(password, salt)
    
    cipher = Cipher(algorithms.AES(key), modes.CBC(iv), backend=default_backend())
    decryptor = cipher.decryptor()
    padded = decryptor.update(ciphertext) + decryptor.finalize()
    
    pad_len = padded[-1]
    return padded[:-pad_len].decode('utf-8')

def _load_env_with_decryption(env_path: Path, password: Optional[str] = None) -> dict:
    """Load .env file and decrypt ENC() values if password provided."""
    env_vars = {}
    
    if not env_path.exists():
        return env_vars
    
    with open(env_path, 'r') as f:
        for line in f:
            stripped = line.strip()
            if stripped and not stripped.startswith('#') and '=' in stripped:
                key, value = stripped.split('=', 1)
                if value.startswith('ENC(') and password:
                    try:
                        env_vars[key] = _decrypt(value, password)
                    except Exception:
                        env_vars[key] = value  # Keep encrypted if decryption fails
                else:
                    env_vars[key] = value
    
    return env_vars

class Config:
    """Configuration class for Shioaji API credentials and settings"""

    def __init__(self, decrypt_key: Optional[str] = None):
        # Try to get decrypt key from env or argument
        self.decrypt_key = decrypt_key or os.getenv('DECRYPT_KEY')
        
        # Load .env file with decryption
        env_path = Path(__file__).parent.parent / '.env'
        if not env_path.exists():
            env_path = Path(__file__).parent / '.env'
        
        env_vars = _load_env_with_decryption(env_path, self.decrypt_key)
        
        # Set environment variables for other code that might need them
        for k, v in env_vars.items():
            os.environ[k] = v

        # Load credentials from environment
        self.api_key: Optional[str] = os.getenv('SHIOAJI_API_KEY')
        self.secret_key: Optional[str] = os.getenv('SHIOAJI_SECRET_KEY')
        self.person_id: Optional[str] = os.getenv('SHIOAJI_PERSON_ID')
        self.ca_path: Optional[str] = os.getenv('SHIOAJI_CA_PATH')
        self.ca_passwd: Optional[str] = os.getenv('SHIOAJI_CA_PASSWD')

        # Optional settings
        self.simulation: bool = os.getenv('SHIOAJI_SIMULATION', 'false').lower() == 'true'
        self.timeout: int = int(os.getenv('SHIOAJI_TIMEOUT', '30'))
        self.log_level: str = os.getenv('SHIOAJI_LOG_LEVEL', 'INFO')

    def validate(self) -> tuple[bool, str]:
        """Validate that all required credentials are present"""
        if not self.api_key:
            return False, "SHIOAJI_API_KEY environment variable is not set"
        if not self.secret_key:
            return False, "SHIOAJI_SECRET_KEY environment variable is not set"
        if not self.person_id:
            return False, "SHIOAJI_PERSON_ID environment variable is not set"
        return True, ""

    def is_simulation_mode(self) -> bool:
        return self.simulation

    def get_timeout(self) -> int:
        return self.timeout

    def __repr__(self) -> str:
        return (
            f"Config(api_key={'***' if self.api_key else None}, "
            f"secret_key={'***' if self.secret_key else None}, "
            f"person_id={'***' if self.person_id else None}, "
            f"simulation={self.simulation}, timeout={self.timeout})"
        )


def encrypt_env_file(env_path: str, password: str):
    """Encrypt sensitive values in .env file."""
    with open(env_path, 'r') as f:
        lines = f.readlines()
    
    encrypted_lines = []
    for line in lines:
        stripped = line.strip()
        if stripped and not stripped.startswith('#') and '=' in stripped:
            key, value = stripped.split('=', 1)
            if key in SENSITIVE_KEYS and value and not value.startswith('ENC('):
                encrypted_value = _encrypt(value, password)
                encrypted_lines.append(f"{key}={encrypted_value}\n")
            else:
                encrypted_lines.append(line)
        else:
            encrypted_lines.append(line)
    
    with open(env_path, 'w') as f:
        f.writelines(encrypted_lines)
    
    print(f"Encrypted sensitive values in {env_path}")


if __name__ == "__main__":
    # CLI for encrypt command
    if len(sys.argv) >= 3 and sys.argv[1] == "encrypt":
        env_file = Path(__file__).parent.parent / '.env'
        encrypt_env_file(str(env_file), sys.argv[2])
    else:
        print("Usage: python config.py encrypt <password>")
