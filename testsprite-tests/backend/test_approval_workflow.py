import os
import requests

TARGET_URL = globals().get("TARGET_URL", os.getenv("TARGET_URL", "https://len-valuable-construction-shadows.trycloudflare.com"))

def test_approval_workflow():
    auth_headers = globals().get("__AUTH_HEADERS__", {})
    
    tasks_resp = requests.get(f"{TARGET_URL}/api/v1/approvals/tasks", headers=auth_headers, timeout=30)
    assert tasks_resp.status_code in [200, 401, 403], f"Approval tasks returned {tasks_resp.status_code}"
    
    defs_resp = requests.get(f"{TARGET_URL}/api/v1/approvals/definitions", headers=auth_headers, timeout=30)
    assert defs_resp.status_code in [200, 401, 403], f"Approval definitions returned {defs_resp.status_code}"

test_approval_workflow()
