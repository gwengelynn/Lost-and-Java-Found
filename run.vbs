' Silent launcher — double-click this (not run.bat) to open the app with no console window.
Option Explicit

Dim sh, fso, dir, bat, cmd
Set sh = CreateObject("WScript.Shell")
Set fso = CreateObject("Scripting.FileSystemObject")

dir = fso.GetParentFolderName(WScript.ScriptFullName)
bat = dir & "\run.bat"

If Not fso.FileExists(bat) Then
  MsgBox "Could not find run.bat next to this launcher." & vbCrLf & dir, vbCritical, "Lost and Java-Found"
  WScript.Quit 1
End If

' Window style 0 = hidden — no PowerShell / CMD flash
cmd = "cmd /c cd /d """ & dir & """ && call """ & bat & """ silent"
sh.Run cmd, 0, False
