param([Parameter(Mandatory=$true)][string]$AutoPtuRoot)
$ErrorActionPreference = 'Stop'
# Reproducible data import, not a runtime dependency on the Python checkout.
# No Python application code is executed; only these explicitly selected data files are read.
$sourceRoot = (Resolve-Path -LiteralPath $AutoPtuRoot).Path
$outputRoot = Join-Path $PSScriptRoot '../fabric-adapter/src/main/resources/data/autoptu/ptu'
$null = New-Item -ItemType Directory -Force -Path $outputRoot
$inputs = [ordered]@{
    'moves.json' = 'auto_ptu/data/compiled/moves.json'
    'species.json' = 'auto_ptu/data/compiled/species.json'
    'pools.json' = 'auto_ptu/data/compiled/pokedex_abilities.json'
    'ability_overrides.json' = 'auto_ptu/data/compiled/ability_overrides.json'
    'galar.json' = 'auto_ptu/data/compiled/swsh_galardex.json'
    'hisui.json' = 'auto_ptu/data/compiled/hisuidex.json'
    'sumo_references.json' = 'auto_ptu/data/compiled/sumo_references.json'
    'galar_references.json' = 'auto_ptu/data/compiled/swsh_references.json'
    'hisui_references.json' = 'auto_ptu/data/compiled/hisui_references.json'
    'evolution.json' = 'auto_ptu/data/compiled/evolution_min_levels.json'
    'galar_learnsets.json' = 'auto_ptu/data/compiled/swsh_levelup_learnsets.json'
    'hisui_learnsets.json' = 'auto_ptu/data/compiled/hisui_levelup_learnsets.json'
}
$manifest = [ordered]@{ schema = 1; source_repository = 'Teffa14/AutoPTU'; source_revision = (git -C $sourceRoot rev-parse HEAD); files = @() }
foreach ($entry in $inputs.GetEnumerator()) {
    $source = Join-Path $sourceRoot $entry.Value
    $null = Get-Content -LiteralPath $source -Raw | ConvertFrom-Json
    Copy-Item -LiteralPath $source -Destination (Join-Path $outputRoot $entry.Key)
    $manifest.files += [ordered]@{ resource = $entry.Key; source = $entry.Value; sha256 = (Get-FileHash -LiteralPath $source -Algorithm SHA256).Hash.ToLowerInvariant() }
}
$csvInputs = [ordered]@{
    'abilities.json' = 'files/Copia de Fancy PTU 1.05 Sheet - Version Hisui - Abilities Data.csv'
    'learnsets.json' = 'files/pokedex_learnset.csv'
}
foreach ($entry in $csvInputs.GetEnumerator()) {
    $source = Join-Path $sourceRoot $entry.Value
    $rows = @(Import-Csv -LiteralPath $source -Encoding UTF8)
    $json = ConvertTo-Json -InputObject $rows -Depth 30
    [IO.File]::WriteAllText((Join-Path $outputRoot $entry.Key), $json, [Text.UTF8Encoding]::new($false))
    $manifest.files += [ordered]@{ resource = $entry.Key; source = $entry.Value; source_sha256 = (Get-FileHash -LiteralPath $source).Hash.ToLowerInvariant(); sha256 = (Get-FileHash -LiteralPath (Join-Path $outputRoot $entry.Key)).Hash.ToLowerInvariant() }
}
# The runtime oracle reads this CSV before supplements. Keep its exact numeric values and effects.
$moveSource = 'files/Copia de Fancy PTU 1.05 Sheet - Version Hisui - Moves Data.csv'
$movePath = Join-Path $sourceRoot $moveSource
$moveText = [IO.File]::ReadAllText($movePath, [Text.Encoding]::UTF8)
$movePayload = ($moveText -split '\r?\n', 3)[2]
$moveHeaders = @('name','type','category','damage_base','frequency','ac','range','effects','contest','category_duplicate','type_duplicate','sheer_force','tough_claws','technician','reckless','iron_fist','mega_launcher','mega_launcher_playtest','punk_rock','strong_jaw','reckless_playtest')
$moveRows = @($movePayload | ConvertFrom-Csv -Header $moveHeaders)
[IO.File]::WriteAllText((Join-Path $outputRoot 'move_oracle.json'), (ConvertTo-Json -InputObject $moveRows -Depth 10), [Text.UTF8Encoding]::new($false))
$manifest.files += [ordered]@{ resource = 'move_oracle.json'; source = $moveSource; source_sha256 = (Get-FileHash -LiteralPath $movePath).Hash.ToLowerInvariant(); sha256 = (Get-FileHash -LiteralPath (Join-Path $outputRoot 'move_oracle.json')).Hash.ToLowerInvariant() }
[IO.File]::WriteAllText((Join-Path $outputRoot 'manifest.json'), (ConvertTo-Json -InputObject $manifest -Depth 10), [Text.UTF8Encoding]::new($false))
Write-Output "Imported $($manifest.files.Count) PTU datasets into $outputRoot"
