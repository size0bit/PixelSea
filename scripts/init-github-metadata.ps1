param(
    [string]$Repo,
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"

if (-not $Repo) {
    throw "Usage: .\scripts\init-github-metadata.ps1 -Repo owner/repo [-DryRun]"
}

if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
    throw "GitHub CLI 'gh' is not installed or not in PATH."
}

function Format-GhCommand {
    param([string[]]$CommandArgs)

    $formattedArgs = $CommandArgs | ForEach-Object {
        if ($_ -match '\s') { '"' + $_ + '"' } else { $_ }
    }

    return "gh " + ($formattedArgs -join " ")
}

function Invoke-Gh {
    param(
        [string[]]$CommandArgs,
        [switch]$CaptureOutput
    )

    if ($DryRun) {
        Write-Host "DRY RUN:"
        Write-Host (Format-GhCommand -CommandArgs $CommandArgs)
        Write-Host ""
        return ""
    }

    $output = & gh @CommandArgs 2>&1
    if ($LASTEXITCODE -ne 0) {
        if ($output) {
            $output | Write-Host
        }
        throw "gh command failed: $(Format-GhCommand -CommandArgs $CommandArgs)"
    }

    if ($CaptureOutput) {
        return ($output -join "`n")
    }

    if ($output) {
        $output | Write-Host
    }
}

$labels = @(
    @{ Name = "priority:P0"; Color = "B60205"; Description = "Highest priority" },
    @{ Name = "priority:P1"; Color = "D93F0B"; Description = "High priority" },
    @{ Name = "priority:P2"; Color = "FBCA04"; Description = "Medium priority" },
    @{ Name = "type:build"; Color = "1D76DB"; Description = "Build and tooling work" },
    @{ Name = "type:fix"; Color = "D73A4A"; Description = "Bug fix or behavior correction" },
    @{ Name = "type:refactor"; Color = "5319E7"; Description = "Code or architecture refactor" },
    @{ Name = "type:perf"; Color = "0E8A16"; Description = "Performance or UX optimization" },
    @{ Name = "type:test"; Color = "006B75"; Description = "Test coverage and verification" },
    @{ Name = "type:docs"; Color = "0075CA"; Description = "Documentation and project guidance" },
    @{ Name = "area:app"; Color = "C2E0C6"; Description = "App module and navigation" },
    @{ Name = "area:build-logic"; Color = "BFD4F2"; Description = "Gradle and build logic" },
    @{ Name = "area:core-data"; Color = "F9D0C4"; Description = "Core data and media querying" },
    @{ Name = "area:gallery"; Color = "FEF2C0"; Description = "Gallery feature" },
    @{ Name = "area:viewer"; Color = "D4C5F9"; Description = "Viewer feature" }
)

$milestones = @(
    "M1.1",
    "M1.2",
    "M2.1",
    "M2.2",
    "M2.3",
    "M3"
)

$existingLabels = @{}
if (-not $DryRun) {
    $existingLabelsJson = Invoke-Gh -CommandArgs @( "label", "list", "--repo", $Repo, "--limit", "200", "--json", "name" ) -CaptureOutput
    if ($existingLabelsJson) {
        (ConvertFrom-Json $existingLabelsJson) | ForEach-Object {
            $existingLabels[$_.name] = $true
        }
    }
}

foreach ($label in $labels) {
    if ($existingLabels.ContainsKey($label.Name)) {
        Write-Host "Label exists: $($label.Name)"
        continue
    }

    Write-Host "Creating label: $($label.Name)"
    Invoke-Gh -CommandArgs @(
        "label", "create", $label.Name,
        "--repo", $Repo,
        "--color", $label.Color,
        "--description", $label.Description
    )
}

$existingMilestones = @{}
if (-not $DryRun) {
    $existingMilestonesJson = Invoke-Gh -CommandArgs @( "api", "repos/$Repo/milestones?state=all&per_page=100" ) -CaptureOutput
    if ($existingMilestonesJson) {
        (ConvertFrom-Json $existingMilestonesJson) | ForEach-Object {
            $existingMilestones[$_.title] = $true
        }
    }
}

foreach ($title in $milestones) {
    if ($existingMilestones.ContainsKey($title)) {
        Write-Host "Milestone exists: $title"
        continue
    }

    Write-Host "Creating milestone: $title"
    Invoke-Gh -CommandArgs @(
        "api",
        "--method", "POST",
        ("repos/$Repo/milestones"),
        "-f", "title=$title"
    )
}
