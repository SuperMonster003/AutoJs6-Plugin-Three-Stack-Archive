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

Archive Manager는 AutoJs6 파일 관리자를 대체하지 않고 그 안에서 동작합니다. 지원되는 압축 파일은 호스트의 목록, 경로 표시줄, 테마, 뷰어, 선택 모드, 진행 UI 및 디렉터리 새로 고침을 그대로 사용합니다. 별도 관리 페이지는 상세 형식 정보와 더 풍부한 양식이 필요한 추가 설정에 사용합니다.

### 현재 기능

- ZIP/JAR/AAR/WAR, 7Z, RAR4/RAR5 및 TAR 계열을 AutoJs6 기본 목록에서 탐색하고 내부 경로 표시줄, 검색, 정렬 및 뒤로 가기를 사용할 수 있습니다.
- 이름이 지원 압축 파일과 일치하지 않으면 명시적인 압축 파일로 열기... 재검사를 제공하고 감지된 형식을 경로 표시줄에 표시합니다. TAR 복합 접미사를 전체 이름으로 정확히 일치시켜 일반 `.gz`, `.xz`, `.bz2`, `.zst` 스트림을 TAR로 표시하지 않습니다.
- 전체 압축 파일을 먼저 풀지 않고 읽을 수 있는 문서, 이미지, 오디오 및 비디오를 호스트의 기존 뷰어로 미리 볼 수 있습니다.
- 전체 압축 파일, 현재 내부 폴더 또는 선택 항목을 진행률, 취소, 안전한 충돌 이름, 게시 전 검증 및 커밋 전 롤백과 함께 추출할 수 있습니다.
- 암호화된 ZIP, 7Z 및 RAR은 처음 열거나 추출할 때 호스트 기본 암호 창을 사용하며, 잘못된 암호는 현재 경로를 잃지 않고 다시 입력할 수 있습니다.
- 호스트 경로 표시줄에서 ZIP 파일 이름 인코딩을 직접 수정할 수 있습니다. 같은 읽기 전용 세션이 인덱스를 다시 만들고 가능한 경우 현재 내부 경로와 선택을 유지합니다.
- 완전한 표준 `.z01 + .zip`, 최신 WinRAR `partN.rar`, 번호형 `.zip.001` 및 번호형 `.7z.001` 세트를 호스트가 제한해 승인한 인접 볼륨 설명자로 탐색, 미리 보기 및 추출할 수 있습니다. 누락되거나 변경된 볼륨은 명시적으로 실패합니다.
- 단일 항목 또는 같은 상위 폴더의 선택으로 ZIP, 7Z, TAR, TAR.GZ, TAR.XZ, TAR.BZ2 및 TAR.ZST를 만들 수 있습니다. ZIP은 AES-256, 표준 분할 및 항목별 개별 생성도 지원합니다.
- 표준 분할 ZIP과 항목별 개별 압축은 검증된 모든 물리 출력을 하나의 복구 가능 배치로 게시합니다. 실패나 호스트 재시작을 성공한 부분 결과로 표시하지 않습니다.
- 일반 단일 볼륨 ZIP, 안전한 일반 7Z 및 지원되는 TAR, TAR.GZ/TGZ, TAR.XZ/TXZ, TAR.BZ2/TBZ2 및 TAR.ZST/TZST는 검증된 재구성으로 파일이나 전체 폴더 트리 추가, 빈 폴더 생성, 이름 변경 및 삭제를 수행하고 완전한 재읽기 후에만 원본을 원자적으로 교체합니다.
- 압축 파일 전용 경로 표시줄 동작으로 파일이나 전체 폴더 트리의 명시적 혼합 선택을 추가하고 빈 폴더를 만들 수 있습니다. 같은 기본 목록에서 항목 이름을 바꾸거나 다중 선택을 삭제할 수도 있습니다. 기존 대화 상자, 진행률, 경로 및 선택 복원을 재사용하며 세션과 관련 항목이 허용할 때만 동작을 표시합니다.
- 쓰기 가능한 압축 파일 편집에 성공한 뒤 성공 메시지나 관리 화면 메뉴에서 이전 버전을 복원할 수 있습니다. 호스트의 제한된 보존 기간 안에 한 번만 제공되며 다른 앱이 대상을 변경하면 거부됩니다.
- 관리 화면은 실제 형식, 콘텐츠 합계, 사용 가능한 편집 동작 및 정확한 읽기 전용 이유를 한곳에 표시합니다. 각 편집은 출력 예약 전에 실제 작업량, 전체 재구성 및 메타데이터 영향을 검토하며 취소해도 보류 중인 출력을 만들지 않습니다.
- 위험한 이름은 읽기 전용으로 격리하고 쓰기 전에 구조 및 리소스 제한을 적용하며, 가능하면 호스트의 seek 가능한 설명자에서 직접 읽습니다.
- 모든 물리 출력이 검증되고 커밋된 뒤에만 전체 원본 선택을 호스트 휴지통으로 옮길 수 있습니다. 이 옵션은 기본적으로 꺼져 있으며 원본 변경이나 불완전한 출력 증명이 있으면 원본 데이터를 제거하기 전에 중지합니다.

### 현재 형식

현재 버전은 다음 탐색 및 압축 풀기 가능 확장자를 인식합니다:

```text
zip, zip.001, jar, aar, war, 7z, 7z.001, rar, tar, tar.gz, tgz, tar.xz, txz, tar.bz2, tbz2, tar.zst, tzst
```

현재 버전은 다음 형식을 만들 수 있습니다:

```text
zip, 7z, tar, tar.gz, tar.xz, tar.bz2, tar.zst
```

> 기본 통합에는 Explorer Action v20을 제공하는 대응 AutoJs6 6.8.0 빌드 (버전 코드 5276 이상) 가 필요합니다. RAR 및 분할 압축 파일은 읽기 전용입니다. 일반 단일 볼륨 ZIP, 비암호화 비 solid 단일 볼륨이며 디코더 예산 안에 있는 7Z, 안전한 일반 파일과 디렉터리만 포함한 TAR 계열 압축 파일을 편집할 수 있습니다. 암호화, solid, 분할, 위험 경로, 미지원 방식 또는 예산 초과 7Z는 읽기 전용입니다. 표준 분할 ZIP은 최종 `.zip`에서, 최신 WinRAR 세트는 첫 `partN.rar`에서, 번호형 ZIP 또는 7Z는 `.001` 볼륨에서 열고 필요한 모든 볼륨을 같은 폴더에 두어야 합니다. 생성 시 파일 이름 암호화 및 JAR/AAR/WAR, RAR 내부 편집은 계속 지원하지 않습니다.

### 사용법

1. Archive Manager를 설치하고 AutoJs6 플러그인 센터에서 활성화합니다.
2. 지원 파일의 기본 동작을 누르거나 압축 파일 열기를 선택합니다. 이름이 인식되지 않으면 파일 메뉴에서 압축 파일로 열기...를 선택합니다. 일반 폴더처럼 탐색할 수 있으며 이름과 형식이 다르면 경로 표시줄에 감지 결과가 표시됩니다.
3. 경로 표시줄의 추출 동작으로 현재 내부 폴더를 추출하거나 파일 이름 인코딩 동작으로 ZIP 이름을 수정합니다. 길게 눌러 선택 항목을 추출하거나 파일 메뉴의 압축 해제 위치...로 전체 파일을 추출합니다. 필요하면 암호 창이 표시됩니다.
4. 파일이나 폴더에서 압축...을 선택하거나 같은 디렉터리의 여러 항목을 선택해 아래 작업 표시줄의 압축...을 사용합니다. 성공 후 원본을 정리하려면 기본적으로 꺼진 압축 후 원본 항목을 휴지통으로 이동 옵션을 명시적으로 켭니다.
5. 쓰기 가능한 기본 압축 파일 페이지에서는 경로 표시줄의 파일 또는 폴더 추가와 새 폴더를 사용하고, 항목의 직접 이름 변경이나 다중 선택 삭제도 할 수 있습니다. 상세 형식 정보와 추가 설정에는 압축 파일 관리...를 선택합니다.

### 권한 및 데이터

Archive Manager는 저장소 또는 네트워크 권한을 요청하지 않습니다. 호스트는 수명이 짧은 읽기 전용 설명자와 플러그인 UID에 고정된 출력 트랜잭션만 제공해 임의 경로 선택을 막습니다. Explorer Action v11은 제한된 동기 재시도에서만 암호를 전달하고 저장하지 않습니다. Explorer Action v12가 추가하는 것은 세션에 한정된 호스트 승인 인접 볼륨 카탈로그뿐이며 불투명 ID와 파일 신원을 재검증합니다. Explorer Action v13은 같은 임시 저장 원본만 다시 인덱싱하고 완전한 대체 인덱스가 준비될 때까지 이전 상태를 유지합니다. Explorer Action v14는 `.zip.001` 및 `.7z.001` 같은 제한된 복합 접미사만 일치시키고 임의의 `.001` 파일은 일치시키지 않으며 디렉터리 또는 경로 권한 없이 v12 카탈로그를 재사용합니다. Explorer Action v17은 일반 기본 동작이 일치하지 않을 때 일치 조건 없는 읽기 전용 보조 동작만 추가합니다. 사용자가 누른 뒤 기존 압축 파일 세션을 한 번 재사용하며 경로, 디렉터리 또는 쓰기 권한을 추가하지 않습니다. 위험한 경로는 격리되고 출력은 게시 전에 검증되며 리소스 예산 확인으로 구조 안전 검사가 비활성화되지 않습니다.

Explorer Action v15는 같은 세션에서 검증된 새 출력만 최대 128개 멤버의 복구 가능 배치로 묶습니다. Explorer Action v16은 플러그인이 원래의 전체 선택을 순서대로 제출하고 커밋된 모든 출력 트랜잭션을 정확히 증명한 경우에만 호스트가 원본과 출력을 다시 검증해 원본을 휴지통으로 옮깁니다. 호스트는 원본 데이터를 제거하기 전에 복구 사본을 동기화하고 기록을 영구 저장합니다. 플러그인에는 임의 경로나 직접 삭제 권한이 없습니다. Binder 응답이 유실되면 이동을 재시도하지 않고 같은 멱등 종단 결과를 조회합니다.

Explorer Action v18은 이전 압축 파일을 호스트 비공개 영구 저장소에만 보관하고 백업 경로가 아닌 불투명 기록 ID를 반환합니다. 상위 폴더와 대상이 정확한 커밋 교체 결과와 일치할 때 한 번만 복원할 수 있습니다. 외부 변경이 있으면 기록이 무효화되며, 중단된 복구 증거는 호스트가 해결할 때까지 보존되고 같은 대상의 다음 교체를 막습니다. 일반 기록은 기간, 개수, 총 바이트 및 여유 공간 제한을 받으며 v8-v17 세션은 교체 백업을 만들지 않습니다. Explorer Action v19는 허용된 항목의 삭제 또는 이름 변경에 필요한 불투명 ID와 안전한 마지막 이름만 전달합니다. Explorer Action v20은 명시적으로 선택한 입력 루트에만 고정 권한을 추가합니다. 플러그인은 제한된 불투명 노드, 메타데이터 및 일회용 읽기 전용 디스크립터만 받고 원본 경로, URI, 선택하지 않은 형제 항목 또는 일반 저장소 접근은 받지 않습니다. 호스트는 커밋 전에 전체 스냅샷을 다시 검증하며 변경되거나 실패한 입력이 하나라도 있으면 전체 압축 파일 교체를 중단합니다.

### Roadmap

호스트 기본 생성, 추가, 삭제 및 이름 변경은 일반 파일 관리자 레이아웃을 바꾸지 않고 완료되었습니다. 남은 체크 항목은 휴지통 그룹 실행 취소와 기록, 나머지 기기 매트릭스 및 첫 공개 릴리스 자료입니다.

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/ROADMAP.md)

### 릴리스 노트

#### v2.21.0

_2026/08/30_

- `참고` 이 릴리스에는 Explorer Action v21을 포함한 AutoJs6 6.8.0 빌드 (버전 코드 5276 이상) 가 필요합니다
- `추가` 압축 파일 생성 후 완료 페이지와 원본 복원 기록을 유지합니다. 사용자가 명시적으로 휴지통으로 보낸 원본은 Activity 또는 호스트 세션을 다시 만든 뒤에도 복원할 수 있습니다
- `수정` 원본 복원은 기존 이름을 덮어쓰거나 생성된 압축 파일을 삭제하지 않습니다. 충돌과 부분 또는 중단된 복원은 복구 증거를 유지하고 항목별 결과를 보고합니다
- `개선` 제한된 영구 기록은 호스트가 소유하며 불투명 메타데이터만 공개합니다. 복원 UI는 플러그인 압축 페이지 안에만 있고 일반 파일 관리자 레이아웃은 변경하지 않습니다

#### v2.20.0

_2026/08/30_

- `참고` 이 릴리스에는 Explorer Action v20을 포함한 AutoJs6 6.8.0 빌드 (버전 코드 5276 이상) 가 필요합니다
- `추가` 쓰기 가능한 네이티브 압축 파일 페이지에서 빈 폴더를 만들고 파일과 전체 폴더 트리의 명시적 혼합 선택을 추가할 수 있습니다. 빈 폴더를 보존하고 이름이 같은 루트 폴더는 안전하게 번호를 붙입니다
- `수정` 커밋 전에 고정된 입력 스냅샷을 다시 검증합니다. 취소, 원본 변경, 선택한 트리 안의 동등 이름 모호성 또는 직접 파일 충돌이 있으면 전체 추가가 실패하고 원본 압축 파일을 보존합니다
- `개선` Explorer Action v20은 선택한 입력에 대해 제한된 불투명 노드, 메타데이터 및 일회용 읽기 전용 디스크립터만 공개합니다. 경로, URI, 선택하지 않은 형제 항목 또는 일반 저장소 접근을 허용하지 않으며 일반 파일 관리자 레이아웃도 변경하지 않습니다

#### v2.19.0

_2026/08/29_

- `참고` 이 릴리스에는 Explorer Action v19를 제공하는 대응 AutoJs6 6.8.0 빌드 (버전 코드 5276 이상) 가 필요합니다
- `추가` 쓰기 가능한 압축 파일의 파일과 폴더를 AutoJs6 기본 압축 파일 목록에서 바로 이름 변경하거나 삭제할 수 있으며 다중 선택 삭제도 지원합니다. 대화 상자, 진행률, 경로 및 선택 복원은 기존 호스트 프레임워크를 재사용합니다
- `수정` 삭제와 이름 변경은 이제 세션 및 항목별 기능을 모두 확인합니다. 위험 경로, 누락 볼륨, RAR, 분할 압축 파일 및 기타 읽기 전용 변형은 사용할 수 없는 변경 동작을 표시하지 않습니다
- `개선` Explorer Action v19는 제한된 불투명 항목 ID와 안전한 마지막 이름만 전달하고 호스트 소유 보류 출력에서 재구성, 전체 재읽기, 원자적 교체 및 현재 위치 재인덱싱을 수행합니다. 일반 파일 관리자 레이아웃과 시각 스타일은 변경하지 않았습니다

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
