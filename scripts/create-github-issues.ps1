param(
    [string]$Repo,
    [string]$BacklogFile = "F:\IdeaProjects\PixelSea\github-projects-backlog.tsv",
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"

if (-not $Repo) {
    throw "Usage: .\scripts\create-github-issues.ps1 -Repo owner/repo [-BacklogFile path] [-DryRun]"
}

if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
    throw "GitHub CLI 'gh' is not installed or not in PATH."
}

if (-not (Test-Path $BacklogFile)) {
    throw "Backlog file not found: $BacklogFile"
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

$rows = Import-Csv -Delimiter "`t" -Path $BacklogFile

foreach ($row in $rows) {
    $title = $row.Title.Trim()
    $summary = $row.Summary.Trim()
    $priority = $row.Priority.Trim()
    $estimate = $row.Estimate.Trim()
    $dependencies = $row.Dependencies.Trim()
    $milestone = $row.Milestone.Trim()

    $labels = @()
    if ($row.Labels) {
        $labels = $row.Labels.Trim('"').Split(",") | ForEach-Object { $_.Trim() } | Where-Object { $_ }
    }

    $bodyLines = @(
        "## Summary",
        $summary,
        "",
        "## Planning",
        "- Priority: $priority",
        "- Estimate: $estimate",
        "- Milestone: $milestone"
    )

    if ($dependencies) {
        $bodyLines += ""
        $bodyLines += "## Dependencies"
        $dependencies.Split(";") | ForEach-Object {
            $dep = $_.Trim()
            if ($dep) {
                $bodyLines += "- $dep"
            }
        }
    }

    $body = $bodyLines -join "`n"

    $commandArgs = @(
        "issue", "create",
        "--repo", $Repo,
        "--title", $title,
        "--body", $body
    )

    foreach ($label in $labels) {
        $commandArgs += @("--label", $label)
    }

    if ($milestone) {
        $commandArgs += @("--milestone", $milestone)
    }

    Write-Host "Creating issue: $title"
    Invoke-Gh -CommandArgs $commandArgs
}
