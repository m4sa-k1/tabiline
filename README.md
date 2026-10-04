# Tabiline

旅の移動を、ひとつのタイムラインに。Tabilineは、電車・飛行機・船・バスなどの確定した移動予定をオフラインで管理するAndroidアプリです。

## 主な機能

- 旅行ごとの移動タイムライン
- 日付・タイムゾーンをまたぐ移動
- 次の移動と出発までの残り時間
- 交通手段別の色・アイコン表示
- 乗り継ぎ時間の自動計算と警告
- 長押しドラッグによる並べ替え
- ライト／ダーク／端末テーマ
- 乗り継ぎ警告時間のカスタマイズ
- 端末内へのオフライン保存

## 開発環境

- Android Studio（JDK 17）
- Android SDK 36
- Kotlin / Jetpack Compose / Material 3
- Room / DataStore
- minSdk 26

リポジトリを開き、Gradle Sync後に`app`構成を実行してください。コマンドラインでは次を使用できます。

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

デバッグAPKは`app/build/outputs/apk/debug/app-debug.apk`に生成されます。

## ブランチ運用

- `dev`: 通常の開発先
- `main`: リリース対象

開発は`dev`で行い、リリース可能になった変更だけを`main`へマージします。`main`へのpushで署名済みAPKのビルドとGitHub Release作成が実行されます。

## 自動バージョン

ワークフローは既存のGitHub Releaseから`v0.0.N`形式の最大値を調べます。

- Releaseがない場合: `0.0.0` / versionCode `1`
- 次回: `0.0.1` / versionCode `2`
- APK名: `tabiline-0.0.N.apk`
- Release名: `Tabiline v0.0.N`

バージョンはビルド引数として注入するため、バージョン更新コミットや再実行ループは発生しません。

## 署名鍵の初期設定

同じアプリとしてアップデートするには、すべてのリリースで同じ鍵が必要です。鍵とパスワードは安全な場所へバックアップしてください。紛失した場合、既存インストールへの更新はできません。

### 1. 鍵を生成

```powershell
keytool -genkeypair -v -keystore tabiline-release.jks -alias tabiline -keyalg RSA -keysize 2048 -validity 10000
```

生成した`.jks`はリポジトリへ追加しないでください。

### 2. 鍵をBase64化

PowerShell:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("tabiline-release.jks")) | Set-Clipboard
```

### 3. GitHub Secretsを登録

GitHubの`Settings > Secrets and variables > Actions > New repository secret`で以下を登録します。

| Secret | 内容 |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | Base64化した鍵ファイル全文 |
| `ANDROID_KEYSTORE_PASSWORD` | KeyStoreのパスワード |
| `ANDROID_KEY_ALIAS` | 鍵のalias（上の例では`tabiline`） |
| `ANDROID_KEY_PASSWORD` | aliasの鍵パスワード |

リポジトリのActions設定で、ワークフローの`GITHUB_TOKEN`にcontentsへの書き込みを許可してください。ワークフロー自身にも`contents: write`を最小権限として指定しています。

### 4. 初回リリース

`dev`を`main`へマージしてpushします。Actions完了後、Releasesに`Tabiline v0.0.0`と署名済みAPKが追加されます。

## APKのインストール

ReleasesからAPKを端末へダウンロードし、Androidの「不明なアプリのインストール」を許可して開きます。更新版も同じ署名鍵で署名されるため、データを維持したまま上書きできます。

## 拡張しやすい構成

- `core/model`: UIや保存方式に依存しないモデル
- `core/domain`: 次の移動・乗り継ぎなどの純粋なルール
- `data/local`: Roomの永続化実装
- `data/repository`: UIと保存先の境界
- `data/settings`: DataStore設定
- `feature/*`: 画面機能
- `ui/*`: テーマと共通部品
- `di`: 依存関係の組み立て

将来クラウド同期や運行情報APIを追加する場合も、`TabilineRepository`の実装を追加することで画面とドメインルールを保ったまま拡張できます。交通手段固有の警告ルールは`TransferPolicy`へ集約しています。

## プライバシー

初期版はアカウント、位置情報、ネットワーク権限を使用しません。旅行情報は端末内に保存されます。
