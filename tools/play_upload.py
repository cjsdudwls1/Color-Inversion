#!/usr/bin/env python3
"""AAB 를 Play 트랙에 올린다 (r0adkll 대체). 실패 시 API 응답 본문을 그대로 출력한다.

  python tools/play_upload.py <aab> --track alpha --version-code N [--notes-file F] [--status completed]

순서: edits.insert → bundles.upload → tracks.update → edits.validate → edits.commit.
commit 이 실패하면 changesNotSentForReview=true 로 한 번 더 시도한다(콘솔에서 '검토 전송' 필요).
서비스 계정 JSON 은 PLAY_SERVICE_ACCOUNT_JSON 환경변수. pip install google-auth requests
"""
import argparse, json, os, sys
from google.auth.transport.requests import AuthorizedSession
from google.oauth2 import service_account

PACKAGE = "com.cjsdudwls1.colorinversion"
API = f"https://androidpublisher.googleapis.com/androidpublisher/v3/applications/{PACKAGE}"
UP = f"https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications/{PACKAGE}"


def req(http, method, url, ok_fail=False, **kw):
    r = http.request(method, url, **kw)
    print(f"{method} {url.split(PACKAGE)[-1]} -> {r.status_code}")
    if r.status_code >= 400:
        # 여러 줄이면 GitHub 가 서비스계정 JSON 의 "{" "}" 줄과 같다고 *** 로 가린다 → 한 줄로
        print("BODY", " ".join(r.text.split()))
        if not ok_fail:
            sys.exit(1)
        return None
    return r.json() if r.content else {}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("aab"); ap.add_argument("--track", default="alpha")
    ap.add_argument("--version-code", type=int, required=True)
    ap.add_argument("--notes-file"); ap.add_argument("--status", default="completed")
    a = ap.parse_args()
    creds = service_account.Credentials.from_service_account_info(
        json.loads(os.environ["PLAY_SERVICE_ACCOUNT_JSON"]),
        scopes=["https://www.googleapis.com/auth/androidpublisher"])
    http = AuthorizedSession(creds)

    eid = req(http, "POST", f"{API}/edits")["id"]
    print("[진단] 빈 edit validate"); req(http, "POST", f"{API}/edits/{eid}:validate", ok_fail=True)
    with open(a.aab, "rb") as f:
        b = req(http, "POST", f"{UP}/edits/{eid}/bundles?uploadType=media", data=f,
                headers={"Content-Type": "application/octet-stream"}, timeout=600)
    print("uploaded versionCode", b.get("versionCode"))
    print("[진단] 업로드 후 validate"); req(http, "POST", f"{API}/edits/{eid}:validate", ok_fail=True)
    print("[진단] 현재 트랙", json.dumps(req(http, "GET", f"{API}/edits/{eid}/tracks"), ensure_ascii=False))
    rel = {"name": str(a.version_code), "versionCodes": [str(a.version_code)], "status": a.status}
    if a.notes_file:
        rel["releaseNotes"] = [{"language": "ko-KR", "text": open(a.notes_file, encoding="utf-8").read().strip()}]
    req(http, "PUT", f"{API}/edits/{eid}/tracks/{a.track}", json={"track": a.track, "releases": [rel]})
    req(http, "POST", f"{API}/edits/{eid}:validate", ok_fail=True)
    if req(http, "POST", f"{API}/edits/{eid}:commit", ok_fail=True) is not None:
        print("committed"); return
    print("retry commit with changesNotSentForReview=true")
    if req(http, "POST", f"{API}/edits/{eid}:commit?changesNotSentForReview=true", ok_fail=True) is not None:
        print("committed (검토 미전송 — Play Console 게시 개요에서 '검토를 위해 변경사항 전송' 필요)"); return
    req(http, "DELETE", f"{API}/edits/{eid}", ok_fail=True)
    sys.exit("commit failed")


if __name__ == "__main__":
    main()
