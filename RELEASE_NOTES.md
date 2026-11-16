## ETF Tracker v1.0.1

### 보안 강화
- 공개 APK 디컴파일 시 토큰 노출을 방지하기 위해 **`APP_TOKEN`을 APK(`BuildConfig`)에 내장하지 않도록 제거**했습니다.
- 앱 내 **앱 정보 · 설정**에서 직접 입력한 토큰은 **Android Keystore(AES-256-GCM)**로 암호화되어 기기 내부에만 저장됩니다.
