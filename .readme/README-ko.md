<div align="center">
  <p>
    <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="Archive Manager" width="128" />
  </p>

  <h1>Archive Manager</h1>

  <p>AutoJs6 파일 관리자에 통합되어 지원 형식을 탐색, 압축 해제, 생성 및 안전하게 편집하는 범용 압축 파일 관리자</p>

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

Archive Manager는 AutoJs6 파일 관리자를 대체하지 않고 그 안에서 동작합니다. 지원되는 압축 파일은 호스트의 목록, 경로 표시줄, 테마, 뷰어, 선택 모드, 진행 UI 및 디렉터리 새로 고침을 그대로 사용합니다. 별도 관리 페이지는 더 많은 설정이나 쓰기 작업이 필요할 때만 사용합니다.

### 현재 기능

- ZIP/JAR/AAR/WAR, 7Z, RAR4/RAR5 및 TAR 계열을 AutoJs6 기본 목록에서 탐색하고 내부 경로 표시줄, 검색, 정렬 및 뒤로 가기를 사용할 수 있습니다.
- 전체 압축 파일을 먼저 풀지 않고 읽을 수 있는 문서, 이미지, 오디오 및 비디오를 호스트의 기존 뷰어로 미리 볼 수 있습니다.
- 전체 압축 파일, 현재 내부 폴더 또는 선택 항목을 진행률, 취소, 안전한 충돌 이름, 게시 전 검증 및 커밋 전 롤백과 함께 추출할 수 있습니다.
- 암호화된 ZIP, 7Z 및 RAR은 처음 열거나 추출할 때 호스트 기본 암호 창을 사용하며, 잘못된 암호는 현재 경로를 잃지 않고 다시 입력할 수 있습니다.
- 단일 항목 또는 같은 상위 폴더의 선택으로 ZIP, 7Z, TAR, TAR.GZ, TAR.XZ, TAR.BZ2 및 TAR.ZST를 만들 수 있습니다. ZIP은 AES-256, 표준 분할 및 항목별 개별 생성도 지원합니다.
- 일반 단일 볼륨 ZIP은 검증된 재구성으로 파일이나 전체 폴더 트리 추가, 빈 폴더 생성, 이름 변경 및 삭제를 수행하고 완전한 재읽기 후에만 원본을 원자적으로 교체합니다.
- 위험한 이름은 읽기 전용으로 격리하고 쓰기 전에 구조 및 리소스 제한을 적용하며, 가능하면 호스트의 seek 가능한 설명자에서 직접 읽습니다.

### 현재 형식

현재 버전은 다음 탐색 및 압축 풀기 가능 확장자를 인식합니다:

```text
zip, jar, aar, war, 7z, rar, tar, tar.gz, tgz, tar.xz, txz, tar.bz2, tbz2, tar.zst, tzst
```

현재 버전은 다음 형식을 만들 수 있습니다:

```text
zip, 7z, tar, tar.gz, tar.xz, tar.bz2, tar.zst
```

> 기본 통합에는 Explorer Action v11을 제공하는 AutoJs6 6.8.0, 버전 코드 5276 이상이 필요합니다. RAR은 의도적으로 읽기 전용입니다. 편집은 일반 단일 볼륨 `.zip`으로 제한됩니다. 호스트가 선택한 파일 설명자만 제공하므로 기존 분할 ZIP/RAR 세트는 아직 완전한 세트로 읽을 수 없습니다. RAR 첫 볼륨은 메타데이터를 표시할 수 있지만 추출은 비활성화됩니다. 생성 시 파일 이름 암호화, 압축 후 원본 삭제 및 7Z/RAR/TAR 계열 내부 편집은 지원하지 않습니다.

### 사용법

1. Archive Manager를 설치하고 AutoJs6 플러그인 센터에서 활성화합니다.
2. 지원 파일의 기본 동작을 누르거나 압축 파일 열기를 선택한 뒤 호스트 경로 표시줄로 일반 폴더처럼 탐색합니다.
3. 경로 표시줄 동작으로 현재 내부 폴더를 추출하고, 길게 눌러 선택 항목을 추출하거나 파일 메뉴의 압축 해제 위치...로 전체 파일을 추출합니다. 필요하면 암호 창이 표시됩니다.
4. 파일이나 폴더에서 압축...을 선택하거나 같은 디렉터리의 여러 항목을 선택해 아래 작업 표시줄의 압축...을 사용합니다.
5. 내용을 추가, 이름 변경 또는 삭제할 때만 일반 단일 볼륨 ZIP에서 압축 파일 관리...를 선택합니다.

### 권한 및 데이터

Archive Manager는 저장소 또는 네트워크 권한을 요청하지 않습니다. 호스트는 수명이 짧은 읽기 전용 설명자와 플러그인 UID에 고정된 출력 트랜잭션만 제공하므로 플러그인이 임의 파일 시스템 경로를 선택할 수 없습니다. Explorer Action v11은 제한된 동기 재시도 요청에서만 암호를 전달하며 호스트와 플러그인은 보유 버퍼를 즉시 제거하고 지우고 상태, 설정, 로그 또는 진단에 저장하지 않습니다. Android와 Java 라이브러리가 피할 수 없는 수명이 짧은 런타임 복사본을 만들 수 있으므로 이는 최선의 메모리 위생이며 절대적인 보장은 아닙니다. 위험한 경로는 격리되고 출력은 게시 전에 검증되며 리소스 예산 확인으로 구조 안전 검사가 비활성화되지 않습니다.

### Roadmap

남은 작업은 체크 가능한 항목으로 추적합니다. 분할 ZIP/RAR의 인접 볼륨 입력, 호스트 기본 파일 이름 인코딩 수정, 일반 ZIP 이외 형식의 쓰기 재구성, 실행 취소 또는 원본 삭제 트랜잭션, 접근성 검토 및 나머지 기기와 생성 도구 매트릭스입니다.

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/ROADMAP.md)

### 릴리스 노트

#### v2.4.0

_2026/08/26_

- `참고` 이 버전에는 Explorer Action v11을 제공하는 AutoJs6 6.8.0, 버전 코드 5276 이상이 필요합니다
- `추가` RAR4/RAR5 탐색, 미리 보기 및 추출을 지원하며 내용 암호화와 헤더 암호화 입력도 처리합니다. RAR은 의도적으로 읽기 전용입니다
- `추가` AutoJs6 기본 압축 파일 페이지가 처음 열거나 추출하는 동안 암호를 요청하고 현재 경로나 선택을 잃지 않고 다시 시도할 수 있습니다
- `수정` 분할 RAR 첫 볼륨은 읽을 수 있는 메타데이터를 유지하지만 인접 볼륨을 사용할 수 없을 때 추출 동작을 더 이상 표시하지 않습니다
- `수정` 잘못된 암호는 이전 입력을 지우고 기본 페이지를 떠나지 않은 채 변경되지 않은 스냅샷으로 다시 시도합니다
- `개선` RAR은 가능할 때 호스트의 seek 가능한 설명자에서 직접 읽고 네이티브 ABI를 추가하지 않으며 공통 안전 검사를 재사용합니다
- `의존성` 포함된 라이선스 조건에 따라 읽기 전용 RAR을 제공하는 Junrar 8.1.0과 SLF4J 2.0.17을 추가했습니다

#### v2.3.0

_2026/08/26_

- `참고` 이 버전은 Explorer Action v10을 포함한 AutoJs6 6.8.0 버전 코드 5276 이상의 연계 빌드가 필요합니다
- `추가` 네이티브 압축 파일 화면에서 별도의 관리 화면을 열지 않고 경로 표시줄의 현재 내부 폴더나 선택 표시줄의 선택 항목을 풀 수 있습니다
- `추가` 네이티브 압축 풀기는 호스트가 소유한 출력 트리에 쓰고 진행률, 취소, 안전한 충돌 번호 부여 및 Explorer 자동 새로 고침을 제공합니다
- `수정` Android 7에서 압축 파일을 나갈 때 호스트가 미리보기 캐시를 정리하며 충돌하는 문제를 수정했습니다
- `수정` 좁은 화면에서 5개 작업 파일 선택 표시줄의 텍스트가 아이콘 아래에 가운데 정렬됩니다
- `개선` 압축 파일 선택 모드에서는 나가기와 압축 풀기만 표시하고 압축 파일 내부에 적용되지 않는 파일 시스템 작업을 숨깁니다

#### v2.2.0

_2026/08/26_

- `참고` 이 버전에는 Explorer Action v9이 포함된 대응 AutoJs6 6.8.0 빌드 (버전 코드 5276 이상)가 필요합니다
- `추가` ‘압축 풀기...’에서 현재 폴더를 권장하고 호스트 소유 디렉터리 트랜잭션으로 같은 이름의 출력 폴더를 만듭니다. 동등한 이름이 이미 있으면 기존 내용을 변경하지 않고 안전하게 번호를 붙입니다
- `추가` 일반 단일 볼륨 ZIP 관리에서 Android 시스템 선택기를 통해 중첩 파일과 빈 폴더를 포함한 전체 폴더를 가져올 수 있습니다
- `수정` 압축 풀기 위치 선택 화면이 좁거나 낮은 화면에서도 완전히 사용할 수 있고 정확한 기본 경로를 표시합니다
- `수정` 같은 디렉터리 압축 풀기가 취소, 실패, 중단되거나 공간이 부족하면 게시되지 않은 출력을 롤백합니다. 다음 세션은 원본 압축 파일을 변경하지 않고 중단된 호스트 트랜잭션을 복구합니다
- `개선` Explorer Action v9이 검증된 디렉터리 트리를 원자적으로 게시하고 커밋 직후 AutoJs6에서 새 출력 폴더를 새로 고칩니다

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
- 타사 소프트웨어 고지: [THIRD_PARTY_NOTICES.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/THIRD_PARTY_NOTICES.md)
- Android Storage Access Framework: https://developer.android.com/guide/topics/providers/document-provider
