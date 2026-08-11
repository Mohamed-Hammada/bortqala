# API integration support matrix

This matrix is generated from each supplier package. `implementation_status` distinguishes a usable generic transport/probe from a complete vendor-specific business wrapper.

| Supplier | Route | Version policy | Version constraint | Status |
|---|---|---|---|---|
| zkteco | `time-cloud-zlink` | capability-or-vendor-matrix | `*` | generic-client-or-probe |
| zkteco | `wdms-api` | capability-or-vendor-matrix | `*` | generic-client-or-probe |
| zkteco | `zkbio-cvaccess-api` | server-coupled | `*` | generic-client-or-probe |
| zkteco | `zkbio-cvsecurity-api` | server-coupled | `*` | generic-client-or-probe |
| zkteco | `zkbio-time-api` | server-coupled | `*` | generic-client-or-probe |
| hikvision | `device-gateway` | capability-or-vendor-matrix | `*` | generic-client-or-probe |
| hikvision | `hikcentral-openapi` | capability-or-vendor-matrix | `*` | generic-client-or-probe |
| hikvision | `isapi` | capability-negotiated | `*` | generic-client-or-probe |
| dahua | `dss-dip-bridge` | capability-or-vendor-matrix | `*` | generic-client-or-probe |
| dahua | `dss-openapi` | dss-server-coupled | `*` | generic-client-or-probe |
| suprema | `biostar2-api-v1` | capability-or-vendor-matrix | `1.*` | generic-client-or-probe |
| suprema | `biostar2-api-v2` | capability-or-vendor-matrix | `2.*` | generic-client-or-probe |
| suprema | `biostar2-local-api` | server-coupled | `>=2.7.10` | generic-client-or-probe |
| virdi | `accessmanager-pro` | capability-or-vendor-matrix | `*` | generic-client-or-probe |
| virdi | `alpeta-api` | alpeta-server-coupled | `*` | generic-client-or-probe |
| virdi | `unis-server` | capability-or-vendor-matrix | `*` | generic-client-or-probe |
| anviz | `crosschex-cloud-api` | capability-or-vendor-matrix | `*` | generic-client-or-probe |
| anviz | `crosschex-cloud-webhook` | capability-or-vendor-matrix | `*` | generic-client-or-probe |
| anviz | `crosschex-standard` | application-version-coupled | `*` | generic-client-or-probe |
| honeywell | `panel-through-prowatch` | capability-or-vendor-matrix | `*` | generic-client-or-probe |
| honeywell | `prowatch-db-integration` | capability-or-vendor-matrix | `*` | generic-client-or-probe |
| honeywell | `prowatch-web-services` | server-coupled | `*` | generic-client-or-probe |

See the supplier `DOCUMENTATION.md` and `profiles/integration_versions.json` for official documentation links and route-specific notes.
