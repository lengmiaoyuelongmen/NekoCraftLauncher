# 发布产物的代码签名

> 结论先说：**签了，但用的是自签名证书**。签名可校验、可追溯；要消掉 Windows 的
> SmartScreen 提示，需要购买 CA 证书后把同一个脚本换成商业 pfx。

## 1. 一键签名

```powershell
# 先构建产物（release\installer\*.exe、release\exe\NekoCraftLauncher\）
.\scripts\sign-release.ps1 -Version 2.10.0
```

脚本做的事：

| 步骤 | 内容 |
|---|---|
| 1 | 没有证书时用 `New-SelfSignedCertificate -Type CodeSigningCert` 生成 5 年期证书，导出 `release/signing/neko-codesign.pfx` 与公钥 `.cer` |
| 2 | `signtool sign /fd SHA256 /tr <时间戳>` 签安装包与免安装版 exe；时间戳失败会自动退回不带时间戳重签 |
| 3 | `jarsigner` 签 app-image 里的 `mc-launcher.jar`（keystore 在 `release/signing/neko-jar.jks`） |
| 4 | 重新打 `NekoCraftLauncher-<版本>-win64.zip`，保证 zip 里是签名后的文件 |
| 5 | `signtool verify /pa` + `jarsigner -verify` 校验，结果写到 `release/signing/verify.txt` |

换成商业证书：

```powershell
.\scripts\sign-release.ps1 -Version 2.10.0 -PfxPath D:\codesign.pfx -PfxPassword ***
```

## 2. 校验结果怎么看

`release/signing/verify.txt` 里每个 exe 有 5 行：

```
EXE  NekoCraftLauncher-2.10.0.exe
     Status      : UnknownError / UntrustedRoot        ← 自签名的必然结果
     Signer      : CN=Neko Craft Launcher (self-signed)
     Thumbprint  : <证书指纹>
     TimeStamper : CN=DigiCert Timestamp 2024
     signtool    : Signing Certificate Chain: Issued to: Neko Craft Launcher (self-signed) …
```

- **签名存在**：`SignerCertificate` / `TimeStamperCertificate` 都有值，说明 exe 被签过且带时间戳；
- **信任链未建立**：`Status` 是 `UntrustedRoot` —— 因为根证书不在系统信任库里，不是签名损坏。

用户侧可自行解除提示（可选）：

```powershell
Import-Certificate -FilePath release\signing\neko-codesign.cer -CertStoreLocation Cert:\CurrentUser\Root
```

或：右键 exe → 属性 → 数字签名 → 详细信息 → 安装证书 → 放入「受信任的根证书颁发机构」。

## 3. 为什么先用自签名

- 公开 CA 的代码签名证书要实名审核 + 年费，不是代码问题；
- 自签名已经把「哪个构建、什么时候签的、内容有没有被改」这件事固化了：
  任何拿到 exe 的人都能用 `signtool verify` / `jarsigner -verify` 看出签名是否被改动；
- 脚本与流程完全一致，换成商业证书只需要换 pfx 路径，不需要重写发布步骤。

## 4. 已知边界

- 自签名**不会**消除 SmartScreen「未知发布者」提示，这是信任链问题，不是签名缺失；
- `jarsigner` 签名会让 jar 内容变化（新增 `META-INF/*.SF/*.RSA`），所以必须在 jpackage 之前或之后
  统一处理一次，不能混签两次；
- 时间戳依赖网络（`timestamp.digicert.com`），离线构建时用 `-SkipTimestamp`，签名依旧有效但不带时间戳。
