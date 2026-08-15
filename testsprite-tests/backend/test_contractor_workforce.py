import requests

def test_contractor_workforce():
    auth_headers = globals().get("__AUTH_HEADERS__", {})
    
    # 1. Labor requests
    req_resp = requests.get(f"{TARGET_URL}/api/v1/workforce/requests", headers=auth_headers, timeout=30)
    assert req_resp.status_code in [200, 401, 403], f"Workforce requests returned {req_resp.status_code}"
    
    # 2. Contractor settlements
    settle_resp = requests.get(f"{TARGET_URL}/api/v1/workforce/settlements", headers=auth_headers, timeout=30)
    assert settle_resp.status_code in [200, 401, 403], f"Contractor settlements returned {settle_resp.status_code}"

test_contractor_workforce()
