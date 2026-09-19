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

### 스크린샷

실제 Android 화면에서 호스트 메뉴 연동, 네이티브 압축 파일 탐색, 압축 파일 만들기 및 상세 관리를 보여 줍니다. 공개 가능한 합성 데이터만 사용했습니다.

<table>
  <tr>
    <td><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/docs/images/screenshots/explorer-actions.png?raw=true" alt="Archive actions in AutoJs6 Explorer" width="280" /></td>
    <td><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/docs/images/screenshots/native-archive-browsing.png?raw=true" alt="Native archive browsing" width="280" /></td>
  </tr>
  <tr>
    <td><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/docs/images/screenshots/create-archive-form.png?raw=true" alt="Archive creation form" width="280" /></td>
    <td><img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/docs/images/screenshots/archive-management.png?raw=true" alt="Archive management page" width="280" /></td>
  </tr>
</table>

- 촬영 정보와 전체 스크린샷: [docs/images/screenshots/README.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/docs/images/screenshots/README.md)

### 현재 기능

- ZIP, 7Z 및 TAR 계열 압축 파일을 AutoJs6 네이티브 파일 목록에서 직접 열고 호스트 테마, 다크 모드 및 동적 색상 사용.
- 경로 표시줄에 외부 폴더, 압축 파일 이름 및 내부 폴더 표시. 계층을 눌러 이동하고 뒤로 가기로 내부 상위 폴더를 거친 뒤 압축 파일 종료.
- 호스트 네이티브 화면을 떠나지 않고 경로 표시줄에서 현재 내부 폴더의 압축을 풀거나 선택 모드에서 표시한 파일과 폴더의 압축을 풀 수 있음. 진행률을 표시하고 취소할 수 있으며 완료 후 상위 폴더를 자동 새로 고침.
- 지원되는 문서, 이미지, 오디오 및 비디오 항목을 호스트의 기존 뷰어로 미리 보기.
- ‘압축 풀기...’로 전체 압축 파일을 권장되는 같은 디렉터리의 폴더에 풀거나 Android 시스템 선택기로 다른 폴더를 선택합니다. 이미 있는 동등한 폴더 이름은 안전하게 번호가 붙습니다.
- ‘압축 파일 관리...’로 관리 화면을 열어 전체 압축 파일, 현재 내부 폴더 또는 현재 체크한 항목을 풀거나 일반 단일 볼륨 ZIP이나 지원되는 TAR, TAR.GZ/TGZ, TAR.XZ/TXZ, TAR.BZ2/TBZ2 또는 TAR.ZST/TZST를 ‘파일 추가...’, ‘폴더 추가...’, ‘새 폴더...’, ‘이름 바꾸기...’ 및 ‘삭제’로 편집할 수 있으며 ‘압축 풀기...’는 전체 압축 파일 바로 가기로 유지됩니다.
- ZIP과 TAR 변경은 쓰기 전에 계획되고 호스트 소유 보류 출력에 다시 만들어진 뒤 완전히 재읽혀 검증 후에만 원본을 원자적으로 교체합니다. 취소나 실패는 원본을 변경하지 않으며 확정 성공 후 Explorer가 자동 새로 고침됩니다.
- 동등한 출력 이름에는 매번 확인, 건너뛰기, 덮어쓰기 또는 자동 이름 변경을 선택할 수 있습니다. 모두 적용은 남은 호환 충돌을 처리하며 기존 출력 폴더에는 항상 번호를 붙여 보존합니다.
- 압축 파일 폴더 탐색, 검색 및 정렬.
- 모든 항목을 미리 풀지 않고 디렉터리 메타데이터로 목록 생성.
- 일반 압축 파일은 호스트의 읽기 전용 seek 가능 디스크립터와 독립 위치 읽기 채널에서 전체 복사 없이 직접 탐색. 파이프, seek 불가 또는 쓰기 가능한 입력, Android 7, 프로세스가 읽을 수 있는 로컬 파일이 필요한 reader(현재 암호화 ZIP)만 비공개 캐시로 대체하고 종료할 때 삭제.
- 호환, 엄격 또는 사용자 지정 압축 해제 리소스 예산 선택. 항목 수, 경로, 출력 크기 또는 압축률 예산을 넘은 압축 파일도 탐색할 수 있고 쓰기 전에 예상 공간과 위험을 표시한 뒤 한 번 확인합니다.
- 압축 해제 중 항목 및 바이트 진행률, 현재 항목, 전송 속도와 예상 남은 시간을 표시합니다. 취소 또는 실패 시 새 출력 루트를 되돌리고, 공급자가 삭제를 거부한 잔여 위치는 이름과 URI로 알립니다.
- 상위 경로 이동, 절대 경로, 드라이브 접두사 또는 제어 문자가 포함된 이름을 경로 표시줄에 보이는 안전하지 않은 경로 폴더로 격리. 읽을 수 있는 데이터는 계속 미리 볼 수 있고 전체 압축 해제 시 일반 항목에 영향을 주지 않으면서 해당 항목을 명시적으로 건너뛰도록 요구.
- 비압축 TAR를 탐색, 미리 보기 및 압축 풀기. 심볼릭 링크, 하드 링크, 장치 노드 및 희소 항목은 목록에만 표시하고 일반 파일로 쓰지 않음.
- TAR.GZ/TGZ, TAR.XZ/TXZ, TAR.BZ2/TBZ2 및 TAR.ZST/TZST를 동일한 내부 경로, 특수 항목 격리 및 무결성 검사로 탐색, 미리 보기 및 압축 풀기.
- 일반 또는 solid 7Z를 탐색, 미리 보기 및 압축 풀기. 일반 압축/필터 체인과 콘텐츠 및 헤더 암호화를 지원하며 비밀번호 누락이나 오류를 명확히 진단.
- 실제 ZIP/7Z/TAR 구조를 확인하고 미리 보기, 압축 풀기 및 만들기 기능 판정을 통합하며 지원되지 않는 옵션은 비활성 상태로 유지.
- Zip64, 자동 압축 풀기 형식의 앞부분 데이터, 기존 이름 인코딩, Windows 경로 구분자 지원.
- 호스트가 제한적으로 승인한 같은 폴더의 설명자를 통해 완전한 표준 `.z01 + .zip` 세트를 탐색, 미리보기 및 압축 해제. 일반 프리셋 또는 사용자 지정 MiB 크기로 표준 분할 ZIP을 만들고 누락되거나 변경된 볼륨을 명확히 보고합니다.
- ZipCrypto 또는 AES로 보호된 ZIP을 탐색하고 압축 해제하며, 잘못된 비밀번호를 현재 화면에서 다시 입력할 수 있습니다. 만들 때 파일 이름이 보이는 AES-256 비밀번호를 선택적으로 설정할 수 있으며 암호화 ZIP을 만들려면 같은 비밀번호를 한 번 더 입력해야 합니다.
- ZIP 파일 이름 자동 감지가 잘못된 경우 수동으로 재정의하고 탐색과 압축 풀기에서 같은 선택을 재사용.
- 실패 시 형식, 처리 단계, 안정적인 코드와 명확한 이유를 표시하며 디버그 빌드에서는 전체 진단 정보를 복사 가능.
- 일반 파일, 폴더 및 같은 상위 폴더의 다중 선택에 ‘압축...’ 동작 제공.
- 같은 상위 폴더에서 여러 항목을 선택했을 때 항목마다 별도의 압축 파일을 만듭니다. 양식에서 출력 수와 파생 이름을 미리 보고, 기존 이름이나 중복 이름에는 덮어쓰지 않고 자동으로 번호를 붙입니다. 각 출력은 독립적으로 확정되며, 중간 취소나 실패 시 완료된 출력을 유지하고 보고하면서 모호한 전체 일괄 재시도를 차단합니다.
- 일반 또는 표준 분할 ZIP, 7Z, TAR, TAR.GZ, TAR.XZ, TAR.BZ2 및 TAR.ZST를 만들고 선택한 형식이 실제 지원하는 압축 수준과 비밀번호 옵션만 표시.
- 같은 폴더의 임시 파일에 쓴 뒤 원자적으로 확정하고 자동 번호 매기기 또는 정확한 이름을 먼저 시도한 뒤 번호를 붙여 재시도하기 전 확인을 선택. 기존 파일은 덮어쓰지 않습니다. 이름을 예약한 뒤 임시 출력을 열기 전에 한도가 있는 원본 스냅샷을 검사하며, 화면에는 검사, 압축, 검증, 확정 단계와 전체 파일 수, 읽은 바이트, 크기를 알 수 없는 파일 수를 표시합니다. 게시 전에 숨겨진 출력을 모두 다시 읽어 형식, 항목, 크기, CRC 및 콘텐츠 지문을 확인합니다. 만들기나 검증에 실패하면 트랜잭션을 중단하며, 호스트가 임시 출력 정리를 확인하지 못하면 예정 경로를 표시하고 재시도를 막습니다.

### 현재 형식

현재 버전은 다음 탐색 및 압축 풀기 가능 확장자를 인식합니다:

```text
zip, zip.001, jar, aar, war, 7z, 7z.001, rar, tar, tar.gz, tgz, tar.xz, txz, tar.bz2, tbz2, tar.zst, tzst
```

현재 버전은 다음 형식을 만들 수 있습니다:

```text
zip, 7z, tar, tar.gz, tar.xz, tar.bz2, tar.zst
```

> 기본 통합에는 Explorer Action v21을 제공하는 대응 AutoJs6 6.8.0 빌드 (버전 코드 5276 이상) 가 필요합니다. RAR 및 분할 압축 파일은 읽기 전용입니다. 일반 단일 볼륨 ZIP, 비암호화 비 solid 단일 볼륨이며 디코더 예산 안에 있는 7Z, 안전한 일반 파일과 디렉터리만 포함한 TAR 계열 압축 파일을 편집할 수 있습니다. 암호화, solid, 분할, 위험 경로, 미지원 방식 또는 예산 초과 7Z는 읽기 전용입니다. 표준 분할 ZIP은 최종 `.zip`에서, 최신 WinRAR 세트는 첫 `partN.rar`에서, 번호형 ZIP 또는 7Z는 `.001` 볼륨에서 열고 필요한 모든 볼륨을 같은 폴더에 두어야 합니다. 생성 시 파일 이름 암호화 및 JAR/AAR/WAR, RAR 내부 편집은 계속 지원하지 않습니다.

### 사용법

1. 플러그인을 설치하고 AutoJs6 플러그인 센터에서 활성화합니다.
2. ZIP, JAR, AAR, WAR, 7Z 또는 TAR 계열 압축 파일 메뉴를 엽니다.
3. ‘압축 파일 열기’를 선택한 뒤 호스트 파일 목록에서 폴더를 열거나 검색하고 경로 표시줄로 이동합니다.
4. 전체 압축 파일을 풀려면 파일 메뉴에서 ‘압축 풀기...’를 선택합니다. 권장되는 현재 폴더를 사용하거나 Android 시스템 선택기로 다른 폴더를 선택한 뒤 정확한 출력 경로를 확인합니다.
5. 현재 내부 폴더의 압축을 풀려면 경로 표시줄 오른쪽의 압축 풀기 버튼을 누릅니다. 특정 항목은 길게 눌러 선택 모드에 진입한 뒤 파일이나 폴더를 선택하고 하단 표시줄의 ‘압축 풀기’를 누릅니다. 암호, 인코딩 수정, 위험한 경로 확인, 충돌 정책 또는 다른 대상 폴더가 필요하면 ‘압축 파일 관리...’ 또는 ‘압축 풀기...’를 사용합니다.
6. 일반 단일 볼륨 ZIP이나 지원되는 TAR, TAR.GZ/TGZ, TAR.XZ/TXZ, TAR.BZ2/TBZ2 또는 TAR.ZST/TZST를 편집하려면 ‘압축 파일 관리...’를 선택하고 ‘파일 추가...’, 전체 폴더 트리를 가져오는 ‘폴더 추가...’, 빈 폴더를 만드는 ‘새 폴더...’, ‘이름 바꾸기...’ 또는 ‘삭제’를 사용합니다. 다시 만들기, 검증 및 성공 메시지가 끝날 때까지 화면을 벗어나지 마세요.
7. 관리 화면에서 압축을 풀기 전에 동등한 출력 이름의 처리 방법을 선택합니다. 매번 확인에서는 건너뛰기, 덮어쓰기 또는 자동 이름 변경 결정을 남은 모든 호환 충돌에 적용할 수 있습니다.
8. 압축 파일을 만들려면 일반 파일 또는 폴더 메뉴에서 ‘압축...’을 선택하거나 같은 폴더의 여러 항목을 선택한 뒤 하단 표시줄의 ‘압축...’을 사용합니다. 항목마다 별도로 만들려면 ‘각 항목을 개별적으로 압축’을 켜고 출력 미리보기를 확인한 다음 만드세요. 이 모드는 이름 충돌을 항상 안전한 자동 번호 지정으로 처리합니다. ZIP에서는 분할 없음, 일반 MiB 프리셋 또는 1~4096 MiB의 정수 사용자 지정 값을 선택할 수 있습니다. 출력이 해당 크기를 초과하면 `.z01`, `.z02`, ... 번호 볼륨과 마지막 `.zip`으로 구성되며, 더 작으면 하나의 `.zip` 파일로 유지됩니다.

### 권한 및 데이터

Archive Manager는 저장소 또는 네트워크 권한을 요청하지 않습니다. 호스트는 수명이 짧은 읽기 전용 설명자와 플러그인 UID에 고정된 출력 트랜잭션만 제공해 임의 경로 선택을 막습니다. Explorer Action v11은 제한된 동기 재시도에서만 암호를 전달하고 저장하지 않습니다. Explorer Action v12가 추가하는 것은 세션에 한정된 호스트 승인 인접 볼륨 카탈로그뿐이며 불투명 ID와 파일 신원을 재검증합니다. Explorer Action v13은 같은 임시 저장 원본만 다시 인덱싱하고 완전한 대체 인덱스가 준비될 때까지 이전 상태를 유지합니다. Explorer Action v14는 `.zip.001` 및 `.7z.001` 같은 제한된 복합 접미사만 일치시키고 임의의 `.001` 파일은 일치시키지 않으며 디렉터리 또는 경로 권한 없이 v12 카탈로그를 재사용합니다. Explorer Action v17은 일반 기본 동작이 일치하지 않을 때 일치 조건 없는 읽기 전용 보조 동작만 추가합니다. 사용자가 누른 뒤 기존 압축 파일 세션을 한 번 재사용하며 경로, 디렉터리 또는 쓰기 권한을 추가하지 않습니다. 위험한 경로는 격리되고 출력은 게시 전에 검증되며 리소스 예산 확인으로 구조 안전 검사가 비활성화되지 않습니다.

Explorer Action v15는 같은 세션에서 검증된 새 출력만 최대 128개 멤버의 복구 가능 배치로 묶습니다. Explorer Action v16은 플러그인이 원래의 전체 선택을 순서대로 제출하고 커밋된 모든 출력 트랜잭션을 정확히 증명한 경우에만 호스트가 원본과 출력을 다시 검증해 원본을 휴지통으로 옮깁니다. 호스트는 원본 데이터를 제거하기 전에 복구 사본을 동기화하고 기록을 영구 저장합니다. 플러그인에는 임의 경로나 직접 삭제 권한이 없습니다. Binder 응답이 유실되면 이동을 재시도하지 않고 같은 멱등 종단 결과를 조회합니다.

Explorer Action v18은 이전 압축 파일을 호스트 비공개 영구 저장소에만 보관하고 백업 경로가 아닌 불투명 기록 ID를 반환합니다. 상위 폴더와 대상이 정확한 커밋 교체 결과와 일치할 때 한 번만 복원할 수 있습니다. 외부 변경이 있으면 기록이 무효화되며, 중단된 복구 증거는 호스트가 해결할 때까지 보존되고 같은 대상의 다음 교체를 막습니다. 일반 기록은 기간, 개수, 총 바이트 및 여유 공간 제한을 받으며 v8-v17 세션은 교체 백업을 만들지 않습니다. Explorer Action v19는 허용된 항목의 삭제 또는 이름 변경에 필요한 불투명 ID와 안전한 마지막 이름만 전달합니다. Explorer Action v20은 명시적으로 선택한 입력 루트에만 고정 권한을 추가합니다. 플러그인은 제한된 불투명 노드, 메타데이터 및 일회용 읽기 전용 디스크립터만 받고 원본 경로, URI, 선택하지 않은 형제 항목 또는 일반 저장소 접근은 받지 않습니다. 호스트는 커밋 전에 전체 스냅샷을 다시 검증하며 변경되거나 실패한 입력이 하나라도 있으면 전체 압축 파일 교체를 중단합니다. Explorer Action v21은 Activity 또는 호스트 세션을 다시 만든 뒤에도 호스트 소유의 제한된 원본 복구 배치를 유지합니다. 원본 복원은 사용 중인 이름을 덮어쓰거나 만든 압축 파일을 삭제하지 않습니다.

### Roadmap

기본 탐색, 풀기, 만들기, 압축 파일 변경, 이전 버전 복원 및 영구 원본 복구는 일반 파일 관리자 레이아웃을 바꾸지 않고 완료되었습니다. Roadmap의 미체크 항목은 향후 선택적 프로토콜, 백엔드 또는 경계 사례 개선이며 현재 기능이 아닙니다.

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/ROADMAP.md)

### 릴리스 노트

#### v2.22.3

_2026/09/19_

- `수정` 공유 빌드 플러그인 1.8.3을 통해 AGP 9.1의 SDK XML v4 파싱 경고 및 JVM 단위 테스트 조립 작업에서 APK 네이티브 라이브러리 정렬 검사가 잘못 실행되는 문제 해결
- `개선` compileSdk 에 이어 targetSdk 를 37 (Android 17) 로 올리며, 플러그인 동작은 새 대상 버전의 영향을 받지 않음

#### v2.22.2

_2026/09/13_

- `수정` 플러그인 센터의 버전과 ABI 정보가 설치된 APK와 일치
- `수정` 버전 날짜를 일관된 영어 형식으로 표시
- `개선` 다운로드 파일 생성 전에 릴리스 APK의 버전, 서명 및 전체 변형 구성을 검증

#### v2.22.1

_2026/09/11_

- `개선` 64비트 네이티브 라이브러리의 16 KB 페이지 정렬을 빌드 시 검증, manifest 계약 검사 및 JSON 보고서 지원

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

- 설치 안내: [docs/INSTALLATION.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/docs/INSTALLATION.md)
- 형식별 기능표: [docs/FORMAT_CAPABILITIES.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/docs/FORMAT_CAPABILITIES.md)
- 보안 정책: [SECURITY.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/SECURITY.md)
- AutoJs6 문서: https://docs.autojs6.com
- 타사 소프트웨어 고지: [THIRD_PARTY_NOTICES.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/THIRD_PARTY_NOTICES.md)
- Android Storage Access Framework: https://developer.android.com/guide/topics/providers/document-provider


[16 KB page alignment and build verification](https://github.com/SuperMonster003/AutoJs6-Plugin-Archive-Manager/blob/master/docs/16kb.md)
