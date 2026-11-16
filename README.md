# ETF Tracker

**Editorial Private Wealth** 스타일의 정밀 ETF 분석 안드로이드 앱입니다.  
[`EveryCorp`](https://github.com/hdlee73/EveryCorp) 백엔드(Oracle Cloud 고정 IP · 키움 REST API · KRX · 공공데이터포털)와 연동하여 실시간 시세·PDF 구성종목 데이터를 조회할 수 있습니다.

## 주요 기능
1. **PDF(자산구성내역) 심층 해부 & X-Ray**: 상위 종목 비중 및 일별 기여도(`%p`), ETF 간 중복도(Venn/Bar), 액티브 ETF 리밸런싱 감지
2. **실부담비용 · 괴리율 · 추적오차**: KOFIA 기준 `[총보수 + 기타비용 + 매매·중개수수료율]` 1원 단위 비교, 실시간 iNAV 괴리율·LP 호가 공백 경보, 지수 추적오차 시계열
3. **배당 캐시플로우 & 절세 시뮬레이터**: TR vs 현금수령 복리 격차, ROC 원금 잠식 판별, 일반 vs ISA / 연금저축 / IRP 세후 실수령액 비교
4. **폴드8 대화면 Dual-Pane & AI 뉴스 임팩트**: 좌우 분할 비교 뷰 및 보유 종목 뉴스 호재/악재 매핑
5. **백테스팅 & 상관계수 매트릭스**: 위기 구간 MDD · 원금 회복 기간, 환노출(UH)/환헤지(H)/원달러 환율 상관계수 히트맵
6. **앱 정보 & 안전장치**: GitHub Releases 자동 업데이트 확인, 로컬 크래시 로그(`etf_tracker_crash.log`) 기록 및 조회

## 로컬 테스트용 Debug APK 빌드 방법
Android SDK가 설치된 로컬 환경(Android Studio 또는 CLI)에서 아래 명령을 실행하세요:

```bash
./gradlew testDebugUnitTest assembleDebug
```

생성된 APK 경로:
- `app/build/outputs/apk/debug/app-debug.apk`

## EveryCorp 서버 연동
앱 내 **좌측 메뉴 → 앱 정보 · 설정**에서 `EveryCorp API Base URL`과 `X-App-Token`을 입력하면 즉시 서버와 동기화됩니다.
