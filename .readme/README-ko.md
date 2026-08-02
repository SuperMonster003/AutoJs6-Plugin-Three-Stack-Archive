<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-archive-browser-ic-launcher" border="0" width="128" />
  </p>

  <p>AutoJs6 탐색기를 위한 ZIP 계열 압축 파일 읽기 전용 탐색 및 선택적 SAF 압축 해제</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Archive-Browser?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Archive-Browser?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Archive-Browser?color=534BAE&label=License"/></a>
  </p>
</div>

******

### 언어 (Languages)

******

현재 README.md는 다음 언어를 지원합니다:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-zh-Hans.md)
- [香港繁體 [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-zh-Hant-HK.md)
- [台灣繁體 [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-ja.md)
- 한국어 [ko] # 현재
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/.readme/README-ar.md)

******

### 소개

******

AutoJs6 Archive Browser 플러그인은 AutoJs6 탐색기에 읽기 전용 압축 파일 탐색 기능을 추가합니다. ZIP 기반 컨테이너를 전용 계층 탐색기로 열고 사용자가 선택한 항목만 Android Storage Access Framework로 지정한 출력 폴더에 압축 해제합니다.

******

### 기능

******

- 공유 `org.autojs.plugin.EXPLORER_ACTION` 프로토콜을 통해 단일 파일용 읽기 전용 더보기 작업을 등록합니다.
- 압축 파일의 폴더를 탐색하고 압축 해제 크기, 압축 크기, CRC, 수정 시간 메타데이터를 표시합니다.
- 정규화된 항목 경로를 검색하고 개별 파일, 폴더 또는 표시된 모든 항목을 선택할 수 있습니다.
- 선택한 항목을 사용자가 지정한 SAF 트리에 압축 해제하며 진행률 표시와 취소를 지원합니다.
- 지원되는 ZIP 압축 방식을 사용하는 ZIP, JAR, AAR, WAR 컨테이너를 받습니다.
- 읽기 전용 입력을 비공개 캐시에 임시 저장하고 탐색기를 닫을 때 임시 데이터를 삭제합니다.

******

### 지원 형식

******

버전 1은 다음 ZIP 계열 파일 확장자를 인식합니다:

```text
zip, jar, aar, war
```

******

### 플러그인 인터페이스

******

AutoJs6는 다음 식별 정보로 플러그인을 검색하고 실행합니다:

```text
service action: org.autojs.plugin.EXPLORER_ACTION
execute action: org.autojs.plugin.EXPLORER_ACTION_EXECUTE
plugin id: archive-browser
engine: explorer-action
variant: default
```

버전 1은 AutoJs6 기본 탐색기의 단일 파일용 읽기 전용 더보기 작업으로 제한됩니다.

******

### 보안

******

플러그인은 저장소 또는 네트워크 권한을 요청하지 않습니다. 호스트는 입력 content URI에 임시 읽기 전용 접근 권한만 부여하고 출력 접근은 사용자가 명시적으로 선택한 SAF 디렉터리로 제한합니다. 절대 경로, 상위 디렉터리 이동, 드라이브 접두사, 역슬래시, 안전하지 않은 Unicode, 중복 경로, 파일과 디렉터리 충돌, 지원되지 않는 압축 방식, 크기 불일치, CRC 불일치는 거부됩니다.

******

### 안전 제한

******

- 임시 저장 입력 최대 크기: `4 GiB`.
- 암시적 디렉터리를 포함한 압축 파일 경로 노드 최대 수: `20,000`.
- ZIP 중앙 디렉터리 최대 크기: `64 MiB`.
- 정규화 경로 최대 길이: `1,024`자.
- 경로 최대 깊이: `64`단계.
- 단일 항목의 압축 해제 후 최대 크기: `512 MiB`.
- 압축 해제 후 전체 데이터 최대 크기: `2 GiB`.
- 최대 압축 비율: `1000:1`.

******

### 릴리스 내역

******

# v1.0.0

###### 2026/08/02

* `기능` 플러그인 ID `archive-browser`, 엔진 `explorer-action`, 변형 `default`인 Archive Browser 플러그인
* `기능` ZIP, JAR, AAR, WAR 단일 컨테이너용 읽기 전용 탐색기 작업과 계층 탐색, 경로 검색, 항목 선택
* `기능` 사용자가 선택한 SAF 트리로 선택 항목을 압축 해제하며 진행률과 취소를 지원하고 입력은 임시 읽기 전용으로 접근하며 저장소 또는 네트워크 권한을 사용하지 않음
* `기능` 입력 4 GiB, 암시적 디렉터리를 포함한 경로 노드 20,000개, ZIP 중앙 디렉터리 64 MiB, 압축 해제된 단일 항목 512 MiB, 압축 해제된 전체 데이터 2 GiB, 압축 비율 1000:1의 안전 제한
* `기능` 안전하지 않은 경로, 중복 또는 충돌 항목, 지원되지 않는 압축 방식, 입력 소스 변경, 크기 불일치, CRC 불일치 검증
* `기능` 스페인어/프랑스어/러시아어/아랍어/일본어/한국어/영어/중국어 간체/홍콩 중국어 번체/대만 중국어 번체로 현지화한 메타데이터, UI, 사용 설명, README, CHANGELOG

##### 더 많은 릴리스 내역

* [CHANGELOG-ko.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Browser/blob/master/app/src/main/assets/doc/CHANGELOG-ko.md)

******

### 빌드

******

```powershell
.\gradlew.bat :app:assembleDebug
```

Release 빌드:

```powershell
.\gradlew.bat :app:assembleRelease
```

빌드 매개변수는 `version.properties`에서 가져옵니다. 현재 최소 SDK는 24이고 대상 SDK는 36입니다.

******

### 리소스 구조

******

```text
.readme/lang_*.json
.changelog/lang_*.json
.python/generate_markdown.py
app/src/main/assets/doc/CHANGELOG-*.md
app/src/main/res/values-*/strings.xml
app/src/main/res/raw-*/plugin_instruction.md
```

`strings.xml`은 플러그인 메타데이터와 탐색기 UI를 현지화하고 `plugin_instruction.md`는 호스트에 표시할 사용 설명을 제공합니다. `.python/generate_markdown.py`가 JSON 소스에서 README와 변경 내역을 생성합니다.

******

### 링크

******

- AutoJs6 문서: https://docs.autojs6.com
- Android Storage Access Framework: https://developer.android.com/guide/topics/providers/document-provider
