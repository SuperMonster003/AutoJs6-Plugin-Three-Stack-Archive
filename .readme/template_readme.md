<div align="center">
  <p>
    <img src="{{ repo_url }}/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="Archive Manager" width="128" />
  </p>

  <h1>Archive Manager</h1>

  <p>{{ text_plugin_synopsis_current }}</p>

  <p>
    <a href="{{ repo_url }}/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/{{ repo_slug }}?label=Release"/></a>
    <a href="{{ repo_url }}/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/{{ repo_slug }}?color=A24232&label=Issues"/></a>
    <a href="{{ repo_url }}/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/{{ repo_slug }}?color=534BAE&label=License"/></a>
  </p>
</div>

### {{ h3_languages_with_ascii }}

{{ p_languages_all_supported_for_readme }}:

{{ placeholder_ul_languages_all_supported }}

### {{ h3_introduction }}

{{ p_introduction_current }}

### {{ h3_screenshots }}

{{ p_screenshots_current }}

<table>
  <tr>
    <td><img src="{{ repo_url }}/blob/master/docs/images/screenshots/explorer-actions.png?raw=true" alt="Archive actions in AutoJs6 Explorer" width="280" /></td>
    <td><img src="{{ repo_url }}/blob/master/docs/images/screenshots/native-archive-browsing.png?raw=true" alt="Native archive browsing" width="280" /></td>
  </tr>
  <tr>
    <td><img src="{{ repo_url }}/blob/master/docs/images/screenshots/create-archive-form.png?raw=true" alt="Archive creation form" width="280" /></td>
    <td><img src="{{ repo_url }}/blob/master/docs/images/screenshots/archive-management.png?raw=true" alt="Archive management page" width="280" /></td>
  </tr>
</table>

- {{ text_link_screenshot_notes }}: [docs/images/screenshots/README.md]({{ repo_url }}/blob/master/docs/images/screenshots/README.md)

### {{ h3_functions }}

{{ placeholder_features }}

### {{ h3_supported_formats }}

{{ p_supported_formats }}:

```text
{{ supported_formats }}
```

{{ p_creatable_formats }}:

```text
{{ creatable_formats }}
```

> {{ p_current_limits }}

### {{ h3_usage }}

{{ placeholder_usage_steps }}

### {{ h3_security }}

{{ p_security_summary }}

{{ p_security_batch }}

{{ p_security_replacement }}

### Roadmap

{{ p_roadmap_current }}

- [ROADMAP.md]({{ repo_url }}/blob/master/ROADMAP.md)

### {{ h3_release_history }}

{{ placeholder_latest_release_history }}

##### {{ h5_for_more_release_history }}

* {{ placeholder_read_more_in_changelog_md }}

### {{ h3_build }}

```powershell
.\gradlew.bat :app:assembleDebug
```

{{ text_release_build }}:

```powershell
.\gradlew.bat :app:assembleRelease
```

{{ p_build_params }}.

### {{ h3_links }}

- {{ text_link_installation }}: [docs/INSTALLATION.md]({{ repo_url }}/blob/master/docs/INSTALLATION.md)
- {{ text_link_format_capabilities }}: [docs/FORMAT_CAPABILITIES.md]({{ repo_url }}/blob/master/docs/FORMAT_CAPABILITIES.md)
- {{ text_link_security_policy }}: [SECURITY.md]({{ repo_url }}/blob/master/SECURITY.md)
- {{ text_link_autojs6_docs }}: {{ docs_autojs6_url }}
- {{ text_link_third_party_notices }}: [THIRD_PARTY_NOTICES.md]({{ repo_url }}/blob/master/THIRD_PARTY_NOTICES.md)
- {{ text_link_android_saf }}: https://developer.android.com/guide/topics/providers/document-provider
