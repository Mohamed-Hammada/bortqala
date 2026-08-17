import os
import requests

TARGET_URL = globals().get("TARGET_URL", os.getenv("TARGET_URL", "https://len-valuable-construction-shadows.trycloudflare.com"))

def test_finance_and_dashboard():
    auth_headers = globals().get("__AUTH_HEADERS__", {})
    
    je_resp = requests.get(f"{TARGET_URL}/api/v1/finance/journal-entries", headers=auth_headers, timeout=30)
    assert je_resp.status_code in [200, 401, 403], f"Journal entries returned {je_resp.status_code}"
    
    num_resp = requests.get(f"{TARGET_URL}/api/v1/finance/numbering-settings", headers=auth_headers, timeout=30)
    assert num_resp.status_code in [200, 401, 403], f"Numbering settings returned {num_resp.status_code}"
    
    dash_resp = requests.get(f"{TARGET_URL}/api/v1/dashboard/summary", headers=auth_headers, timeout=30)
    assert dash_resp.status_code in [200, 401, 403], f"Dashboard summary returned {dash_resp.status_code}"
    
    trends_resp = requests.get(f"{TARGET_URL}/api/v1/dashboard/trends?months=6", headers=auth_headers, timeout=30)
    assert trends_resp.status_code in [200, 401, 403], f"Dashboard trends returned {trends_resp.status_code}"

test_finance_and_dashboard()
