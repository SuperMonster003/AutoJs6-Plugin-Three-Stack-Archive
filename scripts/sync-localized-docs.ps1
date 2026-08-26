[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

$repositoryRoot = Split-Path -Parent $PSScriptRoot
$readmeRoot = Join-Path $repositoryRoot '.readme'
$changelogRoot = Join-Path $repositoryRoot '.changelog'
$changelogOutputRoot = Join-Path $repositoryRoot 'app\src\main\assets\doc'
$utf8WithoutBom = New-Object System.Text.UTF8Encoding($false)
$locales = @(
    'zh-Hans',
    'zh-Hant-HK',
    'zh-Hant-TW',
    'en',
    'fr',
    'es',
    'ja',
    'ko',
    'ru',
    'ar'
)
$releaseCategories = @('hint', 'feature', 'fix', 'improvement', 'dependency')

function Read-Json([string] $path) {
    return [System.IO.File]::ReadAllText($path, [System.Text.Encoding]::UTF8) | ConvertFrom-Json
}

function Normalize-Newlines([string] $text) {
    return $text.Replace("`r`n", "`n").Replace("`r", "`n")
}

function Write-GeneratedText([string] $path, [string] $text) {
    $normalized = (Normalize-Newlines $text).TrimEnd("`n") + "`n"
    [System.IO.File]::WriteAllText($path, $normalized.Replace("`n", "`r`n"), $utf8WithoutBom)
}

function Replace-TemplateValues([string] $template, [pscustomobject] $values) {
    $result = $template
    foreach ($property in $values.PSObject.Properties) {
        if ($null -ne $property.Value -and $property.Value -isnot [System.Array]) {
            $result = $result.Replace("{{ $($property.Name) }}", [string] $property.Value)
        }
    }
    return $result
}

function Get-ReleaseLabel([pscustomobject] $language, [string] $category) {
    return [string] $language.PSObject.Properties["changelog_label_$category"].Value
}

function Render-ReleaseHistory(
    [pscustomobject] $language,
    [string] $headingPrefix,
    [int] $limit
) {
    $sections = New-Object System.Collections.Generic.List[string]
    $releaseProperties = @($language.'$data'.PSObject.Properties)
    if ($limit -ge 0) {
        $releaseProperties = @($releaseProperties | Select-Object -First $limit)
    }

    foreach ($releaseProperty in $releaseProperties) {
        $release = $releaseProperty.Value
        $lines = New-Object System.Collections.Generic.List[string]
        $lines.Add("$headingPrefix $($releaseProperty.Name)")
        $lines.Add('')
        $lines.Add("_$($release.released_date)_")
        $lines.Add('')

        foreach ($category in $releaseCategories) {
            $categoryProperty = $release.PSObject.Properties[$category]
            if ($null -eq $categoryProperty) {
                continue
            }
            $label = Get-ReleaseLabel $language $category
            foreach ($entry in @($categoryProperty.Value)) {
                $lines.Add("- ``$label`` $entry")
            }
        }
        $sections.Add([string]::Join("`n", $lines))
    }

    return [string]::Join("`n`n", $sections)
}

$common = Read-Json (Join-Path $readmeRoot 'common.json')
$readmeTemplate = Normalize-Newlines ([System.IO.File]::ReadAllText(
    (Join-Path $readmeRoot 'template_readme.md'),
    [System.Text.Encoding]::UTF8
))
$changelogTemplate = Normalize-Newlines ([System.IO.File]::ReadAllText(
    (Join-Path $changelogRoot 'template_changelog.md'),
    [System.Text.Encoding]::UTF8
))

$readmeLanguages = @{}
$changelogLanguages = @{}
foreach ($locale in $locales) {
    $readmeLanguages[$locale] = Read-Json (Join-Path $readmeRoot "lang_$locale.json")
    $changelogLanguages[$locale] = Read-Json (Join-Path $changelogRoot "lang_$locale.json")
}

foreach ($locale in $locales) {
    $language = $readmeLanguages[$locale]
    $changelogLanguage = $changelogLanguages[$locale]
    $languageLines = foreach ($candidate in $locales) {
        $candidateLanguage = $readmeLanguages[$candidate]
        $displayName = [string] $candidateLanguage.'$name'
        if ($candidate -eq $locale) {
            "- $displayName [$candidate] # $($language.text_current_lowercase)"
        } else {
            "- [$displayName [$candidate]]($($common.repo_url)/blob/master/.readme/README-$candidate.md)"
        }
    }

    $readme = Replace-TemplateValues $readmeTemplate $common
    $readme = Replace-TemplateValues $readme $language
    $readme = $readme.Replace(
        '{{ placeholder_ul_languages_all_supported }}',
        [string]::Join("`n", $languageLines)
    )
    $readme = $readme.Replace(
        '{{ placeholder_features }}',
        [string]::Join("`n", @($language.highlights_current | ForEach-Object { "- $_" }))
    )
    $usageIndex = 0
    $usageLines = @($language.quick_start | ForEach-Object {
        $usageIndex += 1
        "$usageIndex. $_"
    })
    $readme = $readme.Replace(
        '{{ placeholder_usage_steps }}',
        [string]::Join("`n", $usageLines)
    )
    $readme = $readme.Replace(
        '{{ placeholder_latest_release_history }}',
        (Render-ReleaseHistory $changelogLanguage '####' 3)
    )
    $readme = $readme.Replace(
        '{{ placeholder_read_more_in_changelog_md }}',
        "[CHANGELOG-$locale.md]($($common.repo_url)/blob/master/app/src/main/assets/doc/CHANGELOG-$locale.md)"
    )
    if ($readme.Contains('{{')) {
        throw "Unresolved README placeholder for locale $locale"
    }
    Write-GeneratedText (Join-Path $readmeRoot "README-$locale.md") $readme

    $changelog = Replace-TemplateValues $changelogTemplate $language
    $changelog = Replace-TemplateValues $changelog $changelogLanguage
    $changelog = $changelog.Replace(
        '{{ placeholder_release_history }}',
        (Render-ReleaseHistory $changelogLanguage '##' -1)
    )
    if ($changelog.Contains('{{')) {
        throw "Unresolved CHANGELOG placeholder for locale $locale"
    }
    Write-GeneratedText (Join-Path $changelogOutputRoot "CHANGELOG-$locale.md") $changelog
}

$simplifiedReadme = [System.IO.File]::ReadAllText(
    (Join-Path $readmeRoot 'README-zh-Hans.md'),
    [System.Text.Encoding]::UTF8
)
Write-GeneratedText (Join-Path $repositoryRoot 'README.md') $simplifiedReadme

$aliases = @{
    'CHANGELOG.md' = 'CHANGELOG-zh-Hans.md'
    'CHANGELOG-zh.md' = 'CHANGELOG-zh-Hans.md'
    'CHANGELOG-zh-rHK.md' = 'CHANGELOG-zh-Hant-HK.md'
    'CHANGELOG-zh-rTW.md' = 'CHANGELOG-zh-Hant-TW.md'
}
foreach ($alias in $aliases.GetEnumerator()) {
    $source = [System.IO.File]::ReadAllText(
        (Join-Path $changelogOutputRoot $alias.Value),
        [System.Text.Encoding]::UTF8
    )
    Write-GeneratedText (Join-Path $changelogOutputRoot $alias.Key) $source
}

Write-Host "Synchronized $($locales.Count) README locales and $($locales.Count) CHANGELOG locales."
