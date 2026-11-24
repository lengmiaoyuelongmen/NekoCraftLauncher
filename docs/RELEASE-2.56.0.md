# Neko Craft Launcher 2.56.0

> 背包可批量附魔；首个「继续压首帧」的尝试被判无效（有数据说明）；i18n 小步推进。

## 安装包

| 文件 | 说明 |
|---|---|
| `NekoCraftLauncher-2.56.0.exe` | **安装版**：jpackage/WiX，静默参数 `/quiet` |
| `NekoCraftLauncher-2.56.0-win64.zip` | 免安装版；支持就地更新、回退与备份管理 |
| `NekoCraftLauncher-2.56.0-Features.pdf` / `.docx` | 功能预览（图文版） |
| `SHA256SUMS.txt` / `verify.txt` / `neko-codesign.cer` | 校验与签名材料 |

## 1. 背包：批量附魔

背包里每个物品行新增 **批量** 按钮：

- 填附魔 id（`unbreaking` 会自动补 `minecraft:`）与等级，一键应用到**同 id 的所有槽位**；
- 只读写一次文件、只备份一次（不是逐件循环）；
- 没有匹配物品时直接返回 0，不写文件、不报错。

实测单元测试：两个苹果槽位同时被改（`changed=2`），不匹配的 id 返回 0。

## 2. 首帧：这次尝试被判无效（如实记录）

试的是「骨架模式下关掉入场动画，省一次重绘」：

| 场景 | 首帧 |
|---|---|
| 保留入场动画 | 3583 ms |
| 关掉入场动画 | **3707 ms** |

关掉反而更慢（落在波动区间内，但没有收益）。**已回退**，保留动画，不为「看起来改了」而留下副作用。

本轮首帧实测 4184 ms（这一次编译含批量附魔代码，波动范围仍在此前的 3.6~4.6 s 区间）。
要继续压，下一刀应该切 JVM 之后的固定开销（1.25~1.4 s 的 FX 启动 + 首帧光栅化），
而不是继续微调动画这种一次性的东西。

## 3. i18n

设置页「管理」组命中 0 条（写法与预期不同），基线维持 **2178**。
同样不虚报降幅。

## 交付

- **351 项单测全绿**（新增批量附魔用例）；无头冒烟六页全过；编译 0 告警
- exe（安装包 + 便携壳）均已签名并带 DigiCert 时间戳

## 校验

```
bcd9b8fe1b79f956b1f174f71228be6e010a8f72fc8f99a983370df8b3ed7e3c  NekoCraftLauncher-2.56.0.exe
af51600e747fd7f9919843673c655f3859b5f8c45a29fb8b1acb3f6f11e174ce  NekoCraftLauncher-2.56.0-win64.zip
98717bb253355b82ac354811febef1519dabd07f6882f5d682ac77359dc7328c  NekoCraftLauncher-2.56.0-Features.pdf
cd3832aa8cbcfdd6cd609b9e0f8ef8033d04e032de7a08c89f6182bb4af564b6  NekoCraftLauncher-2.56.0-Features.docx
41bdc7658bd42ee52d59d81ccd541597b7deac9e600715ab23327227a73cdb89  neko-codesign.cer
5a563ad20846b116b9597b77a439f4813d0bc9b0c3f1c6dbb16cf29ebe71d66a  verify.txt
```
