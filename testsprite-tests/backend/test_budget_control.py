import requests

def test_budget_control():
    auth_headers = globals().get("__AUTH_HEADERS__", {})
    
    # 1. Budgets listing
    budgets_resp = requests.get(f"{TARGET_URL}/api/v1/budget/budgets", headers=auth_headers, timeout=30)
    assert budgets_resp.status_code in [200, 401, 403], f"Budgets returned {budgets_resp.status_code}"
    
    # 2. Budget utilization status
    status_resp = requests.get(f"{TARGET_URL}/api/v1/budget/status", headers=auth_headers, timeout=30)
    assert status_resp.status_code in [200, 401, 403], f"Budget status returned {status_resp.status_code}"
    
    # 3. Encumbrances log
    enc_resp = requests.get(f"{TARGET_URL}/api/v1/budget/encumbrances", headers=auth_headers, timeout=30)
    assert enc_resp.status_code in [200, 401, 403], f"Encumbrances returned {enc_resp.status_code}"

test_budget_control()
