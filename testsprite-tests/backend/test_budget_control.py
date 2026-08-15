import os
import requests

TARGET_URL = globals().get("TARGET_URL", os.getenv("TARGET_URL", "https://len-valuable-construction-shadows.trycloudflare.com"))

def test_budget_control():
    auth_headers = globals().get("__AUTH_HEADERS__", {})
    
    budgets_resp = requests.get(f"{TARGET_URL}/api/v1/budget/budgets", headers=auth_headers, timeout=30)
    assert budgets_resp.status_code in [200, 401, 403], f"Budgets returned {budgets_resp.status_code}"
    
    status_resp = requests.get(f"{TARGET_URL}/api/v1/budget/status", headers=auth_headers, timeout=30)
    assert status_resp.status_code in [200, 401, 403], f"Budget status returned {status_resp.status_code}"
    
    enc_resp = requests.get(f"{TARGET_URL}/api/v1/budget/encumbrances", headers=auth_headers, timeout=30)
    assert enc_resp.status_code in [200, 401, 403], f"Encumbrances returned {enc_resp.status_code}"

test_budget_control()
