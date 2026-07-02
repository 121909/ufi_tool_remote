# Android 签名配置指南

本仓库已配置好 Android 应用签名，用于在 GitHub Actions 中自动构建和发布 Release。

## 本地签名配置

已创建的文件：
- `signing/ufi-tool-remote-release.jks` - 签名密钥库文件
- `keystore.properties` - 本地签名配置文件

签名信息：
- **Keystore 路径**: `signing/ufi-tool-remote-release.jks`
- **Keystore 密码**: `android123`
- **Key Alias**: `ufi-tool-remote-release`
- **Key 密码**: `android123`

## GitHub Actions 配置

要在 GitHub Actions 中启用自动签名和发布，需要配置以下 Secrets：

### 1. 进入仓库设置
访问：`Settings` → `Secrets and variables` → `Actions`

### 2. 添加以下 Secrets

#### ANDROID_KEYSTORE_BASE64
Keystore 文件的 Base64 编码内容：
```
MIILBgIBAzCCCrAGCSqGSIb3DQEHAaCCCqEEggqdMIIKmTCCBdAGCSqGSIb3DQEHAaCCBcEEggW9MIIFuTCCBbUGCyqGSIb3DQEMCgECoIIFQDCCBTwwZgYJKoZIhvcNAQUNMFkwOAYJKoZIhvcNAQUMMCsEFAtQpMACL5NUbXAfrrGSLoZQxqO9AgInEAIBIDAMBggqhkiG9w0CCQUAMB0GCWCGSAFlAwQBKgQQBeeFk3vyL6QqA71RtSGTQQSCBNA3GP0GJULG5Iq3d97vtqW3Z3cewvNx7ik8X94RTkQQ3CCT4FB4vfMRSElEKvrlyXAIavL3dlNor3Tn0h8Mm+Pn3eGci7LEa58HzgkymaIOsPkElinDQHAgiLZTMU9C4Dqv4hegwReh1Q5SLF15qC3higg/Cmo5HHXQsUr9SdhZYtiKbXGjZeDted6TkcakA9UDVDqCwGR2514PeM0FudWvWuyO4ZBoqsKZrd+d/TvN1z3BqAtsD9bmhTSqavtQkQ/50TwVRNDT//HQq8ctYcq11ZIiCG39/L25KRJiz95krOm6+DbpcFJFkQNslFhGtwJ3ac3WwtH5iI/SLuupkYtIDSWyQHPlzJv2dp3Eb9udsiGfou3AExeQBAmW2zKHbc0vw8aBRe4xfbqgHf8M/SQI6rwZfikdFTfq/1Q5TQV8hZKbhUSbrOzN+/iq9pddlFK5np+1YSB11JJr3NWKwZjafHjO9n+Kw7nu5ZEoYFxi7SI/HYxJuLGzLvlpWjgBL7ZuyP0/063RbO+QLVtkflJuuoQlfJ0B3zGE84+N5Xf1OVFdsgdXWHIdXCIEA+vJDvjavpEJPOVjcvlU2qBsQdx5vZ7JpdRZ2+GqExRUyDjoJVcTKjuV+aVaD85tN53EmtjdDDJ2ImGIgjiuW/ORvp+tFpnaSl3ju4uErqgn6Ej8qiv9E0cgNS7xwIft3J5Tpzt0sqT3/3cj1X0ief3AONJ5jZOY+hU5wmbsfnFkjM4ctF2N+S0nmILvjW4fuiNV0ByZzfL9nlA19oA2NpyE5EVEE+jCMT0QI93BHKzNd/Zo+aKuWXUk9eR5Hcv9btAiBhI8LkEX/0DyLnrtsXVrEQHdjOE1a+emI37i0W7CZzaA9B+xJdTp9niF87psxAlS3GuOKhCam3waNozewjqBHbuQroTw8qCSkKOQJQ90+5+yqVSTZnY2pv+ElFSpAx15nDLBUVHvRSlulYmz/7lHmC46N8UKU9ok26vRGWp4+AATD5XYipWixoryR6D7xQWtnyQAsrg/rDCMYMsUlcXUN5NMaaXRRfGsqfRoeDarkLq4ELkfn9xOEb5WD9xGCAReEn4SmFAqGzYbmiJDCv1E1vC/SmlpxhPZJh9PAZiEODykfmgECuaixD7Kzdi1sLhZCAAIz89CuZa3oBAEzF8JptNl7inBC5ZiOeCfrlzj7MogFfWvkIju+oVnjoxJET71jwV1vfC+PBUMUwqLnbTGpMu34smu1Pt45fK45ORkbda9GMGwSbe5hZqi9T/VhwPOygMdtlfQV3LKBwvFcA76rwoUuXnTAbSr3D51ys8v3rec6M6jZ1M7PXhswIb4IUvrZbmPVOyVV6icldDRgmCQO9k7KCh9VJZJYWxuWqfIHs3qZSyg23M2gGoXtEDr/HyaU5w/YojdjNLIWbqvu3UnsNA4QDEKKjKNho+hU0Wc3yscBbGTdQKE7OuzsRWrRTW1jztXXI8z69/Q2kOxYlAl/OaW0Q/17iJiLi1ux8OEy6sY28jkIr3JCc8Fo0yK2FkKKA3IA90HR9XJmg9rbUe8swEEUR4WzZzdZnQW1B43rsopktCdRn/CyDIgE8XnxCMa7y2Wr8lp+fIEg5CJXuw6N5Yw98ud2Nqi+ObYR4WVQg7bDzFiMD0GCSqGSIb3DQEJFDEwHi4AdQBmAGkALQB0AG8AbwBsAC0AcgBlAG0AbwB0AGUALQByAGUAbABlAGEAcwBlMCEGCSqGSIb3DQEJFTEUBBJUaW1lIDE3ODMwMDM3ODcxNzgwggTBBgkqhkiG9w0BBwagggSyMIIErgIBADCCBKcGCSqGSIb3DQEHATBmBgkqhkiG9w0BBQ0wWTA4BgkqhkiG9w0BBQwwKwQU2yocDgirCP3KPpyZVfu1K5F41vgCAicQAgEgMAwGCCqGSIb3DQIJBQAwHQYJYIZIAWUDBAEqBBAlqkTdggOqTz+HymLr9QQCgIIEMGMSjkuO9rODti5pV+YXMYEm3Fft1stBq5QzVqsVti4qzr6dGw33dg58ZNdaa/mQL4TIkFo2lkqiYhqGf5gZE8E0P105HsB70iOIGJlag4v2rK65lRjzs59x4dupQoZcjFocXo8JA2g0DtxyWXfqvDVMHXw7KgwGc7srAUGtJTRMptF9YIAywo8Qk5woSK7AWqgWPBE8xLpSGwF77hpAxpIaCqv3XePEiiG1Ztq3Qj9QrCDLDxLJvWMkSIBD6TkHdHQXbwq5Vop/j7CRHOWmIJ+nB/03Kx78AWxmbrhC2PNHzLlBG8dTmymfjT1raZP1cvAVFHO3ImoBRBD81UAgoVXm7CLfJVXblA8KbVqwoLuLcULEkKgkIhxZ00pSpFw/kLVus4YsJEvbYV32I/E+SInFmbdmGfyThYmIOBvCCARomLbmLR8WhvGaZiAcYnx6EhKJuAv2WI3OWRVoRuXzpbeufll51je3e1NnbQ/oDAkNI8h1hea3r5NNyHz4vfIxYm9K8E3eVXMv2Ti8voHDwVxnjY+8vq0HaYJePvnmmQhIsg4XeQ1pXjKjRQgnGrzmQzz6AIhVLjtRKnNInmjIHC3XvQNQMxxc1N5Bs3FZENYA1jh05mYfGCTwyWfjBsWrodG/Yes79w+SsXaTHgBvKWX4HBQtXvQk+SiM1SmLe6MmUX9invK8wrFb4ZsTOy00Wwz5uoJ1q43XEfLXwn86JCBuUvDtH8CP79fvy6C9HGb3Vbmmf1d3aEFZjEFyxjBfVXDUrN+qo4yEpYLdWFWeu3J3njf029jX7f/tfG+5q7yWyLR/nPdNNykxYADj9mMpUXpuAA5eliypUknKB2yMexByjzJSDQ2VqBGHr/OVh9J9uey1rXs3lOKRCQf/w6aIhwPlrmSEXcPFG5qdy/hEXC50x3lNYL0lbMy65OIXXQD/T2IJVjanPX+mNB/Y8FSbqA4ecw02L4C9Tn6rf12hCh3ZB6yijiKd5JPlx8T1nOGFQN/cvQfTssibGQdkdZTu9GpZB9a8TiZuGSmLLAZDrExUxtx7S4t/cctNKQaVjOJkG0vyA8RYTOivBDseQ33QdRT+FDJnWpdGb25YDzzcd+MoVUc5Tf6gb65eJpqp2UlHIXSoScVvKpV76Bq13Fv/ePEgU0AyqbgmHdTCPXX5gyTJfRm8nQLfTPHxxFJbaOusRhYn+ET+lqwvCJrP4jRhre7pIztA2Qt4woYlwxG0Dyf/B3pxUx/E7qr+N/U+uXgCDzhHGUq1ea1DKjVKWaaZVDnP0fePogsVXML3apNJ6Mkaj1IDI+9AmRCKEWBN6TRl0yXD2EAJlVzggHGJfAj8CXEprUBXtRoMMkm3HOdKU4ReZvQYan5xRTAMgb0bqm2WO++8AS9u+aK7kYVeppJE9r0Q1aswgyd9sODbBebhrc4wTTAxMA0GCWCGSAFlAwQCAQUABCBvmDsbPS4uOtiIz8YSmMEu8cNzUpu8G89SHNC86YzqYgQU2OlAGt7YdKqAD+0rQOBkec2ZsCgCAicQ
```

#### ANDROID_KEYSTORE_PASSWORD
Keystore 的密码：
```
android123
```

#### ANDROID_KEY_ALIAS
Key 的别名：
```
ufi-tool-remote-release
```

#### ANDROID_KEY_PASSWORD
Key 的密码：
```
android123
```

## 工作流程说明

配置完成后，GitHub Actions 工作流会在以下情况自动运行：

1. **推送到 main 分支** - 运行测试和构建 Debug APK
2. **推送版本标签 (v*)** - 构建 Debug 和 Release APK，并自动发布到 GitHub Releases
3. **Pull Request** - 运行测试和构建检查

### 发布新版本

要发布新版本，只需创建并推送一个版本标签：

```bash
# 创建标签
git tag v1.0.0

# 推送标签
git push origin v1.0.0
```

这将触发 Release 构建，生成签名的 APK，并自动创建 GitHub Release。

## 安全提示

⚠️ **重要**：
- `keystore.properties` 和 `signing/` 目录已添加到 `.gitignore`，不会被提交到仓库
- Keystore 文件包含敏感信息，请妥善保管
- 建议定期备份 `signing/ufi-tool-remote-release.jks` 文件
- 如果需要更改密码，请同时更新 `keystore.properties` 和 GitHub Secrets

## 验证配置

要在本地验证签名配置是否正常工作：

```bash
# 构建 Release APK
./gradlew assembleRelease

# 验证 APK 签名
apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
```
