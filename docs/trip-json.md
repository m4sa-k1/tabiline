# 旅行JSON / Whole-trip AI interchange contract

「旅を追加」の右下メニューから、旅行1件とその予定をまとめて入力できます。ユーザーがJSONを手作業で作ることではなく、AIへコピーしたプロンプトから生成させることを想定しています。バックアップJSONや旧1.1.0の移動1件用JSONとは別形式です。

## 操作と安全性

1. 「旅を追加」→ ＋ → **AIに考えてもらう** を開き、計画の種類と条件を選びます。
2. プロンプトをコピーして任意のAIへ送ります。残っている `【要入力：…】` を埋めてください。
3. AIと旅程を確定し、旅行全体のJSONを1つ受け取ります。
4. **JSONを読み込む** は直接ファイル選択、**JSONをコピペで読み込む** は入力ポップアップです。
5. 旅行名・期間・メモと予定一覧を確認して保存します。保存後に各予定を編集できます。

取り込みは入力中の旅行と予定一覧を置き換えますが、保存までは登録しません。不正なJSONでは入力を変更しません。保存は新しい旅行の追加のみで、既存データを上書きしません。旅行と予定は1つのトランザクションで保存され、途中で失敗するとすべて取り消されます。保存エラー時は入力を残して再試行できます。

AIへの自動送信・現在地の自動取得はありません。コピーした条件には入力した情報が含まれます。外部サービスへ渡す情報は必要最小限にし、時刻・運行・料金・乗り場は公式情報で確認してください。

## プロンプトの種類とカスタマイズ

目的地指定、地域指定、日数から提案、完全おまかせ、日帰り・近場、予算優先、テーマ・体験重視、複数都市の周遊、青春18きっぷの9種類です。**旅行先でしたいこと** は全種類で入力できます（未定なら「おまかせ」）。

青春18きっぷでは券の種類・利用開始日を入力できます。プロンプトは旅行時点の公式情報で、発売・利用期間、連続する有効日数、対象路線・列車・席、特例、対象外の区間や別料金を確認するよう指示します。旧制度の回数分割・複数人共有を前提にしません。[公式案内](https://www.jreast.co.jp/tickets/info.aspx?GoodsCd=3001)の条件をご確認ください。

出発地、出発日時、帰着期限、予算、人数、目的地、地域、日数、テーマ、必須の体験、避けたいこと、移動時間上限、宿泊、確定済みの予定、歩行・食事などの配慮、乗り換えの余裕を入力できます。旅のペースは「ゆったり・標準・充実」、交通手段は複数選択できます。条件欄は各2,000文字まで。空欄を残してコピーし、後から補うこともできます。プロンプトの全文とJSON仕様をアプリ内で確認できます。

## JSON形式

UTF-8・全体で最大1MB、各予定は最大256KB。単一オブジェクト、予定は1〜200件、旅行期間は最大366日です。整形済み・1行・UTF-8 BOM・単一のJSONコードブロックに対応します。説明文、コメント、末尾カンマ、重複キー、シングルクォート、複数オブジェクトの連結は拒否します。

以下は形式説明用の架空の予定です。

```json
{
  "format": "tabiline.trip",
  "version": 1,
  "trip": {
    "name": "京都旅行",
    "startDate": "2026-11-14",
    "endDate": "2026-11-15",
    "note": "ゆったり観光"
  },
  "legs": [
    {
      "mode": "TRAIN",
      "departure": "2026-11-14T09:00:00+09:00",
      "arrival": "2026-11-14T09:30:00+09:00",
      "departureZoneId": "Asia/Tokyo",
      "arrivalZoneId": "Asia/Tokyo",
      "departurePlace": "出発駅",
      "arrivalPlace": "到着駅",
      "trainType": "LOCAL",
      "trainLine": "確認済みの路線名",
      "departureTerminal": "",
      "arrivalTerminal": "",
      "departurePlatform": "",
      "arrivalPlatform": "",
      "boardingGroup": "",
      "flightNumber": "",
      "memo": "公式情報で時刻を確認",
      "precedingGapType": "WAIT"
    },
    {
      "mode": "FREE_TIME",
      "departure": "2026-11-14T10:00:00+09:00",
      "arrival": "2026-11-14T11:00:00+09:00",
      "departureZoneId": "Asia/Tokyo",
      "arrivalZoneId": "Asia/Tokyo",
      "departurePlace": "観光",
      "memo": "予約・営業時間を確認"
    }
  ]
}
```

## 旅行の項目

`format` は `tabiline.trip`、`version` は整数 `1` が必須。`trip` と `legs` も必須です。

`trip.name` は空欄不可。`startDate`・`endDate` は `YYYY-MM-DD`、終了日は開始日以降です。`note` は省略可能な文字列。全予定の現地出発日・現地到着日が旅行期間内である必要があります。日またぎでは到着日も含めてください。

## 予定の全項目

| 項目 | 内容 |
| --- | --- |
| `mode` | 必須：`TRAIN`, `FLIGHT`, `FERRY`, `BUS`, `WALK`, `FREE_TIME`, `OTHER` |
| `departure`, `arrival` | 必須：UTCオフセットまたは`Z`付きISO 8601日時。終了・到着は開始・出発より後。夜行便は実際の翌日の年月日 |
| `departureZoneId`, `arrivalZoneId` | 必須：有効なIANAタイムゾーン。空き時間は同じ地域 |
| `departurePlace`, `arrivalPlace` | 通常の移動は両方空欄不可。空き時間の用事名は`departurePlace`、未入力なら「空き時間」 |
| `trainType` | `SHINKANSEN`, `LIMITED_EXPRESS`, `EXPRESS`, `RAPID`, `LOCAL`, `OTHER`、または`null`。電車で省略時は`LOCAL` |
| `trainLine` | 路線名 |
| `departureTerminal`, `arrivalTerminal` | 出発・到着ターミナル |
| `departurePlatform`, `arrivalPlatform` | 出発・到着の乗り場・ホーム・ゲート |
| `boardingGroup`, `flightNumber` | 搭乗Group・便番号 |
| `memo` | メモ。空き時間にも使用可能 |
| `precedingGapType` | `WAIT`（省略時）または`TRANSFER` |

任意の文字列は省略時に空欄。各文字列は最大20,000文字です。`trainType`以外の文字列に`null`・数値・配列を使わないでください。空き時間は終了日時も必須で、到着地や交通機関固有項目は空欄にします。同じ場所での待ち時間を独立したダミー移動として作らないでください。

内部ID・`tripId`・`tripName`・`sortOrder`・`createdAt`などは指定できません。予定は出発時刻順に並べて保存し、すべて新しい旅行に紐づきます。未知のキー・不正な型・旅行期間外の予定は拒否します。
