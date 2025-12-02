import sys
import subprocess
import tempfile
import shutil
import os

def check_pip_installable_version(package_specifier):
    """
    Checks if a specific package version (e.g., "shioaji==1.1.5") is installable via pip.
    This is done by attempting to install it into a temporary virtual environment.
    """
    print(f"Checking if '{package_specifier}' is installable via pip...")

    temp_dir = None
    try:
        # Create a temporary directory for the virtual environment
        temp_dir = tempfile.mkdtemp()
        venv_path = os.path.join(temp_dir, "temp_venv")
        
        # Create virtual environment
        print(f"  Creating temporary virtual environment in {venv_path}...")
        subprocess.run([sys.executable, "-m", "venv", venv_path], check=True, capture_output=True)
        
        # Determine pip executable path within the venv
        pip_executable = os.path.join(venv_path, "bin", "pip")
        if not os.path.exists(pip_executable): # For some systems, pip might be in Scripts on Windows
             pip_executable = os.path.join(venv_path, "Scripts", "pip")

        # Attempt to install the package
        print(f"  Attempting to install '{package_specifier}' into temp venv...")
        # Use --index-url https://pypi.org/simple/ for explicit PyPI to avoid local caches interfering
        install_command = [pip_executable, "install", "--disable-pip-version-check", "--no-warn-script-location", "--index-url", "https://pypi.org/simple/", package_specifier]
        result = subprocess.run(install_command, capture_output=True, text=True)
        
        if result.returncode == 0:
            print(f"✅ Success: '{package_specifier}' is installable via pip.")
            return True
        else:
            print(f"❌ Failure: '{package_specifier}' is NOT installable via pip.")
            print("--- pip install stdout ---")
            print(result.stdout)
            print("--- pip install stderr ---")
            print(result.stderr)
            print("--------------------------")
            return False
            
    except subprocess.CalledProcessError as e:
        print(f"❌ Error during virtual environment creation or pip execution: {e}")
        print(f"Stdout: {e.stdout}")
        print(f"Stderr: {e.stderr}")
        return False
    except Exception as e:
        print(f"❌ An unexpected error occurred: {e}")
        return False
    finally:
        # Clean up the temporary directory
        if temp_dir and os.path.exists(temp_dir):
            print(f"  Cleaning up temporary virtual environment at {temp_dir}...")
            shutil.rmtree(temp_dir)

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python verify_dependencies.py <package-specifier>")
        print("Example: python verify_dependencies.py shioaji==1.1.5")
        sys.exit(1)

    package_spec = sys.argv[1]

    if not check_pip_installable_version(package_spec):
        sys.exit(1)
    
    sys.exit(0)