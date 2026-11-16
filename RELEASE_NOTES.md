## ETF Tracker v1.0.2

### 변경 사항
- **GitHub Secrets 자동 주입**: 빌드 시 `API_BASE_URL` 및 `APP_TOKEN`을 자동 주입하여, 앱 설치 후 별도 입력 없이 즉시 `EveryCorp` 서버와 연동됩니다.
- 앱 내 **앱 정보 · 설정**에서 직접 서버 URL/토큰을 변경해 덮어쓸 수도 있으며, 수동 입력 시 **Android Keystore(AES-256-GCM)**로 암호화 저장됩니다.
