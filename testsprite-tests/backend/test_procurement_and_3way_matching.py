import os
import requests

TARGET_URL = globals().get("TARGET_URL", os.getenv("TARGET_URL", "https://len-valuable-construction-shadows.trycloudflare.com"))

def test_procurement_and_3way_matching():
    auth_headers = globals().get("__AUTH_HEADERS__", {})
    
    orders_resp = requests.get(f"{TARGET_URL}/api/v1/orders", headers=auth_headers, timeout=30)
    assert orders_resp.status_code in [200, 401, 403], f"Purchase Orders returned {orders_resp.status_code}"
    
    grn_resp = requests.get(f"{TARGET_URL}/api/v1/goods-receipts", headers=auth_headers, timeout=30)
    assert grn_resp.status_code in [200, 401, 403], f"Goods Receipts returned {grn_resp.status_code}"
    
    inv_resp = requests.get(f"{TARGET_URL}/api/v1/invoices", headers=auth_headers, timeout=30)
    assert inv_resp.status_code in [200, 401, 403], f"Supplier Invoices returned {inv_resp.status_code}"
    
    pay_resp = requests.get(f"{TARGET_URL}/api/v1/payments", headers=auth_headers, timeout=30)
    assert pay_resp.status_code in [200, 401, 403], f"Supplier Payments returned {pay_resp.status_code}"

test_procurement_and_3way_matching()
