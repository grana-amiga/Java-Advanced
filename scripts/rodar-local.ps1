param(
    [ValidateSet('run', 'test', 'package')]
    [string]$Acao = 'run'
)

$ErrorActionPreference = 'Stop'
$raiz = Split-Path $PSScriptRoot -Parent
$javaHomeAnterior = $env:JAVA_HOME
$pathAnterior = $env:PATH
$javaOptionsAnteriores = $env:JAVA_TOOL_OPTIONS

try {
    if (-not (Get-Command javac -ErrorAction SilentlyContinue)) {
        $programas = Join-Path $env:USERPROFILE 'AppData\Local\Programs'
        $idea = Get-ChildItem -LiteralPath $programas -Directory -Filter 'IntelliJ IDEA*' -ErrorAction SilentlyContinue |
            Sort-Object Name -Descending | Select-Object -First 1
        if (-not $idea -or -not (Test-Path -LiteralPath (Join-Path $idea.FullName 'jbr\bin\javac.exe'))) {
            throw 'JDK não encontrado. Instale Java 17+ ou configure JAVA_HOME.'
        }
        $env:JAVA_HOME = Join-Path $idea.FullName 'jbr'
        $env:PATH = (Join-Path $env:JAVA_HOME 'bin') + [IO.Path]::PathSeparator + $env:PATH

        # O JBR deste Windows precisa de um caminho inexistente para usar o socket TCP local.
        $socket = Join-Path $raiz 'target\socket-unix-indisponivel'
        $env:JAVA_TOOL_OPTIONS = ($javaOptionsAnteriores + ' -Djdk.net.unixdomain.tmpdir="' + $socket + '"').Trim()
    }

    $maven = Get-Command mvn -ErrorAction SilentlyContinue
    if ($maven) {
        $mavenExe = $maven.Source
    } else {
        $cacheMaven = Join-Path $env:USERPROFILE '.m2\wrapper\dists'
        $mavenExe = Get-ChildItem -LiteralPath $cacheMaven -Recurse -File -Filter 'mvn.cmd' -ErrorAction SilentlyContinue |
            Sort-Object FullName -Descending | Select-Object -ExpandProperty FullName -First 1
        if (-not $mavenExe) {
            throw 'Maven não encontrado. Instale Maven 3.6.3+ ou configure o PATH.'
        }
    }

    Push-Location $raiz
    try {
        switch ($Acao) {
            'run' { & $mavenExe -B spring-boot:run }
            'package' {
                $target = Join-Path $raiz 'target'
                if (Test-Path -LiteralPath $target) {
                    $item = Get-Item -LiteralPath $target
                    $raizResolvida = (Resolve-Path -LiteralPath $raiz).Path.TrimEnd('\')
                    $targetResolvido = (Resolve-Path -LiteralPath $target).Path
                    if ($item.LinkType -or -not $targetResolvido.StartsWith(
                            $raizResolvida + '\', [StringComparison]::OrdinalIgnoreCase)) {
                        throw 'O diretório target não está dentro do projeto; limpeza cancelada.'
                    }
                }
                $jar = Join-Path $target 'grana-amiga-api-1.0.0.jar'
                if (Test-Path -LiteralPath $jar) {
                    Remove-Item -LiteralPath $jar -Force
                }
                & $mavenExe -B -DskipTests package
            }
            'test' {
                $cacheMockito = Join-Path $env:USERPROFILE '.m2\repository\org\mockito\mockito-core'
                $agent = Get-ChildItem -LiteralPath $cacheMockito -Recurse -File -Filter 'mockito-core-*.jar' -ErrorAction SilentlyContinue |
                    Sort-Object FullName -Descending | Select-Object -ExpandProperty FullName -First 1
                if ($agent) { & $mavenExe -B "-DargLine=-javaagent:$agent" test }
                else { & $mavenExe -B test }
            }
        }
        if ($LASTEXITCODE -ne 0) { throw "Maven terminou com código $LASTEXITCODE." }
    } finally {
        Pop-Location
    }
} finally {
    $env:JAVA_HOME = $javaHomeAnterior
    $env:PATH = $pathAnterior
    $env:JAVA_TOOL_OPTIONS = $javaOptionsAnteriores
}
