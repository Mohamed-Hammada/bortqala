import requests

def test_approval_workflow():
    auth_headers = globals().get("__AUTH_HEADERS__", {})
    
    # 1. Pending tasks
    tasks_resp = requests.get(f"{TARGET_URL}/api/v1/approvals/tasks", headers=auth_headers, timeout=30)
    assert tasks_resp.status_code in [200, 401, 403], f"Approval tasks returned {tasks_resp.status_code}"
    
    # 2. Workflow definitions
    defs_resp = requests.get(f"{TARGET_URL}/api/v1/approvals/definitions", headers=auth_headers, timeout=30)
    assert defs_resp.status_code in [200, 401, 403], f"Approval definitions returned {defs_resp.status_code}"

test_approval_workflow()
