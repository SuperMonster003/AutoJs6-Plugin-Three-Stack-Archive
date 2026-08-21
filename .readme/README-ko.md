<div align="center">
  <p>
    <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="Archive Manager" width="128" />
  </p>

  <h1>Archive Manager</h1>

  <p>ZIP 압축 파일을 열고 풀고 만들기 위한 AutoJs6 파일 관리자 플러그인</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Archive-Manager?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Archive-Manager?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Archive-Manager?color=534BAE&label=License"/></a>
  </p>
</div>

### 언어 (Languages)

README는 다음 언어로 제공됩니다:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-zh-Hans.md)
- [香港繁體 [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-zh-Hant-HK.md)
- [台灣繁體 [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-ja.md)
- 한국어 [ko] # 현재
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/.readme/README-ar.md)

### 소개

Archive Manager는 ZIP 탐색, 압축 풀기 및 만들기를 AutoJs6 파일 관리자에 통합합니다. 현재 버전은 단일 항목이나 같은 상위 폴더의 다중 선택을 압축하고 호스트가 제어하는 파일 세션을 통해 현재 폴더에 안전하게 기록합니다. 추가 형식, 호스트 네이티브 압축 파일 화면 및 내부 편집은 Roadmap에 따라 진행됩니다.

### 현재 기능

- AutoJs6 파일 메뉴에서 ZIP 계열 압축 파일 열기.
- ‘압축 풀기...’ 바로 가기로 탐색 화면을 먼저 열지 않고 전체 압축 파일 풀기.
- 폴더 탐색, 경로 검색, 파일 또는 폴더 선택.
- 모든 항목을 미리 풀지 않고 디렉터리 메타데이터로 목록 생성.
- Zip64, 자동 압축 풀기 형식의 앞부분 데이터, 기존 이름 인코딩, Windows 경로 구분자 지원.
- 개별 항목을 풀 수 없어도 나머지 내용은 계속 탐색 가능.
- Android 선택기로 지정한 폴더에 진행률 및 취소와 함께 선택 항목 풀기.
- 일반 파일, 폴더 및 같은 상위 폴더의 다중 선택에 ‘압축...’ 동작 제공.
- 파일 이름과 압축 수준을 설정하여 ZIP 만들기. 단일 항목은 대상 이름, 다중 항목은 상위 폴더 이름을 기본값으로 사용.
- 같은 폴더의 임시 파일에 쓴 뒤 원자적으로 확정하고 기존 파일을 덮어쓰지 않도록 충돌 이름에 번호 추가.

### 현재 형식

현재 버전은 다음 ZIP 계열 확장자를 인식합니다:

```text
zip, jar, aar, war
```

현재 버전은 다음 형식을 만들 수 있습니다:

```text
zip
```

> Explorer Action v4 통합에는 AutoJs6 버전 코드 5276 이상이 필요합니다. 7z, tar 계열, 비밀번호, 분할 압축, 파일 이름 암호화, 개별 압축, 원본 삭제 및 압축 파일 내부 추가/삭제는 아직 출시된 기능이 아닙니다. Roadmap 체크 상태를 기준으로 확인하세요.

### 사용법

1. 플러그인을 설치하고 AutoJs6 플러그인 센터에서 활성화합니다.
2. ZIP, JAR, AAR 또는 WAR 파일 메뉴를 엽니다.
3. ‘압축 파일 열기’를 선택한 뒤 탐색하거나 검색하여 내용을 선택합니다.
4. ‘선택 항목 압축 풀기’에서 출력 폴더를 지정합니다. 전체 파일은 파일 메뉴의 ‘압축 풀기...’를 바로 선택할 수 있습니다.
5. ZIP을 만들려면 일반 파일 또는 폴더 메뉴에서 ‘압축...’을 선택하거나 같은 폴더의 여러 항목을 선택한 뒤 하단 표시줄의 ‘압축...’을 사용합니다.

### 권한 및 데이터

저장소나 네트워크 권한을 요청하지 않습니다. 탐색과 압축 풀기는 호스트가 임시로 부여한 입력 URI만 사용합니다. ZIP 만들기는 플러그인 UID에 고정된 단기 호스트 세션으로 대상을 페이지 단위로 읽고 현재 상위 폴더에만 트랜잭션 출력을 만들 수 있습니다. 고정 4 GiB 입력 제한과 탐색 시 크기/압축률 제한은 제거했지만 경로 격리, 무결성 검증, 실패 정리는 유지됩니다.

### Roadmap

추가 형식, 비밀번호와 분할, 내부 편집, 호스트 네이티브 압축 파일 화면, 내부 경로 표시줄 및 전체 기기 매트릭스의 작업과 인수 조건은 Roadmap에 있습니다. 체크되지 않은 항목은 현재 기능이 아닙니다.

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/ROADMAP.md)

### 릴리스 노트

#### Unreleased

_미출시_

- `추가` 제품 이름을 Archive Manager로, 파일 동작을 ‘압축 파일 열기’로 통일
- `추가` 출력 위치를 선택해 전체 파일을 푸는 ‘압축 풀기...’ 바로 가기
- `추가` Explorer Action v4로 일반 파일/폴더 메뉴와 같은 상위 폴더 다중 선택의 다섯 동작 표시줄에 ‘압축...’ 추가
- `추가` 기본 이름, 압축 수준, 진행률, 취소 및 충돌 이름 자동 번호 지정을 지원하는 ZIP 만들기
- `수정` ZIP 목록을 메타데이터 방식으로 변경하고 자동 풀기 형식의 앞부분, 기존 이름 인코딩, Windows 구분자 및 더 많은 읽기 가능한 메서드 지원
- `수정` 알 수 없는 크기, 유효한 DocumentsProvider URI, 호스트의 추가 쓰기 권한 때문에 정상 파일이 거부되던 문제 수정
- `수정` 최신 시스템 전용 API 호출로 Android 7.x에서 ZIP을 탐색하거나 풀 수 없던 문제 수정
- `개선` 고정 4 GiB 제한과 탐색 시 크기/압축률 제한을 제거하면서 경로 격리와 무결성 검증 유지
- `개선` 체크 가능한 Roadmap을 추가하고 README와 CHANGELOG를 다시 작성
- `개선` 독립 화면이 시스템 주야간 모드와 Material 동적 색상을 따르도록 개선
- `개선` ZIP 출력을 플러그인 UID에 고정된 호스트 세션으로 같은 폴더의 임시 파일에 쓰고 저장소 권한이나 덮어쓰기 없이 원자적으로 확정

#### v1.0.1

_2026/08/08_

- `수정` 플러그인 활성화 시 서비스 바인딩이 비어 있던 문제
- `개선` 이름, 설명 및 사용 안내 간소화

#### v1.0.0

_2026/08/02_

- `추가` ZIP, JAR, AAR, WAR 탐색과 선택 내용 풀기를 제공한 최초 버전
- `추가` 검색, 선택, 진행률, 취소, 임시 데이터 정리 및 다국어 UI 추가

##### 전체 기록

* [CHANGELOG-ko.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/app/src/main/assets/doc/CHANGELOG-ko.md)

### 빌드

```powershell
.\gradlew.bat :app:assembleDebug
```

Release 빌드:

```powershell
.\gradlew.bat :app:assembleRelease
```

저장소 루트의 Gradle Wrapper를 사용하고 SDK/JDK 요구 사항은 `version.properties`를 따릅니다.

### 링크

- AutoJs6 문서: https://docs.autojs6.com
- Android Storage Access Framework: https://developer.android.com/guide/topics/providers/document-provider
