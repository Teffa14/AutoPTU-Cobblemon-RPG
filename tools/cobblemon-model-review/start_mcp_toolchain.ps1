param(
    [int]$BlockbenchPort = 3000,
    [int]$MinecraftDevPort = 3001
)

$ErrorActionPreference = "Stop"

function Test-LocalPort {
    param([int]$Port)
    try {
        $client = [System.Net.Sockets.TcpClient]::new()
        $task = $client.ConnectAsync("127.0.0.1", $Port)
        if (-not $task.Wait(1200)) {
            $client.Dispose()
            return $false
        }
        $connected = $client.Connected
        $client.Dispose()
        return $connected
    }
    catch {
        return $false
    }
}

Write-Host "Ouros Cobblemon MCP toolchain"
Write-Host "Blockbench MCP: http://127.0.0.1:$BlockbenchPort/bb-mcp"
Write-Host "Minecraft Dev MCP: http://127.0.0.1:$MinecraftDevPort/mcp"
Write-Host ""

if (Test-LocalPort -Port $BlockbenchPort) {
    Write-Host "Blockbench MCP port is reachable."
}
else {
    Write-Warning "Blockbench MCP is not reachable on port $BlockbenchPort. Open desktop Blockbench, load https://jasonjgardner.github.io/blockbench-mcp-plugin/mcp.js, and verify Settings > General > MCP Server Port."
}

if (Test-LocalPort -Port $MinecraftDevPort) {
    Write-Host "Minecraft Dev MCP is already reachable."
    exit 0
}

$npx = Get-Command npx.cmd -ErrorAction SilentlyContinue
if (-not $npx) {
    $npx = Get-Command npx -ErrorAction SilentlyContinue
}
if (-not $npx) {
    throw "npx was not found. Install Node.js 18+ before starting Minecraft Dev MCP."
}

Write-Host "Starting Minecraft Dev MCP on loopback port $MinecraftDevPort..."
$startArgs = @{
    FilePath = $npx.Source
    ArgumentList = @(
        "-y",
        "@mcdxai/minecraft-dev-mcp",
        "--http",
        "--host", "127.0.0.1",
        "--port", "$MinecraftDevPort"
    )
    PassThru = $true
}
$process = Start-Process @startArgs

$ready = $false
for ($attempt = 0; $attempt -lt 30; $attempt++) {
    Start-Sleep -Seconds 1
    if (Test-LocalPort -Port $MinecraftDevPort) {
        $ready = $true
        break
    }
    if ($process.HasExited) {
        break
    }
}

if (-not $ready) {
    throw "Minecraft Dev MCP did not become reachable on port $MinecraftDevPort. Process id: $($process.Id)"
}

Write-Host "Minecraft Dev MCP is reachable. Process id: $($process.Id)"
Write-Host "Toolchain bootstrap complete."
