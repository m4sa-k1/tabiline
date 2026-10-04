# Tabiline

## 日本語

Tabilineは、旅行中の移動を一本のタイムラインで見渡せるAndroidアプリです。電車、飛行機、船、バスなどの予定をまとめ、次に乗る交通機関の時刻・乗り場・行き先をすぐに確認できます。

### 主な機能

- 旅行ごとに移動予定をまとめて管理
- 旅行に紐づけない単独の移動予定にも対応
- 出発・到着時刻、場所、交通手段、乗り場、メモを登録
- 日付ごとに整理されたタイムライン表示
- 次の移動と出発までの残り時間を大きく表示
- 乗り継ぎ時間の自動計算と短時間乗り継ぎの警告
- ライト・ダークテーマと複数のアクセントカラー
- 端末内保存によるオフライン利用

### 対応環境

- Android 8.0（Android API 26）以降
- インターネット接続は不要です

### インストール

1. [Releases](https://github.com/m4sa-k1/tabiline/releases/latest)から最新の`Tabiline` APKをダウンロードします。
2. Androidの設定で、ダウンロードに使用したブラウザまたはファイル管理アプリに「不明なアプリのインストール」を許可します。
3. ダウンロードしたAPKを開いてインストールします。

既存のTabilineへ同じ署名の新しいバージョンをインストールすると、登録済みデータを維持したまま更新できます。念のため、重要な予約番号などは別の安全な場所にも保管してください。

### 基本的な使い方

1. 「旅行」から新しい旅行を作成します。
2. 大きな追加ボタンから移動予定を登録します。
3. 「今日」で直近の移動を確認し、「タイムライン」で旅行全体を確認します。
4. 表示テーマ、アクセントカラー、標準タイムゾーンなどは「設定」から変更できます。

旅行が決まっていない移動は、旅行へ紐づけずに登録することもできます。この場合、日付を名前にした予定として旅行一覧に表示されます。

### データとプライバシー

旅行・移動・設定データは端末内に保存されます。Tabilineは、これらの情報を外部サーバーへ送信しません。アプリを削除すると端末内のデータも失われる場合があります。

### 不具合・要望

不具合や機能要望は[GitHub Issues](https://github.com/m4sa-k1/tabiline/issues)へお寄せください。不具合報告には、端末名、Androidバージョン、Tabilineのバージョン、再現手順を含めてください。予約番号などの個人情報は投稿しないでください。

### 権利とライセンス

Tabiline本体のソースコード、デザイン、画像、文書などの権利は、第三者素材を除き@m4sak1が留保します。公式APKは個人的かつ非商用の目的で利用できます。ソースコードやAPKの複製、改変、再配布、販売などには、権利者の事前の書面による許可が必要です。詳しくは[LICENSE](LICENSE)をご覧ください。

Roboto FlexおよびNoto Sans JPは、アプリ本体とは別にSIL Open Font License 1.1の条件で使用しています。フォントのライセンス全文は[第三者ライセンス](app/src/main/assets/third_party_licenses)にあります。フォントのOFLはTabiline本体には適用されません。

---

## English

Tabiline is an Android app that presents the transport parts of a trip on one clear timeline. It keeps train, flight, ferry, bus, and other travel details together so you can quickly check when to leave, where to board, and where you are going.

### Features

- Organize transport plans by trip
- Add standalone transport plans without assigning them to a trip
- Save departure and arrival times, places, transport modes, boarding locations, and notes
- Browse itineraries grouped by date
- See the next journey and time remaining until departure at a glance
- Automatically calculate transfer times and warn about short connections
- Choose light or dark mode and several accent colors
- Use the app offline with on-device storage

### Requirements

- Android 8.0 (Android API 26) or later
- No internet connection is required

### Installation

1. Download the latest `Tabiline` APK from [Releases](https://github.com/m4sa-k1/tabiline/releases/latest).
2. In Android settings, allow your browser or file manager to install unknown apps.
3. Open the downloaded APK and install it.

Installing a newer version signed with the same official key updates Tabiline while keeping existing data. Keep important booking references in another secure place as a precaution.

### Getting started

1. Create a trip from the Trips tab.
2. Use the large add button to enter a transport plan.
3. Check the nearest journey on Today and browse the full itinerary on Timeline.
4. Change the theme, accent color, default time zone, and other preferences in Settings.

You can also save transport without assigning it to a trip. Tabiline then shows it in the trip list under an automatically generated date-based name.

### Data and privacy

Trip, transport, and preference data is stored locally on your device. Tabiline does not send this information to an external server. Uninstalling the app may remove its local data.

### Issues and feedback

Please use [GitHub Issues](https://github.com/m4sa-k1/tabiline/issues) for bug reports and feature requests. Include your device model, Android version, Tabiline version, and reproduction steps. Do not post personal information such as booking references.

### Rights and licenses

Except for third-party materials, all rights in the Tabiline source code, design, artwork, and documentation are reserved by @m4sak1. The official APK may be used for personal, non-commercial purposes. Copying, modifying, redistributing, or selling the source code or APK requires prior written permission from the rights holder. See [LICENSE](LICENSE) for details.

Roboto Flex and Noto Sans JP are used separately under the SIL Open Font License 1.1. Their full license texts are available under [Third-party licenses](app/src/main/assets/third_party_licenses). The fonts' OFL terms do not apply to Tabiline itself.
