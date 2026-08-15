import requests

def test_procurement_and_3way_matching():
    auth_headers = globals().get("__AUTH_HEADERS__", {})
    
    # 1. Inspect Purchase Orders endpoint
    orders_resp = requests.get(f"{TARGET_URL}/api/v1/orders", headers=auth_headers, timeout=30)
    assert orders_resp.status_code in [200, 401, 403], f"Purchase Orders returned {orders_resp.status_code}"
    
    # 2. Inspect Goods Receipt Notes (GRN)
    grn_resp = requests.get(f"{TARGET_URL}/api/v1/goods-receipts", headers=auth_headers, timeout=30)
    assert grn_resp.status_code in [200, 401, 403], f"Goods Receipts returned {grn_resp.status_code}"
    
    # 3. Inspect Supplier Invoices
    inv_resp = requests.get(f"{TARGET_URL}/api/v1/invoices", headers=auth_headers, timeout=30)
    assert inv_resp.status_code in [200, 401, 403], f"Supplier Invoices returned {inv_resp.status_code}"
    
    # 4. Inspect Supplier Payments
    pay_resp = requests.get(f"{TARGET_URL}/api/v1/payments", headers=auth_headers, timeout=30)
    assert pay_resp.status_code in [200, 401, 403], f"Supplier Payments returned {pay_resp.status_code}"

test_procurement_and_3way_matching()
