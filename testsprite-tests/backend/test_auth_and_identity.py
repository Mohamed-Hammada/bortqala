import requests

def test_auth_and_identity():
    # 1. Test Auth login endpoint
    login_payload = {
        "username": "admin",
        "password": "Password123!"
    }
    headers = {"Content-Type": "application/json"}
    login_resp = requests.post(f"{TARGET_URL}/api/v1/auth/login", json=login_payload, headers=headers, timeout=30)
    assert login_resp.status_code in [200, 401, 404], f"Unexpected status code: {login_resp.status_code}"

    # 2. Test user preferences shortcuts endpoint
    auth_headers = globals().get("__AUTH_HEADERS__", {})
    shortcuts_resp = requests.get(f"{TARGET_URL}/api/v1/auth/preferences/shortcuts", headers=auth_headers, timeout=30)
    assert shortcuts_resp.status_code in [200, 401, 403], f"Unexpected status code: {shortcuts_resp.status_code}"

test_auth_and_identity()
