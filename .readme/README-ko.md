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
- 호스트 경로 표시줄에서 ZIP 파일 이름 인코딩을 직접 수정할 수 있습니다. 같은 읽기 전용 세션이 인덱스를 다시 만들고 가능한 경우 현재 내부 경로와 선택을 유지합니다.
- 완전한 표준 `.z01 + .zip`, 최신 WinRAR `partN.rar`, 번호형 `.zip.001` 및 번호형 `.7z.001` 세트를 호스트가 제한해 승인한 인접 볼륨 설명자로 탐색, 미리 보기 및 추출할 수 있습니다. 누락되거나 변경된 볼륨은 명시적으로 실패합니다.
- 단일 항목 또는 같은 상위 폴더의 선택으로 ZIP, 7Z, TAR, TAR.GZ, TAR.XZ, TAR.BZ2 및 TAR.ZST를 만들 수 있습니다. ZIP은 AES-256, 표준 분할 및 항목별 개별 생성도 지원합니다.
- 표준 분할 ZIP과 항목별 개별 압축은 검증된 모든 물리 출력을 하나의 복구 가능 배치로 게시합니다. 실패나 호스트 재시작을 성공한 부분 결과로 표시하지 않습니다.
- 일반 단일 볼륨 ZIP은 검증된 재구성으로 파일이나 전체 폴더 트리 추가, 빈 폴더 생성, 이름 변경 및 삭제를 수행하고 완전한 재읽기 후에만 원본을 원자적으로 교체합니다.
- 위험한 이름은 읽기 전용으로 격리하고 쓰기 전에 구조 및 리소스 제한을 적용하며, 가능하면 호스트의 seek 가능한 설명자에서 직접 읽습니다.

### 현재 형식

현재 버전은 다음 탐색 및 압축 풀기 가능 확장자를 인식합니다:

```text
zip, zip.001, jar, aar, war, 7z, 7z.001, rar, tar, tar.gz, tgz, tar.xz, txz, tar.bz2, tbz2, tar.zst, tzst
```

현재 버전은 다음 형식을 만들 수 있습니다:

```text
zip, 7z, tar, tar.gz, tar.xz, tar.bz2, tar.zst
```

> 기본 통합에는 Explorer Action v15를 제공하는 대응 AutoJs6 6.8.0 빌드 (버전 코드 5276 이상) 가 필요합니다. RAR과 분할 압축 파일은 읽기 전용이며 편집은 일반 단일 볼륨 `.zip`으로 제한됩니다. 표준 분할 ZIP은 최종 `.zip`에서, 최신 WinRAR 세트는 첫 `partN.rar`에서, 번호형 ZIP 또는 7Z는 `.001` 볼륨에서 열고 필요한 모든 볼륨을 같은 폴더에 두어야 합니다. 생성 시 파일 이름 암호화, 압축 후 원본 삭제 및 7Z/RAR/TAR 계열 내부 편집은 지원하지 않습니다.

### 사용법

1. Archive Manager를 설치하고 AutoJs6 플러그인 센터에서 활성화합니다.
2. 지원 파일의 기본 동작을 누르거나 압축 파일 열기를 선택한 뒤 호스트 경로 표시줄로 일반 폴더처럼 탐색합니다.
3. 경로 표시줄의 추출 동작으로 현재 내부 폴더를 추출하거나 파일 이름 인코딩 동작으로 ZIP 이름을 수정합니다. 길게 눌러 선택 항목을 추출하거나 파일 메뉴의 압축 해제 위치...로 전체 파일을 추출합니다. 필요하면 암호 창이 표시됩니다.
4. 파일이나 폴더에서 압축...을 선택하거나 같은 디렉터리의 여러 항목을 선택해 아래 작업 표시줄의 압축...을 사용합니다.
5. 내용을 추가, 이름 변경 또는 삭제할 때만 일반 단일 볼륨 ZIP에서 압축 파일 관리...를 선택합니다.

### 권한 및 데이터

Archive Manager는 저장소 또는 네트워크 권한을 요청하지 않습니다. 호스트는 수명이 짧은 읽기 전용 설명자와 플러그인 UID에 고정된 출력 트랜잭션만 제공해 임의 경로 선택을 막습니다. Explorer Action v11은 제한된 동기 재시도에서만 암호를 전달하고 저장하지 않습니다. Explorer Action v12가 추가하는 것은 세션에 한정된 호스트 승인 인접 볼륨 카탈로그뿐이며 불투명 ID와 파일 신원을 재검증합니다. Explorer Action v13은 같은 임시 저장 원본만 다시 인덱싱하고 완전한 대체 인덱스가 준비될 때까지 이전 상태를 유지합니다. Explorer Action v14는 `.zip.001` 및 `.7z.001` 같은 제한된 복합 접미사만 일치시키고 임의의 `.001` 파일은 일치시키지 않으며 디렉터리 또는 경로 권한 없이 v12 카탈로그를 재사용합니다. 위험한 경로는 격리되고 출력은 게시 전에 검증되며 리소스 예산 확인으로 구조 안전 검사가 비활성화되지 않습니다.

Explorer Action v15는 같은 세션에서 검증된 새 파일 출력만 최대 128개 멤버의 복구 가능 배치로 묶습니다. 호스트는 게시 전에 상위 폴더와 파일 신원을 영구 기록하고 실패나 재시작 후에도 신원이 일치하는 멤버만 정리합니다. 외부에서 변경된 파일은 보존하고 수동 복구 필요 상태로 표시합니다. 이 프로토콜은 덮어쓰기, 원본 삭제, 디렉터리 트리 또는 임의 경로 권한을 부여하지 않습니다.

### Roadmap

남은 작업은 체크 가능한 항목으로 추적합니다. 일반 ZIP 이외 형식의 쓰기 재구성, 실행 취소 또는 원본 삭제 트랜잭션, 접근성 검토, 나머지 기기와 생성 도구 매트릭스 및 첫 공개 릴리스 자료입니다.

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/ROADMAP.md)

### 릴리스 노트

#### v2.8.0

_2026/08/27_

- `참고` 이 릴리스에는 Explorer Action v15를 제공하는 대응 AutoJs6 6.8.0 빌드 (버전 코드 5276 이상) 가 필요합니다
- `추가` 표준 분할 ZIP과 항목별 개별 압축은 이제 모든 출력의 쓰기와 다시 읽기 검증을 마친 뒤 Explorer Action v15의 단일 복구 가능 배치로 게시됩니다
- `수정` 다중 출력 생성은 일반 실패 경로에서 확정된 부분 결과를 남기지 않으며, 전체 배치가 커밋된 뒤에만 Explorer를 새로 고치고 성공을 보고합니다
- `수정` Android 7에서 압축 옵션 스위치가 일반 텍스트 레이블처럼만 보이던 문제를 수정하여 올바르게 표시되고 탭할 수 있습니다
- `개선` 호스트는 게시 전에 상위 폴더와 각 임시 파일의 신원을 영구 기록하며, 실패나 재시작 시 신원이 일치하는 멤버만 롤백합니다. 외부에서 변경된 파일은 보존하고 수동 복구 필요 상태로 보고합니다

#### v2.7.0

_2026/08/27_

- `참고` 이 릴리스에는 Explorer Action v14를 제공하는 대응 AutoJs6 6.8.0 빌드 (버전 코드 5276 이상) 가 필요합니다
- `추가` 완전한 번호형 `.zip.001` 및 `.7z.001` 세트를 `.001` 볼륨에서 탐색, 미리 보기 및 추출할 수 있습니다. 번호형 세트는 읽기 전용입니다
- `추가` Explorer Action v14는 제한된 복합 파일 이름 접미사 일치를 추가하고 UID에 고정된 v12 인접 볼륨 소스를 재사용하며 임의의 `.001` 파일은 일치시키지 않습니다
- `수정` Android 7은 Zip4j 호환 경로를 사용하기 전에 호스트가 승인한 번호형 ZIP 볼륨을 하나의 비공개 로컬 파일로 결합하므로 유효한 세트를 손상된 것으로 보고하지 않습니다
- `개선` 인접 볼륨 번호는 `.002`부터 `.128`까지로 제한되고 제공된 모든 볼륨은 연속되어야 합니다. reader는 다음 누락 볼륨을 정확히 보고하고 구체화 전후에 신원을 재검증하며 내부 편집 기능을 게시하지 않습니다

#### v2.6.0

_2026/08/27_

- `참고` 이 버전에는 Explorer Action v13을 제공하는 대응 AutoJs6 6.8.0 빌드 (버전 코드 5276 이상) 가 필요합니다
- `추가` 호스트 기본 압축 파일 화면에서 관리 페이지를 열지 않고 경로 표시줄의 ZIP 파일 이름 인코딩을 선택할 수 있으며, 변경 후에도 현재 내부 폴더와 아직 존재하는 선택 항목을 유지합니다
- `추가` Explorer Action v13은 같은 읽기 전용 세션에서 임시 저장된 원본을 다시 인덱싱하고 안정적인 항목 ID로 사용 가능한 가장 깊은 경로와 아직 존재하는 항목을 복원합니다
- `개선` 대체 인덱스는 완성된 뒤에만 게시됩니다; 잘못된 선택, 스캔 실패, 진행 중인 미리 보기 또는 추출은 이전 인덱스를 유지하고 입력한 암호는 지울 수 있는 메모리에만 남습니다

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
